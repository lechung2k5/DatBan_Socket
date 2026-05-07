package dao;
import entity.NhanVien;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.HashMap;
import java.util.Map;
public class EmployeeDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Employees";
    public NhanVien findByUsername(String username) {
        // Tìm bằng username attribute
        Map<String, Condition> filter = new HashMap<>();
        filter.put("username", Condition.builder()
        .comparisonOperator(ComparisonOperator.EQ)
        .attributeValueList(AttributeValue.builder().s(username).build())
        .build());
        ScanRequest request = ScanRequest.builder()
        .tableName(TBL)
        .scanFilter(filter)
        .build();
        try {
            ScanResponse response = db.scan(request);
            if (!response.items().isEmpty()) {
                return mapToNhanVien(response.items().get(0));
            }
            // Fallback: Nếu không tìm thấy bằng username, thử tìm bằng employeeId (maNV)
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("employeeId", AttributeValue.builder().s(username).build());
            GetItemResponse getRes = db.getItem(GetItemRequest.builder().tableName(TBL).key(key).build());
            if (getRes.hasItem()) return mapToNhanVien(getRes.item());
        } catch (Exception e) {
        System.err.println("Lỗi khi tìm nhân viên: " + e.getMessage());
    }
    return null;
}
public java.util.List<NhanVien> findAll() {
    ScanRequest scanRequest = ScanRequest.builder().tableName(TBL).build();
    java.util.List<NhanVien> list = new java.util.ArrayList<>();
    try {
        ScanResponse response = db.scan(scanRequest);
        for (Map<String, AttributeValue> item : response.items()) {
            list.add(mapToNhanVien(item));
        }
    } catch (Exception e) {
    System.err.println("Lỗi khi lấy danh sách nhân viên: " + e.getMessage());
}
return list;
}
public void upsert(NhanVien nv) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("employeeId", AttributeValue.builder().s(nv.getMaNV()).build());
    item.put("name", AttributeValue.builder().s(nv.getHoTen() != null ? nv.getHoTen() : "").build());
    item.put("phone", AttributeValue.builder().s(nv.getSdt() != null ? nv.getSdt() : "").build());
    item.put("email", AttributeValue.builder().s(nv.getEmail() != null ? nv.getEmail() : "").build());
    item.put("role", AttributeValue.builder().s(nv.getChucVu() != null ? nv.getChucVu() : "NhanVien").build());
    item.put("passwordHash", AttributeValue.builder().s(nv.getMatKhau() != null ? nv.getMatKhau() : "123").build());
    item.put("username", AttributeValue.builder().s(nv.getUsername_entity() != null ? nv.getUsername_entity() : nv.getMaNV()).build());
    db.putItem(PutItemRequest.builder().tableName(TBL).item(item).build());
}
public void updatePassword(String maNV, String newPassword) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put("employeeId", AttributeValue.builder().s(maNV).build());
    Map<String, AttributeValueUpdate> updates = new HashMap<>();
    updates.put("passwordHash", AttributeValueUpdate.builder()
    .value(AttributeValue.builder().s(newPassword).build())
    .action(AttributeAction.PUT)
    .build());
    UpdateItemRequest request = UpdateItemRequest.builder()
    .tableName(TBL)
    .key(key)
    .attributeUpdates(updates)
    .build();
    db.updateItem(request);
}
private NhanVien mapToNhanVien(Map<String, AttributeValue> item) {
    NhanVien nv = new NhanVien();
    if (item.containsKey("employeeId")) nv.setMaNV(item.get("employeeId").s());
    if (item.containsKey("name")) nv.setHoTen(item.get("name").s());
    if (item.containsKey("phone")) nv.setSdt(item.get("phone").s());
    if (item.containsKey("email")) nv.setEmail(item.get("email").s());
    if (item.containsKey("role")) nv.setChucVu(item.get("role").s());
    if (item.containsKey("passwordHash")) nv.setMatKhau(item.get("passwordHash").s()); // Giư xài thuộc tính mặt khȣu để chứa passwordHash
    if (item.containsKey("username")) nv.setUsername_entity(item.get("username").s());
    return nv;
}
}