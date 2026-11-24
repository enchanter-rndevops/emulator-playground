package one.enchanter.rndevops.emulator.playground.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
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
 * 
 * AwsConfig
 */
@Profile({ "aws", "awsreal" })
@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client() {
        return S3Client.create();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.AP_NORTHEAST_1)
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.AP_NORTHEAST_1)
                .build();
    }

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.AP_NORTHEAST_1)
                .build();
    }

}
