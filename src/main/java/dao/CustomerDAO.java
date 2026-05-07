package dao;
import entity.KhachHang;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CustomerDAO - CRUD cho bảng Customers
 */
public class CustomerDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Customers";

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

    public List<KhachHang> findAll() {
        List<KhachHang> list = new ArrayList<>();
        try {
            db.scan(ScanRequest.builder().tableName(TBL).build())
                .items().forEach(item -> list.add(mapToKhachHang(item)));
        } catch (Exception e) {
            System.err.println("[DAO:Customers] Lỗi findAll: " + e.getMessage());
        }
        return list;
    }

    public void insert(KhachHang kh) {
        try {
            db.putItem(PutItemRequest.builder()
                .tableName(TBL)
                .item(toMap(kh))
                .conditionExpression("attribute_not_exists(customerId)")
                .build());
        } catch (ConditionalCheckFailedException e) {
            System.out.println("[DAO:Customers] KH đã tồn tại: " + kh.getSoDT());
        } catch (Exception e) {
            System.err.println("[DAO:Customers] Lỗi insert: " + e.getMessage());
        }
    }

    public void update(KhachHang kh) {
        try {
            db.updateItem(UpdateItemRequest.builder()
                .tableName(TBL)
                .key(Map.of("customerId", av(kh.getSoDT())))
                .updateExpression("SET #n = :name, email = :e, diaChi = :d, ngayDangKy = :rd, membership = :m, diemTichLuy = :p, updatedAt = :ts")
                .expressionAttributeNames(Map.of("#n", "name"))
                .expressionAttributeValues(Map.of(
                    ":name", av(kh.getTenKH()),
                    ":e",    av(kh.getEmail()),
                    ":d",    av(kh.getDiaChi()),
                    ":rd",   av(kh.getNgayDangKy() != null ? kh.getNgayDangKy().toString() : ""),
                    ":m",    av(kh.getThanhVien() != null ? kh.getThanhVien() : ""),
                    ":p",    AttributeValue.builder().n(String.valueOf(kh.getDiemTichLuy())).build(),
                    ":ts",   av(LocalDateTime.now().toString())
                ))
                .build());
        } catch (Exception e) {
            System.err.println("[DAO:Customers] Lỗi update: " + e.getMessage());
        }
    }

    public void delete(String phone) {
        try {
            db.deleteItem(DeleteItemRequest.builder()
                .tableName(TBL)
                .key(Map.of("customerId", av(phone)))
                .build());
        } catch (Exception e) {
            System.err.println("[DAO:Customers] Lỗi delete: " + e.getMessage());
        }
    }

    public void addPointsAndAdjustLevel(String phone, int pointsToAdd) {
        try {
            KhachHang kh = findByPhone(phone);
            if (kh == null) return;
            int newPoints = kh.getDiemTichLuy() + pointsToAdd;
            String newTier = "Member";
            if (newPoints >= 450) newTier = "Diamond";
            else if (newPoints >= 200) newTier = "Gold";
            db.updateItem(UpdateItemRequest.builder()
                .tableName(TBL)
                .key(Map.of("customerId", av(phone)))
                .updateExpression("SET diemTichLuy = :p, membership = :m, updatedAt = :ts")
                .expressionAttributeValues(Map.of(
                    ":p",  AttributeValue.builder().n(String.valueOf(newPoints)).build(),
                    ":m",  av(newTier),
                    ":ts", av(LocalDateTime.now().toString())
                ))
                .build());
        } catch (Exception e) {
            System.err.println("[DAO:Customers] Lỗi addPoints: " + e.getMessage());
        }
    }

    public KhachHang findOrCreate(String phone, String name) {
        KhachHang kh = findByPhone(phone);
        if (kh == null) {
            upsert(phone, name);
            return findByPhone(phone);
        }
        return kh;
    }

    public void upsert(String phone, String name) {
        try {
            db.updateItem(UpdateItemRequest.builder()
                .tableName(TBL)
                .key(Map.of("customerId", av(phone)))
                .updateExpression("SET #n = :name, membership = if_not_exists(membership, :m), registrationDate = if_not_exists(registrationDate, :rd), updatedAt = :ts")
                .expressionAttributeNames(Map.of("#n", "name"))
                .expressionAttributeValues(Map.of(
                    ":name", av(name != null ? name : "Khách hàng " + phone),
                    ":m",    av("Member"),
                    ":rd",   av(LocalDate.now().toString()),
                    ":ts",   av(LocalDateTime.now().toString())
                ))
                .build());
        } catch (Exception e) {
            System.err.println("[DAO:Customers] Lỗi upsert: " + e.getMessage());
        }
    }

    private AttributeValue av(String s)  { return AttributeValue.builder().s(s != null ? s : "").build(); }

    private Map<String, AttributeValue> toMap(KhachHang kh) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("customerId",  av(kh.getSoDT()));
        item.put("name",        av(kh.getTenKH()));
        item.put("email",       av(kh.getEmail()));
        item.put("diaChi",      av(kh.getDiaChi()));
        item.put("membership",  av(kh.getThanhVien() != null ? kh.getThanhVien() : ""));
        item.put("diemTichLuy", AttributeValue.builder().n(String.valueOf(kh.getDiemTichLuy())).build());
        item.put("registrationDate", av(kh.getNgayDangKy() != null ? kh.getNgayDangKy().toString() : LocalDate.now().toString()));
        item.put("updatedAt",   av(LocalDateTime.now().toString()));
        return item;
    }

    private KhachHang mapToKhachHang(Map<String, AttributeValue> m) {
        KhachHang kh = new KhachHang();
        if (m.containsKey("customerId")) {
            String phone = m.get("customerId").s();
            kh.setSoDT(phone);
            kh.setMaKH(phone);
        }
        if (m.containsKey("name")) kh.setTenKH(m.get("name").s());
        if (m.containsKey("email")) kh.setEmail(m.get("email").s());
        if (m.containsKey("diaChi")) kh.setDiaChi(m.get("diaChi").s());
        if (m.containsKey("membership")) kh.setThanhVien(m.get("membership").s());
        if (m.containsKey("diemTichLuy")) kh.setDiemTichLuy(Integer.parseInt(m.get("diemTichLuy").n()));
        if (m.containsKey("registrationDate")) {
            try {
                String dateStr = m.get("registrationDate").s();
                if (!dateStr.isEmpty()) {
                    if (dateStr.contains("T")) {
                        kh.setNgayDangKy(java.time.LocalDateTime.parse(dateStr).toLocalDate());
                    } else {
                        kh.setNgayDangKy(java.time.LocalDate.parse(dateStr));
                    }
                }
            } catch (Exception e) {}
        }
        return kh;
    }
}
