package server;

import org.junit.jupiter.api.Test;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * DatabaseSeederTest  Chạy test này MỘT LẦN để tạo bảng và seed dữ liệu DynamoDB.
 *
 * mvn test -Dtest=DatabaseSeederTest
 */
public class DatabaseSeederTest {

    private final DynamoDbClient ddb = DynamoDBConfig.getClient();

    @Test
    void seedDatabase() {
        System.out.println("============================================");
        System.out.println(">>> KHỞI TẠO DYNAMODB TABLES <<<");
        System.out.println("============================================");

        createTable("Tables",    "maBan",     null, true);  // force recreate nếu PK sai
        createTable("Invoices",  "invoiceId",  null, false);
        createTable("MenuItems", "categoryId", "itemId", false);
        createTable("Customers", "customerId", null, true);  // force recreate nếu PK sai
        createTable("Employees", "employeeId", null, false);

        System.out.println("\n>>> NẠPTỈ DỮ LIỆU MẪU <<<");
        seedEmployees();
        seedTables();
        seedMenu();

        System.out.println("\n>>> HOÀN TẤT ✓ <<<");
    }

    private void createTable(String name, String pk, String sk, boolean forceRecreate) {
        if (forceRecreate) {
            try {
                System.out.print("[DELETE] Xóa bảng '" + name + "' cũ...");
                ddb.deleteTable(DeleteTableRequest.builder().tableName(name).build());
                // Đợi bảng bị xóa hoàn toàn
                for (int i = 0; i < 20; i++) {
                    try {
                        ddb.describeTable(DescribeTableRequest.builder().tableName(name).build());
                        System.out.print(".");
                        Thread.sleep(2000);
                    } catch (ResourceNotFoundException e) {
                        System.out.println(" Đã xóa ✓");
                        break;
                    }
                }
            } catch (ResourceNotFoundException e) {
                System.out.println("[INFO] Bảng '" + name + "' chưa tồn tại, bỏ qua xóa.");
            } catch (Exception e) {
                System.err.println("[WARN] Không xóa được bảng: " + e.getMessage());
            }
        }

        try {
            CreateTableRequest.Builder builder = CreateTableRequest.builder()
                .tableName(name)
                .billingMode(BillingMode.PAY_PER_REQUEST);

            if (sk == null) {
                builder.keySchema(
                    KeySchemaElement.builder().attributeName(pk).keyType(KeyType.HASH).build()
                ).attributeDefinitions(
                    AttributeDefinition.builder().attributeName(pk).attributeType(ScalarAttributeType.S).build()
                );
            } else {
                builder.keySchema(
                    KeySchemaElement.builder().attributeName(pk).keyType(KeyType.HASH).build(),
                    KeySchemaElement.builder().attributeName(sk).keyType(KeyType.RANGE).build()
                ).attributeDefinitions(
                    AttributeDefinition.builder().attributeName(pk).attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName(sk).attributeType(ScalarAttributeType.S).build()
                );
            }

            ddb.createTable(builder.build());
            System.out.print("[CREATING] Bảng '" + name + "'");
            waitForTable(name);
            System.out.println(" → ACTIVE ✓");

        } catch (ResourceInUseException e) {
            System.out.println("[SKIP] Bảng '" + name + "' đã tồn tại.");
        } catch (Exception e) {
            System.err.println("[ERROR] '" + name + "': " + e.getMessage());
        }
    }

    private void waitForTable(String name) {
        for (int i = 0; i < 30; i++) {
            try {
                DescribeTableResponse res = ddb.describeTable(DescribeTableRequest.builder().tableName(name).build());
                if (res.table().tableStatus() == TableStatus.ACTIVE) return;
                System.out.print(".");
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.print("?");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void seedEmployees() {
        String[][] emps = {
            {"admin",    "Quản trị viên",  "admin123",  "Admin"},
            {"quanly",   "Nguyễn Quản Lý", "quanly123", "QuanLy"},
            {"thungan1", "Trần Thu Ngân",   "tn123",     "ThuNgan"},
        };
        for (String[] e : emps) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("employeeId",   s(e[0])); item.put("name", s(e[1]));
            item.put("passwordHash", s(e[2])); item.put("role", s(e[3]));
            ddb.putItem(PutItemRequest.builder().tableName("Employees").item(item).build());
            System.out.println("  + NV: " + e[0] + " (" + e[3] + ")");
        }
    }

    private void seedTables() {
        String[][] tables = {
            {"B001","Tầng 1","4"},{"B002","Tầng 1","4"},{"B003","Tầng 1","6"},
            {"B004","Tầng 2","4"},{"B005","Tầng 2","8"},{"B006","Sân vườn","6"},
        };
        for (String[] t : tables) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("maBan",    s(t[0]));  // PK: maBan
            item.put("viTri",    s(t[1]));
            item.put("sucChua",  n(t[2]));
            item.put("trangThai",s("Trong"));
            item.put("maLoaiBan",s("Thuong"));
            ddb.putItem(PutItemRequest.builder().tableName("Tables").item(item).build());
            System.out.println("  + Bàn: " + t[0]);
        }
    }

    private void seedMenu() {
        Object[][] menu = {
            {"CAT001","FOOD001","Phở Bò",55000},{"CAT001","FOOD002","Phở Gà",50000},
            {"CAT002","FOOD003","Cơm Tấm Sườn",65000},{"CAT002","FOOD004","Cơm Gà",60000},
            {"CAT003","FOOD005","Gà Nướng",150000},{"CAT003","FOOD006","Bò Lúc Lắc",130000},
            {"CAT004","DRINK001","Bia Tiger",25000},{"CAT004","DRINK002","Nước Ngọt",15000},
        };
        for (Object[] m : menu) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("categoryId", s((String)m[0])); item.put("itemId", s((String)m[1]));
            item.put("name", s((String)m[2])); item.put("price", n(String.valueOf(m[3])));
            item.put("available", AttributeValue.builder().bool(true).build());
            ddb.putItem(PutItemRequest.builder().tableName("MenuItems").item(item).build());
            System.out.println("  + Món: " + m[2] + " - " + m[3] + " VNĐ");
        }
    }

    private AttributeValue s(String v) { return AttributeValue.builder().s(v).build(); }
    private AttributeValue n(String v) { return AttributeValue.builder().n(v).build(); }
}
