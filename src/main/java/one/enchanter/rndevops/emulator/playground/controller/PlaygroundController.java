package one.enchanter.rndevops.emulator.playground.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
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
    private SqsClient sqsClient;

    @Autowired 
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private CloudWatchLogsClient cloudWatchLogsClient;

    @GetMapping("lambda")
    public Map<String,String> lambda(){

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

    @GetMapping ("sqs")
    public Map<String,String> sqs(){

        String queueUrl = "https://sqs.ap-northeast-1.amazonaws.com/000000000000/emulator-dev-queue";
        String messageBody = "{\"orderId\":1001}";

        SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .delaySeconds(0)
                    .build();

        SendMessageResponse response = sqsClient.sendMessage(request);

        return Map.of("message","success. id:" +response.messageId());
        
    }


    @GetMapping ("s3ls")
    public Map<String,String> s3ls(){
        
        ListBucketsResponse response = s3Client.listBuckets();
        for (Bucket bucket : response.buckets()) {
            System.out.printf("%s  %s%n", bucket.creationDate(), bucket.name());
        }

        return Map.of("message", "success:" + response.buckets());

    }
    
    /*
     * あらかじめデータを作成しておく。
     * aws dynamodb put-item --table-name emulator-dev-dynamodb --item '{"Id": {"S": "1"}, "data": {"S": "sample-session-payload"}}' 
     */
    @GetMapping("dynamodb")
    public Map<String,?> dynamodb(String id){

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

    @GetMapping("/logs")
    public Map<String, String> logs() {

        cloudWatchLogsClient.putLogEvents(
                PutLogEventsRequest.builder()
                        .logGroupName("test-group")
                        .logStreamName("test-docker")
                        .logEvents(
                                InputLogEvent.builder()
                                        .timestamp(System.currentTimeMillis())
                                        .message("hello emulator")
                                        .build())
                        .build());

        return Map.of("message", "success");
    }

}
