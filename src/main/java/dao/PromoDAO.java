package dao;
import entity.UuDai;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/**
* PromoDAO - Quản lý chương trình khuyến mãi (UuDai)
*/
public class PromoDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Promos";
    public List<UuDai> findAll() {
        List<UuDai> list = new ArrayList<>();
        try {
            ScanResponse response = db.scan(ScanRequest.builder().tableName(TBL).build());
            for (Map<String, AttributeValue> item : response.items()) {
                list.add(mapToUuDai(item));
            }
        } catch (Exception e) {
        System.err.println("[DAO:Promos] Lỗi findAll: " + e.getMessage());
    }
    return list;
}
public void insert(UuDai ud) {
    try {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("maUuDai", AttributeValue.builder().s(ud.getMaUuDai()).build());
        item.put("name", AttributeValue.builder().s(ud.getTenUuDai()).build());
        item.put("discountPercent", AttributeValue.builder().n(String.valueOf(ud.getGiaTri())).build());
        item.put("description", AttributeValue.builder().s(ud.getMoTa() != null && !ud.getMoTa().trim().isEmpty() ? ud.getMoTa() : "Không có mô tả").build());
        if (ud.getNgayBatDau() != null) {
            item.put("ngayBatDau", AttributeValue.builder().s(ud.getNgayBatDau().toString()).build());
        }
        if (ud.getNgayKetThuc() != null) {
            item.put("ngayKetThuc", AttributeValue.builder().s(ud.getNgayKetThuc().toString()).build());
        }
        db.putItem(PutItemRequest.builder().tableName(TBL).item(item).build());
    } catch (Exception e) {
        System.err.println("[DAO:Promos] Lỗi insert: " + e.getMessage());
        throw new RuntimeException("Lỗi lưu DB: " + e.getMessage()); // Re-throw để Server báo lỗi
    }
}
public UuDai findById(String id) {
    try {
        GetItemResponse response = db.getItem(GetItemRequest.builder()
        .tableName(TBL)
        .key(Map.of("maUuDai", AttributeValue.builder().s(id).build()))
        .build());
        if (response.hasItem() && !response.item().isEmpty()) {
            return mapToUuDai(response.item());
        }
    } catch (Exception e) {
    System.err.println("[DAO:Promos] Lỗi findById: " + e.getMessage());
}
return null;
}
public void delete(String id) {
    try {
        db.deleteItem(DeleteItemRequest.builder()
        .tableName(TBL)
        .key(Map.of("maUuDai", AttributeValue.builder().s(id).build()))
        .build());
    } catch (Exception e) {
    System.err.println("[DAO:Promos] Lỗi delete: " + e.getMessage());
}
}
private UuDai mapToUuDai(Map<String, AttributeValue> item) {
    UuDai ud = new UuDai();
    // Hỗ trợ cả tên cột mới (maUuDai) và cũ (promoId) để backward compatible
    if (item.containsKey("maUuDai")) ud.setMaUuDai(item.get("maUuDai").s());
    else if (item.containsKey("promoId")) ud.setMaUuDai(item.get("promoId").s());
    if (item.containsKey("name")) ud.setTenUuDai(item.get("name").s());
    if (item.containsKey("discountPercent")) ud.setGiaTri(Double.parseDouble(item.get("discountPercent").n()));
    if (item.containsKey("description")) ud.setMoTa(item.get("description").s());
    // Hỗ trợ cả tên cột mới (ngayBatDau) và cũ (startDate/ngayBatDau từ seed) để backward compatible
    if (item.containsKey("ngayBatDau")) { try { ud.setNgayBatDau(LocalDate.parse(item.get("ngayBatDau").s())); } catch (Exception ignored) {} }
    else if (item.containsKey("startDate")) { try { ud.setNgayBatDau(LocalDate.parse(item.get("startDate").s())); } catch (Exception ignored) {} }
    if (item.containsKey("ngayKetThuc")) { try { ud.setNgayKetThuc(LocalDate.parse(item.get("ngayKetThuc").s())); } catch (Exception ignored) {} }
    else if (item.containsKey("endDate")) { try { ud.setNgayKetThuc(LocalDate.parse(item.get("endDate").s())); } catch (Exception ignored) {} }
    return ud;
}
}