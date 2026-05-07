package db;
import db.EnvConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

public class DynamoDBConfig {
    private static DynamoDbClient client;
    private static DynamoDbEnhancedClient enhancedClient;

    public static DynamoDbClient getClient() {
        if (client == null) {
            String accessKey = EnvConfig.awsAccessKey();
            String secretKey = EnvConfig.awsSecretKey();
            String region = EnvConfig.awsRegion();

            client = DynamoDbClient.builder()
                    .region(Region.of(region))
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
        return client;
    }
    public static String getTableName() {
        return EnvConfig.get("DYNAMODB_TABLE_NAME", "QuanLyDatBan-Table");
    }
    public static DynamoDbEnhancedClient getEnhancedClient() {
        if (enhancedClient == null) {
            enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(getClient())
            .build();
        }
        return enhancedClient;
    }
}