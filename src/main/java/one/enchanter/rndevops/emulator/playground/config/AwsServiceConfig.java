package one.enchanter.rndevops.emulator.playground.config;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AwsServiceConfig
 * 
 * S3Clientなどを環境ごとに切り替えるため、Beanにしています。
 * awsrealとawsemulatorで同じBeanを作成する必要がある場合、ここにそのinterfaceを定義する。
 * このようにすることで、実装漏れをコンパイル時に見つけられるようになっています。
 * 
 * 注意。直接クラスに実装することができてしまいます。その場合はコンパイル時のチェックが機能しません。
 * 
 */
public interface AwsServiceConfig {
    public S3Client s3Client();

    public S3Presigner s3Presigner();

    public SqsClient sqsClient();

    public DynamoDbClient dynamoDbClient();

    public LambdaClient lambdaClient();

    public CloudWatchLogsClient cloudWatchLogsClient();

}
