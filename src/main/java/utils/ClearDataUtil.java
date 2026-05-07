package utils;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.Map;

public class ClearDataUtil {
    public static void main(String[] args) {
        DynamoDbClient client = DynamoDBConfig.getClient();
        
        System.out.println(">>> ĐANG XÓA TOÀN BỘ HÓA ĐƠN...");
        clearTable(client, "Invoices", "invoiceId");
        
        System.out.println(">>> ĐANG RESET TRẠNG THÁI TẤT CẢ BÀN VỀ 'TRONG'...");
        resetTables(client);
        
        System.out.println(">>> HOÀN TẤT DỌN DẸP DỮ LIỆU!");
        System.exit(0);
    }

    private static void clearTable(DynamoDbClient client, String tableName, String partitionKey) {
        try {
            ScanResponse scanResponse = client.scan(ScanRequest.builder().tableName(tableName).build());
            for (Map<String, AttributeValue> item : scanResponse.items()) {
                String id = item.get(partitionKey).s();
                client.deleteItem(DeleteItemRequest.builder()
                        .tableName(tableName)
                        .key(Map.of(partitionKey, AttributeValue.builder().s(id).build()))
                        .build());
                System.out.println("  - Đã xóa " + tableName + ": " + id);
            }
        } catch (Exception e) {
            System.err.println(" Lỗi khi xóa bảng " + tableName + ": " + e.getMessage());
        }
    }

    private static void resetTables(DynamoDbClient client) {
        try {
            ScanResponse scanResponse = client.scan(ScanRequest.builder().tableName("Tables").build());
            for (Map<String, AttributeValue> item : scanResponse.items()) {
                String maBan = item.get("maBan").s();
                client.updateItem(UpdateItemRequest.builder()
                        .tableName("Tables")
                        .key(Map.of("maBan", AttributeValue.builder().s(maBan).build()))
                        .updateExpression("SET trangThai = :s")
                        .expressionAttributeValues(Map.of(":s", AttributeValue.builder().s("Trong").build()))
                        .build());
                System.out.println("  - Đã reset bàn: " + maBan);
            }
        } catch (Exception e) {
            System.err.println(" Lỗi khi reset bàn: " + e.getMessage());
        }
    }
}
