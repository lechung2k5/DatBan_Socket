package dao;
import entity.KhachHang;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDateTime;
import java.util.*;
/**
* CustomerDAO  CRUD cho bảng Customers
*
* DynamoDB Table: "Customers"
* PK: customerId = số điện thoại (S)
*/
public class CustomerDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Customers";
    // ─── 1. Tìm theo số điện thoại (PK) ──────────────────────────────────────
    public KhachHang findByPhone(String phone) {
        try {
            GetItemResponse res = db.getItem(GetItemRequest.builder()
            .tableName(TBL)
            .key(Map.of("customerId", av(phone)))
            .build());
            if (res.hasItem() && !res.item().isEmpty()) return mapToKhachHang(res.item());
        } catch (Exception e) {
        System.err.println("[DAO:Customers] Lỗi findByPhone: " + e.getMessage());
    }
    return null;
}
// ─── 2. Lấy tất cả ───────────────────────────────────────────────────────
public List<KhachHang> findAll() {
    List<KhachHang> list = new ArrayList<>();
    try {
        db.scan(ScanRequest.builder().tableName(TBL).build())
        .items().forEach(item -> list.add(mapToKhachHang(item)));
        System.out.println("[DAO:Customers] findAll → " + list.size() + " KH");
    } catch (Exception e) {
    System.err.println("[DAO:Customers] Lỗi findAll: " + e.getMessage());
}
return list;
}
// ─── 3. Thêm mới ─────────────────────────────────────────────────────────
public void insert(KhachHang kh) {
    try {
        db.putItem(PutItemRequest.builder()
        .tableName(TBL)
        .item(toMap(kh))
        .conditionExpression("attribute_not_exists(customerId)") // tránh ghi đè
        .build());
        System.out.println("[DAO:Customers] insert → " + kh.getSoDT());
    } catch (ConditionalCheckFailedException e) {
    System.out.println("[DAO:Customers] KH đã tồn tại: " + kh.getSoDT() + " → bỏ qua");
} catch (Exception e) {
System.err.println("[DAO:Customers] Lỗi insert: " + e.getMessage());
}
}
// ─── 4. Cập nhật thông tin ────────────────────────────────────────────────
public void update(KhachHang kh) {
    try {
        db.updateItem(UpdateItemRequest.builder()
        .tableName(TBL)
        .key(Map.of("customerId", av(kh.getSoDT())))
        .updateExpression("SET #n = :name, membership = :m, updatedAt = :ts")
        .expressionAttributeNames(Map.of("#n", "name"))
        .expressionAttributeValues(Map.of(
        ":name", av(kh.getTenKH()),
        ":m",    av(kh.getThanhVien() != null ? kh.getThanhVien() : ""),
        ":ts",   av(LocalDateTime.now().toString())
        ))
        .build());
        System.out.println("[DAO:Customers] update → " + kh.getSoDT() + " ✓");
    } catch (Exception e) {
    System.err.println("[DAO:Customers] Lỗi update: " + e.getMessage());
}
}
// ─── 5. Xóa ──────────────────────────────────────────────────────────────
public void delete(String phone) {
    try {
        db.deleteItem(DeleteItemRequest.builder()
        .tableName(TBL)
        .key(Map.of("customerId", av(phone)))
        .build());
        System.out.println("[DAO:Customers] delete(" + phone + ") ✓");
    } catch (Exception e) {
    System.err.println("[DAO:Customers] Lỗi delete: " + e.getMessage());
}
}
// ─── Tìm hoặc tạo mới (dùng trong CreateOrder flow) ──────────────────────
public KhachHang findOrCreate(String phone, String name) {
    KhachHang kh = findByPhone(phone);
    if (kh != null) return kh;
    KhachHang newKH = new KhachHang();
    newKH.setSoDT(phone);
    newKH.setTenKH(name != null ? name : "Khách " + phone);
    insert(newKH);
    return newKH;
}
// ─── Helpers ─────────────────────────────────────────────────────────────
private AttributeValue av(String s)  { return AttributeValue.builder().s(s != null ? s : "").build(); }
private AttributeValue avn(double n) { return AttributeValue.builder().n(String.valueOf(n)).build(); }
private Map<String, AttributeValue> toMap(KhachHang kh) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("customerId",  av(kh.getSoDT()));
    item.put("name",        av(kh.getTenKH()));
    item.put("membership",  av(kh.getThanhVien() != null ? kh.getThanhVien() : ""));
    item.put("createdAt",   av(LocalDateTime.now().toString()));
    return item;
}
private KhachHang mapToKhachHang(Map<String, AttributeValue> m) {
    KhachHang kh = new KhachHang();
    if (m.containsKey("customerId")) kh.setSoDT(m.get("customerId").s());
    if (m.containsKey("name"))       kh.setTenKH(m.get("name").s());
    if (m.containsKey("membership")) kh.setThanhVien(m.get("membership").s());
    return kh;
}
}