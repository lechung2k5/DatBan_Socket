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
private void putIfNotEmpty(Map<String, AttributeValue> item, String key, String value) {
    if (value != null && !value.trim().isEmpty()) {
        item.put(key, AttributeValue.builder().s(value.trim()).build());
    }
}

public void upsert(NhanVien nv) {
    Map<String, AttributeValue> item = new HashMap<>();
    putIfNotEmpty(item, "employeeId", nv.getMaNV());
    putIfNotEmpty(item, "name", nv.getHoTen());
    putIfNotEmpty(item, "phone", nv.getSdt());
    putIfNotEmpty(item, "email", nv.getEmail());
    
    String role = nv.getChucVu();
    if (role == null || role.trim().isEmpty()) role = "NhanVien";
    putIfNotEmpty(item, "role", role);
    
    String pw = nv.getMatKhau();
    if (pw == null || pw.trim().isEmpty()) pw = "123";
    putIfNotEmpty(item, "passwordHash", pw);
    
    String username = nv.getUsername_entity();
    if (username == null || username.trim().isEmpty()) username = nv.getMaNV();
    putIfNotEmpty(item, "username", username);

    putIfNotEmpty(item, "dob", nv.getNgaySinh());
    putIfNotEmpty(item, "favoriteShift", nv.getCaLamYeuThich());
    putIfNotEmpty(item, "status", nv.getTrangThai());

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

private String getSafeString(Map<String, AttributeValue> item, String key) {
    if (item.containsKey(key)) {
        AttributeValue val = item.get(key);
        if (val.s() != null) return val.s();
        if (val.n() != null) return val.n();
    }
    return "";
}

private NhanVien mapToNhanVien(Map<String, AttributeValue> item) {
    NhanVien nv = new NhanVien();
    nv.setMaNV(getSafeString(item, "employeeId"));
    nv.setHoTen(getSafeString(item, "name"));
    nv.setSdt(getSafeString(item, "phone"));
    nv.setEmail(getSafeString(item, "email"));
    nv.setChucVu(getSafeString(item, "role"));
    nv.setMatKhau(getSafeString(item, "passwordHash")); // Giư xài thuộc tính mặt khȣu để chứa passwordHash
    nv.setUsername_entity(getSafeString(item, "username"));

    String dob = getSafeString(item, "dob");
    if (!dob.isEmpty()) nv.setNgaySinh(dob);
    
    String favoriteShift = getSafeString(item, "favoriteShift");
    if (!favoriteShift.isEmpty()) nv.setCaLamYeuThich(favoriteShift);
    
    String status = getSafeString(item, "status");
    if (!status.isEmpty()) {
        nv.setTrangThai(status);
    } else {
        nv.setTrangThai("Đang làm");
    }
    return nv;
}
}