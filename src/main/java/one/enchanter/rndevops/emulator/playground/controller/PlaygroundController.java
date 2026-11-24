package one.enchanter.rndevops.emulator.playground.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
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
    
}
