package db;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/**
* DatabaseSeeder  Nạp dữ liệu đầy đủ cho hệ thống Nhà hàng Tứ Hữu.
* Đã nâng cấp để test full tính năng.
*/
public class DatabaseSeeder {
    private static final DynamoDbClient ddb = DynamoDBConfig.getClient();
    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println(">>> KHỞI TẠO DỮ LIỆU ĐẦY ĐỦ (FULL SEED) <<<");
        System.out.println("============================================");
        // 1. Tạo bảng (nếu chưa có)
        createTable("Tables",    "maBan",     null);
        createTable("Invoices",  "invoiceId",  null);
        createTable("MenuItems", "categoryId", "itemId");
        createTable("Customers", "customerId", null);
        createTable("Employees", "employeeId", null);
        createTable("Promos",    "maUuDai",    null);
        createTable("AppNotifications", "targetId", "notificationId");
        System.out.println("\n>>> NẠP DỮ LIỆU MẪU <<<");
        // 2. Seed dữ liệu
        seedEmployees();
        seedTables();
        seedMenu();
        seedCustomers();
        seedPromos();
        System.out.println("\n============================================");
        System.out.println(">>> HOÀN TẤT SEEDING - SẴN SÀNG TEST <<<");
        System.out.println("============================================");
    }
    private static void createTable(String name, String pk, String sk) {
        try {
            CreateTableRequest.Builder builder = CreateTableRequest.builder()
            .tableName(name)
            .billingMode(BillingMode.PAY_PER_REQUEST);
            if (sk == null) {
                builder.keySchema(KeySchemaElement.builder().attributeName(pk).keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName(pk).attributeType(ScalarAttributeType.S).build());
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
        System.out.print("[CREATING] Bảng '" + name + "'...");
        waitForTable(name);
        System.out.println(" → ACTIVE ✓");
    } catch (ResourceInUseException e) {
    System.out.println("[SKIP] Bảng '" + name + "' đã tồn tại.");
} catch (Exception e) {
System.err.println("[ERROR] Lỗi tạo bảng '" + name + "': " + e.getMessage());
}
}
private static void waitForTable(String name) {
    for (int i = 0; i < 30; i++) {
        try {
            DescribeTableResponse res = ddb.describeTable(DescribeTableRequest.builder().tableName(name).build());
            if (res.table().tableStatus() == TableStatus.ACTIVE) return;
            Thread.sleep(1000);
        } catch (Exception e) { }
    }
}
private static void seedEmployees() {
    String[][] employees = {
        {"admin",   "Quản trị viên", "admin123",  "Admin",    "0900000000", "admin@restaurant.com"},
        {"quanly",  "Nguyễn Quản Lý","quanly123", "QuanLy",   "0911111111", "quanly@restaurant.com"},
        {"thungan1","Trần Thu Ngân",  "tn123",     "ThuNgan",  "0922222222", "thungan1@restaurant.com"},
    };
    for (String[] emp : employees) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("employeeId",   s(emp[0]));
        item.put("name",         s(emp[1]));
        item.put("passwordHash", s(emp[2]));
        item.put("role",         s(emp[3]));
        item.put("phone",        s(emp[4]));
        item.put("email",        s(emp[5]));
        ddb.putItem(PutItemRequest.builder().tableName("Employees").item(item).build());
    }
    System.out.println("✓ Đã nạp 3 nhân viên (admin, quanly, thungan1).");
}
private static void seedTables() {
    List<Map<String, AttributeValue>> tableItems = new ArrayList<>();
    // 1. TẦNG TRỆT (TANG_TRET) - 15 bàn, sức chứa 4-10 người
    for (int i = 1; i <= 15; i++) {
        String maBan = String.format("B%03d", i);
        int sucChua = (i % 2 == 0) ? 10 : (4 + (i % 3) * 2); // Mix 4, 6, 8, 10
        tableItems.add(createTableItem(maBan, "Tầng trệt", sucChua, "TANG_TRET", "TRONG"));
    }
    // 2. TẦNG 1 (TANG_1) - 10 bàn, sức chứa 4-10 người
    for (int i = 16; i <= 25; i++) {
        String maBan = String.format("B%03d", i);
        int sucChua = 4 + (i % 4) * 2; // Mix 4, 6, 8, 10
        tableItems.add(createTableItem(maBan, "Tầng 1", sucChua, "TANG_1", "TRONG"));
    }
    // 3. PHÒNG RIÊNG (PHONG) - 5 phòng lớn, sức chứa 12-40 người
    int[] roomCapacities = {15, 20, 30, 40, 12};
    for (int i = 1; i <= 5; i++) {
        String maBan = String.format("P%02d", i);
        tableItems.add(createTableItem(maBan, "Phòng riêng", roomCapacities[i-1], "PHONG", "TRONG"));
    }
    // Thêm một vài bàn có trạng thái khác để test UI
    tableItems.get(2).put("trangThai", s("DANG_SU_DUNG")); // B003
    tableItems.get(5).put("trangThai", s("DA_DAT"));        // B006
    for (Map<String, AttributeValue> item : tableItems) {
        ddb.putItem(PutItemRequest.builder().tableName("Tables").item(item).build());
    }
    System.out.println("✓ Đã nạp 30 bàn ăn (Tầng trệt, Tầng 1, Phòng riêng) đúng chuẩn nghiệp vụ.");
}
private static Map<String, AttributeValue> createTableItem(String maBan, String viTri, int sucChua, String loaiBan, String trangThai) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("maBan", s(maBan));
    item.put("viTri", s(viTri));
    item.put("sucChua", n(String.valueOf(sucChua)));
    item.put("maLoaiBan", s(loaiBan)); // Map với LoaiBan enum
    item.put("trangThai", s(trangThai)); // Map với TrangThaiBan enum
    return item;
}
    private static void seedMenu() {
        System.out.println("--- Đang dọn dẹp bảng MenuItems trước khi nạp mới ---");
        try {
            ScanResponse scanRes = ddb.scan(ScanRequest.builder().tableName("MenuItems").build());
            for (Map<String, AttributeValue> item : scanRes.items()) {
                ddb.deleteItem(DeleteItemRequest.builder()
                        .tableName("MenuItems")
                        .key(Map.of("categoryId", item.get("categoryId"), "itemId", item.get("itemId")))
                        .build());
            }
        } catch (Exception e) {
            System.err.println("Lỗi dọn dẹp MenuItems: " + e.getMessage());
        }

        Object[][] menu = {
            // CAT001: Khai vị & Gỏi
            {"CAT001", "MN001", "Gỏi Ngó Sen Tôm Thịt", 85000.0, "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500"},
            {"CAT001", "MN002", "Đậu Hũ Chiên Chao", 45000.0, "https://images.unsplash.com/photo-1512058560566-42724afbc1db?w=500"},
            {"CAT001", "MN003", "Khô Mực Nướng Tương Ớt", 120000.0, "https://images.unsplash.com/photo-1563379926898-05f4575a45d8?w=500"},

            // CAT002: Đặc sản Đồng Quê
            {"CAT002", "MN004", "Cá Lóc Nướng Trui", 155000.0, "https://images.unsplash.com/photo-1580476262798-bddd9f4b7369?w=500"},
            {"CAT002", "MN005", "Chuột Đồng Chiên Nước Mắm", 95000.0, "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=500"},
            {"CAT002", "MN006", "Ếch Núp Lùm (Chiên Rơm)", 110000.0, "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=500"},

            // CAT003: Món Nhậu Lai Rai
            {"CAT003", "MN007", "Ốc Bươu Hấp Tiêu Xanh", 75000.0, "https://images.unsplash.com/photo-1599481238640-4c1288750d7a?w=500"},
            {"CAT003", "MN008", "Cánh Gà Chiên Nước Mắm", 85000.0, "https://images.unsplash.com/photo-1567620905732-2d1ec7bb7445?w=500"},
            {"CAT003", "MN009", "Sụn Gà Rang Muối", 90000.0, "https://images.unsplash.com/photo-1606787366850-de6330128bfc?w=500"},

            // CAT004: Lẩu & Món Chính
            {"CAT004", "MN010", "Lẩu Mắm Miền Tây (Nhỏ)", 250000.0, "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=500"},
            {"CAT004", "MN011", "Vịt Nấu Chao", 185000.0, "https://images.unsplash.com/photo-1514326640560-7d063ef2aed5?w=500"},
            {"CAT004", "MN012", "Lẩu Cháo Cá Lóc", 220000.0, "https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=500"},

            // CAT005: Đồ uống
            {"CAT005", "MN013", "Rượu Nếp Gò Đen (Xị)", 45000.0, "https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=500"},
            {"CAT005", "MN014", "Bia Tiger (Thùng)", 420000.0, "https://images.unsplash.com/photo-1566633806327-68e152aaf26d?w=500"},
            {"CAT005", "MN015", "Nước Suối Lavie", 15000.0, "https://images.unsplash.com/photo-1559839734-2b71f1e3c7e0?w=500"}
        };

        for (Object[] m : menu) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("categoryId", s((String) m[0]));
            item.put("itemId",     s((String) m[1]));
            item.put("name",       s((String) m[2]));
            item.put("price",      n(String.valueOf(((Double) m[3]).longValue())));
            item.put("imageUrl",   s((String) m[4]));
            item.put("available",  AttributeValue.builder().bool(true).build());
            ddb.putItem(PutItemRequest.builder().tableName("MenuItems").item(item).build());
        }
        System.out.println("✓ Đã nạp Menu Nhậu Miền Tây với 15 món đặc sản.");
    }
