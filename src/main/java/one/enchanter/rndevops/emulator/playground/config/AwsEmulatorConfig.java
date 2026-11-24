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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * local, testからWAS Emulatorを使うConfig
 * AwsEmulatorConfig
 */
@Profile( "awsemulator")
@Configuration
public class AwsEmulatorConfig {

    private static final ProfileFile PROFILE_FILE = ProfileFile.builder()
            .content(
                    Path.of(
                            System.getProperty("user.home"),
                            ".aws",
                            "emulator",
                            "9297",
                            "emulator_credentials"))
            .type(ProfileFile.Type.CREDENTIALS)
            .build();

    /*
     * ホスト名の関係で、証明書をインポートするだけではうまくいかないので、証明書を無視するHttpClientを使用する。
     */
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
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
                NoopHostnameVerifier.INSTANCE
        );

        // @Deprecated Apache5HttpClientに移行したいが、Apache5HttpClientでやる方法がわからないため、とりあえずこのまま。
        return ApacheHttpClient.builder()
                .socketFactory(tlsSocketFactory)
                .build();
    }

    @Value("${aws.emulator.region}")
    private String emulatorRegion; 

    @Value("${aws.emulator.endpoint}")
    private String emulatorEndpoint; 

    @Value("${aws.emulator.endpoint.dynamodb}")
    private String emulatorDynamodbEndpoint; 

    @Bean
    public S3Client s3Client(SdkHttpClient insecureApacheHttpClient) {

        System.out.println("emulatorEndpoint:" + emulatorEndpoint);
        System.out.println("emulatorRegion:" + emulatorRegion);

        return S3Client.builder()
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
    public SqsClient sqsClient(SdkHttpClient insecureApacheHttpClient) {
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
    public DynamoDbClient dynamoDbClient(SdkHttpClient insecureApacheHttpClient) {
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
    public LambdaClient lambdaClient(SdkHttpClient insecureApacheHttpClient) {
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

    // 使用するサービスのClientを生成する処理を追加する。

}