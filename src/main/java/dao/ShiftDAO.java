package dao;
import db.DynamoDBConfig;
import entity.CaTruc;
import entity.NhanVien;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class ShiftDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Shifts";
    private static boolean tableEnsured = false;

    public ShiftDAO() {
        ensureTableExists();
    }

    // ─── Tự động tạo bảng nếu chưa tồn tại ───────────────────────────────
    private synchronized void ensureTableExists() {
        if (tableEnsured) return;
        try {
            db.describeTable(DescribeTableRequest.builder().tableName(TBL).build());
            System.out.println("[ShiftDAO] Bảng " + TBL + " đã tồn tại.");
        } catch (ResourceNotFoundException e) {
            System.out.println("[ShiftDAO] Bảng " + TBL + " chưa tồn tại. Đang tạo...");
            db.createTable(CreateTableRequest.builder()
                .tableName(TBL)
                .keySchema(KeySchemaElement.builder().attributeName("maCa").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder()
                    .attributeName("maCa")
                    .attributeType(ScalarAttributeType.S)
                    .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
            // Chờ bảng được tạo xong
            db.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(TBL).build());
            System.out.println("[ShiftDAO] Đã tạo bảng " + TBL + " thành công!");
        } catch (Exception e) {
            System.err.println("[ShiftDAO] Lỗi khi kiểm tra/tạo bảng: " + e.getMessage());
        }
        tableEnsured = true;
    }

    // ─── Lấy ca trực theo tuần của 1 nhân viên ────────────────────────────
    public List<CaTruc> findWeeklyByEmployee(LocalDate startOfWeek, String maNV) {
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return scanByRange(startOfWeek, endOfWeek).stream()
            .filter(ca -> ca.getNhanVien() != null && maNV.equals(ca.getNhanVien().getMaNV()))
            .collect(Collectors.toList());
    }

    // ─── Lấy ca trực theo tháng của 1 nhân viên ───────────────────────────
    public List<CaTruc> findMonthlyByEmployee(String maNV, LocalDate startOfMonth, LocalDate endOfMonth) {
        return scanByRange(startOfMonth, endOfMonth).stream()
            .filter(ca -> ca.getNhanVien() != null && maNV.equals(ca.getNhanVien().getMaNV()))
            .collect(Collectors.toList());
    }

    // ─── Lấy tất cả ca trực trong 1 tuần (dành cho Admin) ─────────────────
    public List<CaTruc> findAllByWeek(LocalDate startOfWeek) {
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return scanByRange(startOfWeek, endOfWeek);
    }

    // ─── Tính tổng số giờ lịch trong khoảng thời gian ─────────────────────
    public double getScheduledHours(String maNV, LocalDate start, LocalDate end) {
        return findMonthlyByEmployee(maNV, start, end).stream()
            .mapToLong(ca -> {
                java.time.Duration d = java.time.Duration.between(ca.getGioBatDau(), ca.getGioKetThuc());
                if (d.isNegative()) d = d.plusHours(24);
                return d.toMinutes();
            })
            .sum() / 60.0;
    }

    // ─── Upsert (Thêm hoặc cập nhật) ──────────────────────────────────────
    public void upsert(CaTruc ca) {
        if (ca.getMaCa() == null || ca.getMaCa().isEmpty()) {
            ca.setMaCa(generateNewId());
        }
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("maCa",       AttributeValue.builder().s(ca.getMaCa()).build());
        item.put("ngay",       AttributeValue.builder().s(ca.getNgay().toString()).build());
        item.put("gioBatDau",  AttributeValue.builder().s(ca.getGioBatDau().toString()).build());
        item.put("gioKetThuc", AttributeValue.builder().s(ca.getGioKetThuc().toString()).build());
        if (ca.getNhanVien() != null) {
            item.put("maNV", AttributeValue.builder().s(ca.getNhanVien().getMaNV()).build());
            if (ca.getNhanVien().getHoTen() != null && !ca.getNhanVien().getHoTen().isEmpty()) {
                item.put("tenNV", AttributeValue.builder().s(ca.getNhanVien().getHoTen()).build());
            }
        }
        db.putItem(PutItemRequest.builder().tableName(TBL).item(item).build());
        System.out.println("[ShiftDAO] ✅ Đã lưu ca trực: " + ca.getMaCa() +
            " | NV: " + (ca.getNhanVien() != null ? ca.getNhanVien().getMaNV() : "null") +
            " | Ngày: " + ca.getNgay());
    }

    // ─── Xóa ca trực ──────────────────────────────────────────────────────
    public void delete(String maCa) {
        db.deleteItem(DeleteItemRequest.builder()
            .tableName(TBL)
            .key(Map.of("maCa", AttributeValue.builder().s(maCa).build()))
            .build());
        System.out.println("[ShiftDAO] ✅ Đã xóa ca trực: " + maCa);
    }

    // ─── Helper: Scan theo khoảng ngày ────────────────────────────────────
    private List<CaTruc> scanByRange(LocalDate start, LocalDate end) {
        List<CaTruc> list = new ArrayList<>();
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                .tableName(TBL)
                .filterExpression("#ngay BETWEEN :start AND :end")
                .expressionAttributeNames(Map.of("#ngay", "ngay"))
                .expressionAttributeValues(Map.of(
                    ":start", AttributeValue.builder().s(start.toString()).build(),
                    ":end",   AttributeValue.builder().s(end.toString()).build()
                ))
                .build());
            for (Map<String, AttributeValue> item : res.items()) {
                list.add(mapToCaTruc(item));
            }
        } catch (Exception e) {
            System.err.println("[ShiftDAO] Lỗi scan: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ─── Helper: Map DynamoDB item → CaTruc ───────────────────────────────
    private CaTruc mapToCaTruc(Map<String, AttributeValue> item) {
        CaTruc ca = new CaTruc();
        if (item.containsKey("maCa"))       ca.setMaCa(item.get("maCa").s());
        if (item.containsKey("ngay"))       ca.setNgay(LocalDate.parse(item.get("ngay").s()));
        if (item.containsKey("gioBatDau"))  ca.setGioBatDau(LocalTime.parse(item.get("gioBatDau").s()));
        if (item.containsKey("gioKetThuc")) ca.setGioKetThuc(LocalTime.parse(item.get("gioKetThuc").s()));
        if (item.containsKey("maNV")) {
            NhanVien nv = new NhanVien();
            nv.setMaNV(item.get("maNV").s());
            if (item.containsKey("tenNV")) nv.setHoTen(item.get("tenNV").s());
            ca.setNhanVien(nv);
        }
        return ca;
    }

    // ─── Helper: Sinh mã ca mới không trùng ───────────────────────────────
    private String generateNewId() {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                .tableName(TBL)
                .projectionExpression("maCa")
                .build());
            int maxNum = 0;
            for (Map<String, AttributeValue> item : res.items()) {
                String ma = item.get("maCa").s();
                if (ma != null && ma.startsWith("CA")) {
                    try {
                        int num = Integer.parseInt(ma.substring(2));
                        if (num > maxNum) maxNum = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
            return String.format("CA%03d", maxNum + 1);
        } catch (Exception e) {
            return "CA" + System.currentTimeMillis();
        }
    }
}