package dao;
import db.DynamoDBConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
/**
* StatsDAO  Thống kê doanh thu từ DynamoDB (Scan + filter expression - CLO1)
*
* DynamoDB Table: "Invoices"
* Thống kê dựa trên filter trên trường createdAt (ISO date prefix)
*/
public class StatsDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Invoices";
    private static final String STATUS_PAID = "DaThanhToan";
    // ─── 1. Doanh thu theo ngày ───────────────────────────────────────────────
    public double getRevenueByDate(LocalDate date) {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
            .tableName(TBL)
            .filterExpression("begins_with(createdAt, :date) AND #s = :paid")
            .expressionAttributeNames(Map.of("#s", "status"))
            .expressionAttributeValues(Map.of(
            ":date", av(date.toString()),       // "2025-05-06"
            ":paid", av(STATUS_PAID)
            ))
            .build());
            double total = res.items().stream()
            .mapToDouble(item -> {
                try { return Double.parseDouble(item.get("total").n()); }
                catch (Exception e) { return 0; }
            }).sum();
            System.out.println("[DAO:Stats] Doanh thu " + date + " → " + total);
            return total;
        } catch (Exception e) {
        System.err.println("[DAO:Stats] Lỗi getRevenueByDate: " + e.getMessage());
        return 0;
    }
}
// ─── 2. Doanh thu theo tháng ─────────────────────────────────────────────
public double getRevenueByMonth(int year, int month) {
    String prefix = String.format("%d-%02d", year, month); // "2025-05"
    try {
        ScanResponse res = db.scan(ScanRequest.builder()
        .tableName(TBL)
        .filterExpression("begins_with(createdAt, :month) AND #s = :paid")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
        ":month", av(prefix),
        ":paid",  av(STATUS_PAID)
        ))
        .build());
        double total = res.items().stream()
        .mapToDouble(item -> {
            try { return Double.parseDouble(item.get("total").n()); }
            catch (Exception e) { return 0; }
        }).sum();
        System.out.println("[DAO:Stats] Doanh thu tháng " + prefix + " → " + total);
        return total;
    } catch (Exception e) {
    System.err.println("[DAO:Stats] Lỗi getRevenueByMonth: " + e.getMessage());
    return 0;
}
}
// ─── 3. Số hóa đơn theo ngày ─────────────────────────────────────────────
public int countInvoicesByDate(LocalDate date) {
    try {
        ScanResponse res = db.scan(ScanRequest.builder()
        .tableName(TBL)
        .filterExpression("begins_with(createdAt, :date) AND #s = :paid")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
        ":date", av(date.toString()),
        ":paid", av(STATUS_PAID)
        ))
        .build());
        return res.count();
    } catch (Exception e) {
    System.err.println("[DAO:Stats] Lỗi countInvoicesByDate: " + e.getMessage());
    return 0;
}
}
// ─── 4. Top món bán chạy nhất ────────────────────────────────────────────
// (Dùng để demo thống kê phức tạp hơn - CLO1 bonus)
public List<Map<String, AttributeValue>> getRawInvoicesByDate(LocalDate date) {
    try {
        return db.scan(ScanRequest.builder()
        .tableName(TBL)
        .filterExpression("begins_with(createdAt, :date) AND #s = :paid")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
        ":date", av(date.toString()),
        ":paid", av(STATUS_PAID)
        ))
        .build()).items();
    } catch (Exception e) {
    System.err.println("[DAO:Stats] Lỗi getRawInvoicesByDate: " + e.getMessage());
    return new ArrayList<>();
}
}
public double getEmployeeRevenue(String maNV, LocalDate start, LocalDate end) {
    try {
        // Note: In real app, we should use GSI on staffId + createdAt
        ScanResponse res = db.scan(ScanRequest.builder()
        .tableName(TBL)
        .filterExpression("staffId = :nv AND #s = :paid AND createdAt BETWEEN :s AND :e")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
        ":nv", av(maNV),
        ":paid", av(STATUS_PAID),
        ":s", av(start.toString()),
        ":e", av(end.plusDays(1).toString())
        ))
        .build());
        return res.items().stream()
        .mapToDouble(item -> Double.parseDouble(item.get("total").n()))
        .sum();
    } catch (Exception e) {
    return 0;
}
}
public double getCashRevenueByEmployee(String maNV, LocalDate date) {
    try {
        ScanResponse res = db.scan(ScanRequest.builder()
        .tableName(TBL)
        .filterExpression("staffId = :nv AND #s = :paid AND begins_with(createdAt, :d) AND pttt = :cash")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
        ":nv", av(maNV),
        ":paid", av(STATUS_PAID),
        ":d", av(date.toString()),
        ":cash", av("TienMat")
        ))
        .build());
        return res.items().stream()
        .mapToDouble(item -> Double.parseDouble(item.get("total").n()))
        .sum();
    } catch (Exception e) {
    return 0;
}
}
public List<Map<String, AttributeValue>> getRawInvoicesByRange(LocalDate start, LocalDate end) {
    try {
        return db.scan(ScanRequest.builder()
        .tableName(TBL)
        .filterExpression("createdAt BETWEEN :s AND :e AND #s = :paid")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
        ":s", av(start.toString()),
        ":e", av(end.plusDays(1).toString()),
        ":paid", av(STATUS_PAID)
        ))
        .build()).items();
    } catch (Exception e) {
    return new ArrayList<>();
}
}
public List<Map<String, AttributeValue>> getRawInvoicesByMonth(int year, int month) {
    String prefix = String.format("%d-%02d", year, month);
    try {
        return db.scan(ScanRequest.builder()
        .tableName(TBL)
        .filterExpression("begins_with(createdAt, :prefix) AND #s = :paid")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
        ":prefix", av(prefix),
        ":paid", av(STATUS_PAID)
        ))
        .build()).items();
    } catch (Exception e) {
    return new ArrayList<>();
}
}
// ─── Helpers ─────────────────────────────────────────────────────────────
private AttributeValue av(String s) { return AttributeValue.builder().s(s != null ? s : "").build(); }
}