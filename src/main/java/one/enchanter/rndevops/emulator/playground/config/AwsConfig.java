package one.enchanter.rndevops.emulator.playground.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * 本物のAWSを使用する場合のConfig.
 * 
 * ローカルPCから aws login を使用する場合、以下の依存関係が必要。
 * @formatter:off
 * 		<dependency>
 *	 		<groupId>software.amazon.awssdk</groupId>
 *			<artifactId>signin</artifactId>
 *		</dependency>
 * @formatter:on
 * ローカルでは環境変数AWS_REGION=ap-northeast-1の設定を行うこと(通常はcompose.yamlで設定されています)。
 * 
 */
@Profile({ "aws", "awsreal" })
@Configuration
public class AwsConfig implements AwsServiceConfig {

    @Bean
    @Override
    public S3Client s3Client() {
        return S3Client.builder()
                .build();
    }

    @Bean
    @Override
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .build();
    }

    @Bean
    @Override
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .build();
    }

    @Bean
    @Override
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .build();
    }

    @Bean
    @Override
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .build();
    }

    @Bean
    @Override
    public CloudWatchLogsClient cloudWatchLogsClient() {
        return CloudWatchLogsClient.builder()
                .build();
    }

}
