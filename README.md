# AWS Emulator を Spring Boot から利用する

Spring Boot アプリケーションと AWS Emulator を Docker Compose で起動し、AWS SDK からローカルの AWS Emulator を利用できるようにします。

以下のサービスの動作を確認しています。

* Lambda
* S3
* SQS
* DynamoDB Local
* CloudWatch Logs (Fluent Bit経由)

## AWS Emulator のエンドポイントを設定する

ここでは awse というシェル関数を利用します。

環境に合わせてパスやポート番号を変更してください。

```
awse() {
  AWS_ENDPOINT_URL=https://127.0.0.1:9297 \
  AWS_SHARED_CREDENTIALS_FILE=~/.aws/emulator/9297/emulator_credentials \
  AWS_CONFIG_FILE=~/.aws/emulator/9297/config \
  AWS_EC2_METADATA_DISABLED=true \
  AWS_DISABLE_HOST_PREFIX_INJECTION=true \
  AWS_REGION=ap-northeast-1 \
  AWS_DEFAULT_REGION=ap-northeast-1 \
  aws "$@"
}
```

設定確認。UserIdなどが表示されます。
```
$ awse sts get-caller-identity
```

## 使い方

Spring Boot の Docker イメージを作成します。

```
docker build -t app-dev:1.0.0 .
```

コンテナを起動します。

```
docker compose up -d
```

サンプルの CloudFormation テンプレートを適用します。
サンプルコード内ではリソース名をハードコードしているため、スタック名は emulator としてください。

```
cd cloudformation
awse cloudformation create-stack \
  --stack-name emulator \
  --template-body file://emulator.yaml \
  --capabilities CAPABILITY_IAM
```

作成状況

```
awse cloudformation describe-stacks --stack-name emulator
```

## 動作確認

サンプルでは以下の API を用意しています。

|API |	機能|
|---|---|
|/1/lambda | Lambda呼び出し|
|/1/sqs| SQS送信|
|/1/s3ls| S3一覧取得|
|/1/dynamodb?id=1 |	DynamoDB取得|

例:
```
curl http://localhost:8080/1/s3ls
```

## AwsEmulatorConfig

AWS Emulator 用の SDK クライアントを生成しています。

ホスト上の AWS CLI はそのまま HTTPS で接続できますが、Docker コンテナ間では証明書の SAN が localhost,
127.0.0.1 のみとなっているため、

https://aws-emulator:9297

へ接続するとホスト名検証エラーになります。
そのため Docker コンテナから接続する場合は、カスタム HttpClient を使用して証明書検証を無効化しています。

## CloudWatch Logs にログを出力する

構成は以下の通りです。

Spring Boot
    ↓
app.log
    ↓
Fluent Bit
    ↓
AWS Emulator
    ↓
CloudWatch Logs

### Docker Desktop 利用時の注意

Docker Desktop の場合、

/var/lib/docker/containers/*/*-json.log

は Docker Desktop 内部の仮想環境に存在します。

WSL から直接参照できないため、Fluent Bit で Docker のログファイルを監視する方法は利用しません。
代わりに Spring Boot のログファイルを監視します。

Spring Boot の設定

application.properties
```
logging.file.name=logs/app.log
```

Fluent Bit
docker-compose.yml
```
fluent-bit:
  image: fluent/fluent-bit:latest

  volumes:
    - ~/emulator-playground/logs:/root/emulator-playground/logs
    - ./fluent-bit.conf:/fluent-bit/etc/fluent-bit.conf:ro
    - ~/.aws/emulator:/root/.aws/emulator
    - ~/.aws/emulator/9297/emulator_credentials:/root/.aws/credentials:ro

  networks:
    - emulator-playground
```

fluent-bit.conf

```
[SERVICE]
    Flush 1
    Log_Level info

[INPUT]
    Name tail
    Path /root/emulator-playground/logs/*.log
    Tag docker
    Refresh_Interval 5

[OUTPUT]
    Name cloudwatch_logs
    Match docker

    region ap-northeast-1

    tls.ca_file /root/.aws/emulator/9297/ca.pem

    log_group_name test-group
    log_stream_prefix test-

    auto_create_group true

    endpoint aws-emulator
    port 9297
```

実運用を意識する場合は、

log_group_name
log_stream_prefix

を本番 AWS と同じ命名規則にしておくとよいでしょう。

### ログ確認

ロググループ一覧
```
awse logs describe-log-groups
```

ログ取得
```
awse logs get-log-events \
  --log-group-name test-group \
  --log-stream-name test-docker
```

## 今後追加予定

現在は SDK の疎通確認が中心です。

* Lambda Invoke
* S3 ListBuckets
* SQS SendMessage
* DynamoDB GetItem
* CloudWatch Logs

今後は以下を追加予定です。

* S3 PutObject / GetObject
* SQS ReceiveMessage
* SQS → Lambda
* EventBridge → Lambda
* CloudFormation サンプルの追加

