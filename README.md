# AWS EmulatorをSpringBootから使用する

Spring Boot アプリケーションとAWS Emulatorをひとつのcompode.yamlに入れ、AWS Emulatorを使用できるようにします。

## つかいかた

Spring Bootを実行するためのDockerイメージを作成。

```
$ docker build -t app-dev:1.0.0 .
```

サンプルのコンテナを起動

```
$ docker compose up -d
```

このテンプレートからstackを作成する。
```
$ cd cloudformation
$ awse cloudformation create-stack --stack-name emulator --template-body file://emulator.yaml --capabilities CAPABILITY_IAM
```

実行。PlaygroundControllerのMapping定義を参照し、パスを変更してください。
S3, SQS, Lmabdaがあります。
```
$ curl http://localhost:8080/1/s3ls
```

## AwsEmulatorConfig

AwsEmulatorConfig でEmulator用のClientのBeanを生成しています。
ホストから使用する場合はhttpsで接続してもエラーになりませんが、Docker コンテナ関で使用する場合は証明書の関係でhttpsで接続できません。
そのため、コンテナ間で接続する場合、HttpClientを生成し、証明書関係のチェックを行わないようにしています。


## やりたいこと

このプロジェクトにいろいろなサンプルを追加する予定。
アプリケーションのログをAWS Emulator上のCloudWatchに出力する（あまり需要がなさそう）。