private static void seedCustomers() {
    String[][] customers = {
        {"0987654321", "Nguyễn Văn A", "KhachHangThanThiet", "150"},
        {"0912345678", "Trần Thị B", "Moi", "0"},
        {"0900112233", "Lê Văn C", "VIP", "500"}
    };
    for (String[] c : customers) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("customerId", s(c[0]));
        item.put("name",       s(c[1]));
        item.put("membership",  s(c[2]));
        item.put("diemTichLuy", n(c[3]));
        ddb.putItem(PutItemRequest.builder().tableName("Customers").item(item).build());
    }
    System.out.println("✓ Đã nạp 3 khách hàng mẫu.");
}
private static void seedPromos() {
    LocalDate now = LocalDate.now();
    Object[][] promos = {
        {"KM001", "Giảm giá Khai trương", "Giảm 10% tổng hóa đơn", 10.0, now.minusDays(5).toString(), now.plusDays(30).toString()},
        {"KM002", "Mừng Lễ 30/4", "Giảm 20% cho nhóm trên 5 người", 20.0, now.minusDays(10).toString(), now.minusDays(1).toString()},
        {"KM003", "Sắp diễn ra - Quốc tế Thiếu nhi", "Tặng quà cho trẻ em", 5.0, now.plusDays(10).toString(), now.plusDays(20).toString()}
    };
    for (Object[] p : promos) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("maUuDai",     s((String) p[0]));
        item.put("name",       s((String) p[1]));
        item.put("description",s((String) p[2]));
        item.put("discountPercent", n(String.valueOf(p[3])));
        item.put("ngayBatDau", s((String) p[4]));
        item.put("ngayKetThuc",s((String) p[5]));
        ddb.putItem(PutItemRequest.builder().tableName("Promos").item(item).build());
    }
    System.out.println("✓ Đã nạp 3 chương trình khuyến mãi (Đúng cấu trúc Database).");
}
private static AttributeValue s(String v) { return AttributeValue.builder().s(v != null ? v : "").build(); }
private static AttributeValue n(String v) { return AttributeValue.builder().n(v).build(); }
}