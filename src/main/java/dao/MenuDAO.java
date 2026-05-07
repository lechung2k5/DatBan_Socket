package dao;
import entity.MonAn;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.core.SdkBytes;
import java.util.*;
import java.util.stream.Collectors;
/**
* MenuDAO  CRUD cho bảng MenuItems
*
* DynamoDB Table: "MenuItems"
* PK: categoryId (S), SK: itemId (S)
*/
public class MenuDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "MenuItems";
    // ─── 1. Lấy tất cả món ───────────────────────────────────────────────────
    public List<MonAn> findAll() {
        List<MonAn> list = new ArrayList<>();
        try {
            ScanResponse res = db.scan(ScanRequest.builder().tableName(TBL).build());
            res.items().forEach(item -> list.add(mapToMonAn(item)));
            System.out.println("[DAO:Menu] findAll → " + list.size() + " món");
        } catch (Exception e) {
        System.err.println("[DAO:Menu] Lỗi findAll: " + e.getMessage());
    }
    return list;
}
// ─── 2. Tìm theo ID ──────────────────────────────────────────────────────
public MonAn findById(String categoryId, String itemId) {
    try {
        Map<String, AttributeValue> key = Map.of(
        "categoryId", av(categoryId),
        "itemId",     av(itemId)
        );
        GetItemResponse res = db.getItem(GetItemRequest.builder().tableName(TBL).key(key).build());
        if (res.hasItem() && !res.item().isEmpty()) return mapToMonAn(res.item());
    } catch (Exception e) {
    System.err.println("[DAO:Menu] Lỗi findById: " + e.getMessage());
}
return null;
}
// ─── 3. Thêm mới ─────────────────────────────────────────────────────────
public void insert(MonAn monAn) {
    putItem(monAn);
    System.out.println("[DAO:Menu] insert → " + monAn.getMaMon());
}
// ─── 4. Cập nhật ─────────────────────────────────────────────────────────
public void update(MonAn monAn) {
    try {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":name", av(monAn.getTenMon()));
        values.put(":price", avn(monAn.getGiaBan()));
        values.put(":avail", AttributeValue.builder().bool(true).build());
        
        String updateExpr = "SET #n = :name, price = :price, available = :avail";
        if (monAn.getHinhAnhUrl() != null && !monAn.getHinhAnhUrl().isEmpty()) {
            updateExpr += ", imageUrl = :img";
            values.put(":img", av(monAn.getHinhAnhUrl()));
        }
        if (monAn.getHinhAnh() != null && monAn.getHinhAnh().length > 0) {
            updateExpr += ", hinhAnh = :bin";
            values.put(":bin", AttributeValue.builder().b(SdkBytes.fromByteArray(monAn.getHinhAnh())).build());
        }

        db.updateItem(UpdateItemRequest.builder()
        .tableName(TBL)
        .key(Map.of(
        "categoryId", av(monAn.getMaDM()),
        "itemId",     av(monAn.getMaMon())
        ))
        .updateExpression(updateExpr)
        .expressionAttributeNames(Map.of("#n", "name"))
        .expressionAttributeValues(values)
        .conditionExpression("attribute_exists(itemId)")
        .build());
        System.out.println("[DAO:Menu] update → " + monAn.getMaMon() + " ✓");
    } catch (ConditionalCheckFailedException e) {
    System.err.println("[DAO:Menu] Món không tồn tại, chuyển sang insert: " + monAn.getMaMon());
    insert(monAn);
} catch (Exception e) {
System.err.println("[DAO:Menu] Lỗi update: " + e.getMessage());
}
}
// ─── 5. Xóa ──────────────────────────────────────────────────────────────
public void delete(String categoryId, String itemId) {
    try {
        db.deleteItem(DeleteItemRequest.builder()
        .tableName(TBL)
        .key(Map.of(
        "categoryId", av(categoryId),
        "itemId",     av(itemId)
        ))
        .build());
        System.out.println("[DAO:Menu] delete(" + itemId + ") ✓");
    } catch (Exception e) {
    System.err.println("[DAO:Menu] Lỗi delete: " + e.getMessage());
}
}
// ─── Lấy tất cả danh mục (unique) ────────────────────────────────────────
    public List<entity.DanhMucMon> findAllCategories() {
        try {
            // Trong DynamoDB "MenuItems", chúng ta gom nhóm categoryId
            // Tuy nhiên để có Tên Danh Mục, ta có thể hardcode mapping hoặc lấy từ field projection
            // Ở đây ta giả sử categoryId là duy nhất và ta map sang tên thân thiện
            List<String> ids = db.scan(ScanRequest.builder()
                .tableName(TBL)
                .projectionExpression("categoryId")
                .build()).items().stream()
                .map(m -> m.get("categoryId").s())
                .distinct().sorted()
                .collect(Collectors.toList());
                
            List<entity.DanhMucMon> list = new ArrayList<>();
            for (String id : ids) {
                String name = switch(id) {
                    case "CAT001" -> "Khai vị & Gỏi";
                    case "CAT002" -> "Đặc sản Đồng Quê";
                    case "CAT003" -> "Món Nhậu Lai Rai";
                    case "CAT004" -> "Lẩu & Món Chính";
                    case "CAT005" -> "Đồ uống";
                    default -> id; 
                };
                list.add(new entity.DanhMucMon(id, name));
            }
            return list;
        } catch (Exception e) {
            System.err.println("[DAO:Menu] Lỗi findAllCategories: " + e.getMessage());
            return new ArrayList<>();
        }
    }
// ─── Helpers ─────────────────────────────────────────────────────────────
private void putItem(MonAn monAn) {
    try {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("categoryId", av(monAn.getMaDM() != null ? monAn.getMaDM() : "DEFAULT"));
        item.put("itemId",     av(monAn.getMaMon()));
        item.put("name",       av(monAn.getTenMon()));
        item.put("price",      avn(monAn.getGiaBan()));
        item.put("available",  AttributeValue.builder().bool(true).build());
        if (monAn.getHinhAnhUrl() != null && !monAn.getHinhAnhUrl().isEmpty()) {
            item.put("imageUrl", av(monAn.getHinhAnhUrl()));
        }
        if (monAn.getHinhAnh() != null && monAn.getHinhAnh().length > 0) {
            item.put("hinhAnh", AttributeValue.builder().b(SdkBytes.fromByteArray(monAn.getHinhAnh())).build());
        }
        db.putItem(PutItemRequest.builder().tableName(TBL).item(item).build());
    } catch (Exception e) {
    System.err.println("[DAO:Menu] Lỗi putItem: " + e.getMessage());
}
}
private AttributeValue av(String s)  { return AttributeValue.builder().s(s != null ? s : "").build(); }
private AttributeValue avn(double n) { return AttributeValue.builder().n(String.valueOf(n)).build(); }
private MonAn mapToMonAn(Map<String, AttributeValue> item) {
    MonAn m = new MonAn();
    if (item.containsKey("itemId"))     m.setMaMon(item.get("itemId").s());
    if (item.containsKey("name"))       m.setTenMon(item.get("name").s());
    if (item.containsKey("price"))      m.setGiaBan(Double.parseDouble(item.get("price").n()));
    if (item.containsKey("categoryId")) m.setMaDM(item.get("categoryId").s());
    if (item.containsKey("imageUrl"))   m.setHinhAnhUrl(item.get("imageUrl").s());
    
    // Hỗ trợ ảnh nhị phân (legacy)
    if (item.containsKey("hinhAnh") && item.get("hinhAnh").b() != null) {
        m.setHinhAnh(item.get("hinhAnh").b().asByteArray());
    }
    return m;
}
}