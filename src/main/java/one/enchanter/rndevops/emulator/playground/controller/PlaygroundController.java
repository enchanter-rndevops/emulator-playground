package one.enchanter.rndevops.emulator.playground.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@RestController
@Slf4j
@RequestMapping("/1/")
public class PlaygroundController {

    @Autowired
    private LambdaClient lambdaClient;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private CloudWatchLogsClient cloudWatchLogsClient;

    @GetMapping("lambda")
    public Map<String, String> lambda() {

        String jsonPayload = "{\"name\":\"emulator\"}";

        InvokeRequest request = InvokeRequest.builder()
                .functionName("emulator-dev-lambda")
                .payload(SdkBytes.fromUtf8String(jsonPayload))
                .build();

        InvokeResponse response = lambdaClient.invoke(request);

        // 関数内で未処理の例外が発生した場合、functionError にエラー種別が入る
        if (response.functionError() != null) {
            return Map.of("message", "error:" + response.functionError());
        }

        if (response.payload() != null) {
            return Map.of("message", "success:" + response.payload().asUtf8String());
        }

        return Map.of("message", "success:" + "");

    }

    @GetMapping("sqs")
    public Map<String, String> sqs() {

        String queueUrl = "https://sqs.ap-northeast-1.amazonaws.com/000000000000/emulator-dev-queue";
        String messageBody = "{\"orderId\":1001}";

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .delaySeconds(0)
                .build();

        SendMessageResponse response = sqsClient.sendMessage(request);

        return Map.of("message", "success. id:" + response.messageId());

    }

    @GetMapping("s3/ls")
    public Map<String, String> s3ls() {

        ListBucketsResponse response = s3Client.listBuckets();
        for (Bucket bucket : response.buckets()) {
            System.out.printf("%s  %s%n", bucket.creationDate(), bucket.name());
        }

        return Map.of("message", "success:" + response.buckets());

    }

    /*
     * 署名付きURLを取得する。
     * 実際にbucket、ファイルが存在しなくても、URLは生成される。
     * エミュレータではbucket nameがドメインに含まれるため、実際にダウンロードすることができない。
     * パススタイルにすればできるかもしれないけど、そこまでやる必要もない気がします。
     */
    @GetMapping("s3/url")
    public Map<String, String> s3url(@RequestParam("id") String id) {

        // 存在しない場合、例外をthrowする。
        // throwNoExistsS3Object("emulator-playgorund-bucket", id);

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket("emulator-dev-bucket-000000000000")
                .key(id)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // The URL will expire in 10 minutes.
                .getObjectRequest(objectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        // s3Client.getObject(GetObjectRequest.builder().bucket("").key("").build());

        return Map.of(
                "url", presignedRequest.url().toString(),
                "method", presignedRequest.httpRequest().method().toString());

    }

    @GetMapping("s3/get")
    public Map<String, String> s3get(@RequestParam("id") String id) {

        ResponseInputStream stream = s3Client
                .getObject(GetObjectRequest.builder().bucket("emulator-dev-bucket-000000000000").key(id).build());

        try {
            byte[] data = stream.readAllBytes();

            String encodedData = HexFormat.of().formatHex(data);

            return Map.of("data", encodedData);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 
     * @param data
     * @return
     */
    @GetMapping("s3/put")
    public Map<String, String> s3put(@RequestParam("data") String data) {

        String key = UUID.randomUUID().toString();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket("emulator-dev-bucket-000000000000")
                .key(key)
                .contentType("text/plain; charset=UTF-8")
                .build();

        PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromString(data, StandardCharsets.UTF_8));

        return Map.of("ETag:", response.eTag(), "key", key);
    }

    private boolean throwNoExistsS3Object(String bucketName, String key) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build());

            return true;

        } catch (NoSuchKeyException e) {
            // ExceptionHandlerで404を返すようにする。
            throw new RuntimeException("ファイルが存在しません");
        } catch (S3Exception e) {
            // ExceptionHandlerで404を返すようにする。
            if (e.statusCode() == 404) {
                throw new RuntimeException("ファイルが存在しません");
            }
            throw e;
        }

    }

    /*
     * あらかじめテーブルとデータを作成しておく。
     * @formatter:off
     * aws dynamodb create-table --table-name emulator-dev-dynamodb  --attribute-definitions AttributeName=Id,AttributeType=S \
     * --key-schema AttributeName=Id,KeyType=HASH --billing-mode PAY_PER_REQUEST
     * aws dynamodb put-item --table-name emulator-dev-dynamodb --item '{"Id": {"S": "1"}, "data": {"S": "sample-session-payload"}}'
     * @formatter:on
     */
    @GetMapping("dynamodb/get")
    public Map<String, ?> dynamodbQuery(@RequestParam("id") String id) {

        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName("emulator-dev-dynamodb")
                .key(Map.of("Id", AttributeValue.builder().s(id).build()))
                .build());

        if (!response.hasItem()) {
            return Map.of();
        }

        Map<String, AttributeValue> item = response.item();
        Map<String, String> result = new HashMap<>();

        if (item.containsKey("Id") && item.get("Id").s() != null) {
            result.put("Id", item.get("Id").s());
        }
        if (item.containsKey("data") && item.get("data").s() != null) {
            result.put("data", item.get("data").s());
        }

        System.out.println(result);

        return result;

    }

    @GetMapping("dynamodb/put")
    public Map<String, ?> dynamodbPut(@RequestParam("data") String data) {

        String key = UUID.randomUUID().toString();

        PutItemResponse response = dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName("emulator-dev-dynamodb")
                .item(Map.of(
                        "Id", AttributeValue.builder().s(key).build(),
                        "data", AttributeValue.builder().s(data).build()))
                .build());

        System.out.println(response.toString());

        return Map.of("Id", key);

    }

    @GetMapping("/logs")
    public Map<String, String> logs(@RequestParam("data") String data) {

        cloudWatchLogsClient.putLogEvents(
                PutLogEventsRequest.builder()
                        .logGroupName("test-group")
                        .logStreamName("test-docker")
                        .logEvents(
                                InputLogEvent.builder()
                                        .timestamp(System.currentTimeMillis())
                                        .message(data)
                                        .build())
                        .build());

        return Map.of("message", "success");
    }

}
