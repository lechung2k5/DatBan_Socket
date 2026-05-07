package dao;
import entity.*;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
* TableDAO  CLO1: 5 phương thức CRUD đầy đủ trên AWS DynamoDB
*
* DynamoDB Table: "Tables"
* PK: maBan (String)
*/
public class TableDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Tables";
    // ─── 1. Lấy tất cả bàn ───────────────────────────────────────────────────
    public List<Ban> findAll() {
        List<Ban> bans = new ArrayList<>();
        try {
            ScanResponse response = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .consistentRead(true)
                    .build());
            for (Map<String, AttributeValue> item : response.items()) {
                bans.add(mapToBan(item));
            }
            System.out.println("[DAO:Tables] findAll → " + bans.size() + " bàn");
        } catch (Exception e) {
        System.err.println("[DAO:Tables] Lỗi findAll: " + e.getMessage());
    }
    return bans;
}
// ─── 2. Tìm theo ID ──────────────────────────────────────────────────────
public Ban findById(String maBan) {
    try {
        Map<String, AttributeValue> key = Map.of(
        "maBan", AttributeValue.builder().s(maBan).build()
        );
        GetItemResponse res = db.getItem(GetItemRequest.builder()
                .tableName(TBL)
                .key(key)
                .consistentRead(true)
                .build());
        if (res.hasItem() && !res.item().isEmpty()) {
            return mapToBan(res.item());
        }
    } catch (Exception e) {
    System.err.println("[DAO:Tables] Lỗi findById(" + maBan + "): " + e.getMessage());
}
return null;
}
// ─── 3. Thêm / Upsert ────────────────────────────────────────────────────
public void insert(Ban ban) {
    try {
        Map<String, AttributeValue> item = toMap(ban);
        db.putItem(PutItemRequest.builder().tableName(TBL).item(item).build());
        System.out.println("[DAO:Tables] insert → " + ban.getMaBan());
    } catch (Exception e) {
    System.err.println("[DAO:Tables] Lỗi insert: " + e.getMessage());
}
}
// ─── 4. Cập nhật trạng thái (concurrency-safe với conditionExpression) ───
public boolean updateStatus(String maBan, String newStatus) {
    try {
        db.updateItem(UpdateItemRequest.builder()
        .tableName(TBL)
        .key(Map.of("maBan", AttributeValue.builder().s(maBan).build()))
        .updateExpression("SET #s = :status, updatedAt = :ts")
        .expressionAttributeNames(Map.of("#s", "trangThai"))
        .expressionAttributeValues(Map.of(
        ":status", AttributeValue.builder().s(newStatus).build(),
        ":ts",     AttributeValue.builder().s(LocalDateTime.now().toString()).build()
        ))
        .conditionExpression("attribute_exists(maBan)") // tránh tạo record mới
        .build());
        System.out.println("[DAO:Tables] updateStatus(" + maBan + " → " + newStatus + ") ✓");
        return true;
    } catch (ConditionalCheckFailedException e) {
    System.err.println("[DAO:Tables] Bàn không tồn tại: " + maBan);
    return false;
} catch (Exception e) {
System.err.println("[DAO:Tables] Lỗi updateStatus: " + e.getMessage());
return false;
}
}
// ─── 5. Xóa ──────────────────────────────────────────────────────────────
public void delete(String maBan) {
    try {
        db.deleteItem(DeleteItemRequest.builder()
        .tableName(TBL)
        .key(Map.of("maBan", AttributeValue.builder().s(maBan).build()))
        .build());
        System.out.println("[DAO:Tables] delete(" + maBan + ") ✓");
    } catch (Exception e) {
    System.err.println("[DAO:Tables] Lỗi delete: " + e.getMessage());
}
}
// ─── Helpers ─────────────────────────────────────────────────────────────
private Map<String, AttributeValue> toMap(Ban ban) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("maBan",    AttributeValue.builder().s(ban.getMaBan()).build());
    item.put("viTri",    AttributeValue.builder().s(ban.getViTri() != null ? ban.getViTri() : "").build());
    item.put("sucChua",  AttributeValue.builder().n(String.valueOf(ban.getSucChua())).build());
    if (ban.getLoaiBan()  != null) item.put("maLoaiBan", AttributeValue.builder().s(ban.getLoaiBan().toString()).build());
    if (ban.getTrangThai()!= null) item.put("trangThai", AttributeValue.builder().s(ban.getTrangThai().getDbValue()).build());
    item.put("updatedAt", AttributeValue.builder().s(LocalDateTime.now().toString()).build());
    return item;
}
private Ban mapToBan(Map<String, AttributeValue> item) {
    Ban ban = new Ban();
    if (item.containsKey("maBan"))    ban.setMaBan(item.get("maBan").s());
    if (item.containsKey("viTri"))    ban.setViTri(item.get("viTri").s());
    if (item.containsKey("sucChua"))  ban.setSucChua(Integer.parseInt(item.get("sucChua").n()));
    if (item.containsKey("trangThai"))ban.setTrangThai(TrangThaiBan.fromDbValue(item.get("trangThai").s()));
    if (item.containsKey("maLoaiBan"))ban.setLoaiBan(LoaiBan.fromString(item.get("maLoaiBan").s()));
    return ban;
}
}