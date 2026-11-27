package one.enchanter.rndevops.emulator.playground.config;

import java.net.URI;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * 
 * EmulatorHttpClientConfig
 */
@Configuration
@Profile("awsemulator")
class EmulatorHttpClientConfig {

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
    };

    @Bean(destroyMethod = "close")
    public SdkHttpClient insecureApacheHttpClient() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, TRUST_ALL_CERTS, new SecureRandom());

        ConnectionSocketFactory tlsSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE);

        return ApacheHttpClient.builder()
                .socketFactory(tlsSocketFactory)
                .build();
    }
}

/**
 * local, testからAWS Emulatorを使うConfig
 * AwsEmulatorConfig
 */
@Profile("awsemulator")
@Configuration
public class AwsEmulatorConfig implements AwsServiceConfig {

    private static final ProfileFile PROFILE_FILE = ProfileFile.builder()
            .content(
                    Path.of(
                            // System.getProperty("user.home"),
                            "/root",
                            ".aws",
                            "emulator",
                            "9297",
                            "emulator_credentials"))
            .type(ProfileFile.Type.CREDENTIALS)
            .build();

    @Autowired
    private SdkHttpClient insecureApacheHttpClient;

    @Value("${aws.emulator.region}")
    private String emulatorRegion;

    @Value("${aws.emulator.endpoint}")
    private String emulatorEndpoint;

    @Value("${aws.emulator.endpoint.s3signedurl:${aws.emulator.endpoint}}")
    private String emulatorS3signedEndpoint;

    @Value("${aws.emulator.endpoint.dynamodb:${aws.emulator.endpoint}}")
    private String emulatorDynamodbEndpoint;

    @Bean
    @Override
    public S3Client s3Client() {

        // AWSエミュレータはドメイン名を解決しないので、パススタイルにする。

        return S3Client.builder()
                .httpClient(insecureApacheHttpClient)
                .endpointOverride(URI.create(emulatorEndpoint))
                .region(Region.of(emulatorRegion))
                .credentialsProvider(
                        ProfileCredentialsProvider.builder()
                                .profileFile(PROFILE_FILE)
                                .profileName("default")
                                .build())
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                .build();

    }

    @Bean
    @Override
    public S3Presigner s3Presigner() {

        // AWSエミュレータはドメイン名を解決しないので、パススタイルにする。

        return S3Presigner.builder()
                // 署名付きURL生成用エンドポイント。
                //
                // Dockerコンテナ内からアクセスする場合のエンドポイントは
                // https://aws-emulator:9297
                //
                // ホストPCからアクセスする場合のエンドポイントは
                // https://localhost:9297
                //
                // となります。
                //
                // 署名の生成にはホスト名が含まれています。
                // URL生成後に aws-emulator から localhost へ書き換えると SignatureDoesNotMatch エラーとなります。
                //
                // AWSエミュレータが生成した証明書が必要です。AWSエミュレータの自己署名証明書を利用する場合、
                // curl --cacert ~/.aws/emulator/9297/ca.pem <S3署名付きURL>
                // または、
                // curl --insecure <URL>
                // のように、エラーを無視すること。
                .endpointOverride(URI.create(emulatorS3signedEndpoint)) // ↑をプロパティで設定する。
                .region(Region.of(emulatorRegion))
                .credentialsProvider(
                        ProfileCredentialsProvider.builder()
                                .profileFile(PROFILE_FILE)
                                .profileName("default")
                                .build())
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                .build();

    }

    @Bean
    @Override
    public SqsClient sqsClient() {

        return SqsClient.builder()
                .httpClient(insecureApacheHttpClient)
                .endpointOverride(URI.create(emulatorEndpoint))
                .region(Region.of(emulatorRegion))
                .credentialsProvider(
                        ProfileCredentialsProvider.builder()
                                .profileFile(PROFILE_FILE)
                                .profileName("default")
                                .build())
                .build();

    }

    @Bean
    @Override
    public DynamoDbClient dynamoDbClient() {

        return DynamoDbClient.builder()
                .httpClient(insecureApacheHttpClient)
                .endpointOverride(URI.create(emulatorDynamodbEndpoint))
                .region(Region.of(emulatorRegion))
                .credentialsProvider(
                        ProfileCredentialsProvider.builder()
                                .profileFile(PROFILE_FILE)
                                .profileName("default")
                                .build())
                .build();

    }

    @Bean
    @Override
    public LambdaClient lambdaClient() {

        return LambdaClient.builder()
                .httpClient(insecureApacheHttpClient)
                .endpointOverride(URI.create(emulatorEndpoint))
                .region(Region.of(emulatorRegion))
                .credentialsProvider(
                        ProfileCredentialsProvider.builder()
                                .profileFile(PROFILE_FILE)
                                .profileName("default")
                                .build())
                .build();
    }

    @Bean
    @Override
    public CloudWatchLogsClient cloudWatchLogsClient() {

        return CloudWatchLogsClient.builder()
                .httpClient(insecureApacheHttpClient)
                .endpointOverride(URI.create(emulatorEndpoint))
                .region(Region.of(emulatorRegion))
                .credentialsProvider(
                        ProfileCredentialsProvider.builder()
                                .profileFile(PROFILE_FILE)
                                .profileName("default")
                                .build())
                .build();
    }

    // 使用するサービスのClientを生成する処理を追加する。

}