package dao;

import entity.*;
import db.DynamoDBConfig;
import utils.JsonUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * InvoiceDAO Â— HÃ³a Ä‘Æ¡n vá»›i embedded items (document-style NoSQL - CLO1)
 *
 * DynamoDB Table: "Invoices"
 * PK: invoiceId (String)
 *
 * Äáº·c trÆ°ng NoSQL: Danh sÃ¡ch mÃ³n Äƒn (ChiTietHoaDon) Ä‘Æ°á»£c lÆ°u
 * dÆ°á»›i dáº¡ng
 * JSON string bÃªn trong field "itemsJson" Â— thá»ƒ hiá»‡n document-style.
 */
public class InvoiceDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Invoices";

    // â”€â”€â”€ 1. Táº¡o hÃ³a Ä‘Æ¡n má»›i (insert vá»›i embedded items)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void insert(HoaDon hd, List<ChiTietHoaDon> chiTietList) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("invoiceId", av(hd.getMaHD()));
            // â”€â”€â”€ Tá»± Ä‘á»™ng cáº­p nháº­t tráº¡ng thÃ¡i bÃ n â”€â”€â”€
            if (hd.getMaBan() != null && !hd.getMaBan().isEmpty()) {
                String tableStatus = "DaDat";
                if ("DangSuDung".equals(hd.getTrangThai() != null ? hd.getTrangThai().getDbValue() : "")) {
                    tableStatus = "DangPhucVu";
                }
                new dao.TableDAO().updateStatus(hd.getMaBan(), tableStatus);
            }
            item.put("tableId", av(hd.getMaBan() != null ? hd.getMaBan() : ""));
            item.put("status", av(hd.getTrangThai() != null ? hd.getTrangThai().getDbValue() : "Dat"));
            item.put("createdAt",
                    av(hd.getNgayLap() != null ? hd.getNgayLap().toString() : LocalDateTime.now().toString()));
            if (hd.getGioVao() != null)
                item.put("gioVao", av(hd.getGioVao().toString()));
            else
                item.put("gioVao", av(LocalDateTime.now().toString()));
            item.put("employeeId", av(hd.getTenNhanVien() != null ? hd.getTenNhanVien() : ""));
            item.put("total", avn(hd.getTongTienThanhToan()));
            item.put("tienCoc", avn(hd.getTienCoc()));
            // ðŸ”‘ NoSQL embedded items Â— lÆ°u list dÆ°á»›i dáº¡ng JSON string
            if (chiTietList != null && !chiTietList.isEmpty()) {
                item.put("itemsJson", av(JsonUtil.toJson(chiTietList)));
            }
            if (hd.getKhachHang() != null) {
                item.put("customerPhone", av(hd.getKhachHang().getSoDT()));
                item.put("customerName", av(hd.getKhachHang().getTenKH()));
            }
            if (hd.getMaUuDai() != null)
                item.put("promoId", av(hd.getMaUuDai()));
            if (hd.getMaHDGoc() != null)
                item.put("parentInvoiceId", av(hd.getMaHDGoc()));
            db.putItem(PutItemRequest.builder().tableName(TBL).item(item).build());
            System.out.println("[DAO:Invoices] insert â†’ " + hd.getMaHD());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i insert: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // â”€â”€â”€ 2. TÃ¬m theo ID
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public HoaDon findById(String invoiceId) {
        try {
            GetItemResponse res = db.getItem(GetItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .build());
            if (res.hasItem() && !res.item().isEmpty())
                return mapToHoaDon(res.item());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findById: " + e.getMessage());
        }
        return null;
    }

    // â”€â”€â”€ 3. Láº¥y danh sÃ¡ch theo ngÃ y (Scan + filter Â— CLO1 StatsDAO
    // pattern) â”€
    public List<HoaDon> findByDate(LocalDate date) {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .filterExpression("begins_with(createdAt, :date)")
                    .expressionAttributeValues(Map.of(":date", av(date.toString())))
                    .build());
            return res.items().stream().map(this::mapToHoaDon).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findByDate: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<HoaDon> findByParentId(String parentId) {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .filterExpression("parentInvoiceId = :p")
                    .expressionAttributeValues(Map.of(":p", av(parentId)))
                    .build());
            return res.items().stream().map(this::mapToHoaDon).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findByParentId: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // â”€â”€â”€ 4. Láº¥y hÃ³a Ä‘Æ¡n theo tráº¡ng thÃ¡i
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public List<HoaDon> findByStatus(String status) {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .filterExpression("#s = :status")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(":status", av(status)))
                    .build());
            return res.items().stream().map(this::mapToHoaDon).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findByStatus: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<HoaDon> findPending() {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .filterExpression("#s IN (:s1, :s2, :s3, :s4)")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(
                            ":s1", av("Dat"),
                            ":s2", av("ChoXacNhan"),
                            ":s3", av("DangSuDung"),
                            ":s4", av("HoaDonTam")))
                    .build());
            return res.items().stream().map(this::mapToHoaDon).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findPending: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<HoaDon> findAll() {
        try {
            ScanResponse res = db.scan(ScanRequest.builder().tableName(TBL).build());
            return res.items().stream().map(this::mapToHoaDon).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findAll: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<String> findActiveTableIds() {
        try {
            // Láº¥y danh sÃ¡ch maBan cá»§a cÃ¡c hÃ³a Ä‘Æ¡n chÆ°a thanh toÃ¡n (HoaDonTam,
            // DangSuDung)
            ScanResponse res = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .filterExpression("#s <> :paid AND #s <> :cancel")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(
                            ":paid", av("DaThanhToan"),
                            ":cancel", av("DaHuy")))
                    .projectionExpression("tableId")
                    .build());
            return res.items().stream()
                    .map(m -> m.get("tableId").s())
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findActiveTableIds: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void updateInfo(String invoiceId, String customerPhone, double tienCoc) {
        updateInfoExtended(invoiceId, customerPhone, tienCoc, null, null, null, null);
    }

    public void updateInfoExtended(String invoiceId, String customerPhone, Double tienCoc, String tableId,
            String gioVao, String status, String itemsJson) {
        try {
            StringBuilder expr = new StringBuilder("SET ");
            Map<String, AttributeValue> vals = new HashMap<>();
            if (customerPhone != null) {
                expr.append("customerPhone = :c, ");
                vals.put(":c", av(customerPhone));
            }
            if (tienCoc != null) {
                expr.append("tienCoc = :t, ");
                vals.put(":t", avn(tienCoc));
            }
            if (tableId != null) {
                expr.append("tableId = :tb, ");
                vals.put(":tb", av(tableId));
            }
            if (gioVao != null) {
                expr.append("gioVao = :gv, ");
                vals.put(":gv", av(gioVao));
            }
            if (status != null) {
                expr.append("#status = :st, ");
                vals.put(":st", av(status));
            }
            if (itemsJson != null) {
                expr.append("itemsJson = :ij, ");
                vals.put(":ij", av(itemsJson));
            }
            // Remove trailing comma and space
            if (expr.toString().endsWith(", ")) {
                expr.setLength(expr.length() - 2);
            }
            if (vals.isEmpty())
                return;

            UpdateItemRequest.Builder builder = UpdateItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .updateExpression(expr.toString())
                    .expressionAttributeValues(vals);

            if (status != null) {
                builder.expressionAttributeNames(Map.of("#status", "status"));
            }

            db.updateItem(builder.build());
            System.out.println("[DAO:Invoices] updateInfoExtended(" + invoiceId + ") âœ“");
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i updateInfoExtended: " + e.getMessage());
        }
    }

    // â”€â”€â”€ 5. Cáº­p nháº­t tráº¡ng thÃ¡i
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void updateStatus(String invoiceId, String newStatus, boolean setGioRa) {
        try {
            String expr = "SET #s = :s";
            Map<String, AttributeValue> vals = new HashMap<>();
            vals.put(":s", av(newStatus));
            if (setGioRa) {
                expr += ", finishedAt = :f";
                vals.put(":f", av(LocalDateTime.now().toString()));
            }
            db.updateItem(UpdateItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .updateExpression(expr)
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(vals)
                    .build());
            System.out.println("[DAO:Invoices] updateStatus(" + invoiceId + " â†’ " + newStatus + ") âœ“");
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i updateStatus: " + e.getMessage());
        }
    }

    // â”€â”€â”€ Cáº­p nháº­t promo
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void updatePromo(String invoiceId, String promoId, double discountValue) {
        try {
            db.updateItem(UpdateItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .updateExpression("SET promoId = :p, promoValue = :v, khuyenMai = :k")
                    .expressionAttributeValues(Map.of(
                            ":p", av(promoId),
                            ":v", avn(discountValue),
                            ":k", avn(discountValue)))
                    .build());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i updatePromo: " + e.getMessage());
        }
    }

    // â”€â”€â”€ Checkout
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void checkout(String invoiceId, String paymentMethod, String employeeId, double totalAmount) {
        try {
            db.updateItem(UpdateItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .updateExpression(
                            "SET #s = :paid, paymentMethod = :pm, employeeId = :emp, #t = :total, checkoutAt = :now")
                    .expressionAttributeNames(Map.of("#s", "status", "#t", "total"))
                    .expressionAttributeValues(Map.of(
                            ":paid", av("DaThanhToan"),
                            ":pm", av(paymentMethod),
                            ":emp", av(employeeId),
                            ":total", avn(totalAmount),
                            ":now", av(java.time.LocalDateTime.now().toString())))
                    .build());
            System.out.println("[DAO:Invoices] checkout(" + invoiceId + ") âœ“");
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i checkout: " + e.getMessage());
        }
    }

    // â”€â”€â”€ 6. Láº¥y chi tiáº¿t mÃ³n Äƒn (tá»« embedded JSON)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public List<ChiTietHoaDon> getChiTietHoaDon(String invoiceId) {
        try {
            GetItemResponse res = db.getItem(GetItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .projectionExpression("itemsJson")
                    .build());
            if (res.hasItem() && res.item().containsKey("itemsJson")) {
                String json = res.item().get("itemsJson").s();
                return JsonUtil.fromJsonList(json, ChiTietHoaDon.class);
            }
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i getChiTietHoaDon: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // â”€â”€â”€ 7. Láº¥y danh sÃ¡ch hÃ³a Ä‘Æ¡n theo khÃ¡ch hÃ ng
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public List<HoaDon> findByCustomer(String customerPhone) {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .filterExpression("customerPhone = :p")
                    .expressionAttributeValues(Map.of(":p", av(customerPhone)))
                    .build());
            return res.items().stream().map(this::mapToHoaDon).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findByCustomer: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // â”€â”€â”€ 8. Gá»™p bÃ n (Cá»™ng dá»“n itemsJson)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void mergeItems(String targetInvoiceId, String sourceInvoiceId) {
        try {
            List<ChiTietHoaDon> targetItems = getChiTietHoaDon(targetInvoiceId);
            List<ChiTietHoaDon> sourceItems = getChiTietHoaDon(sourceInvoiceId);
            // Gá»™p list (simplified logic: just add all, can group by monAn if needed)
            Map<String, ChiTietHoaDon> map = new HashMap<>();
            for (ChiTietHoaDon item : targetItems)
                map.put(item.getTenMon(), item);
            for (ChiTietHoaDon item : sourceItems) {
                if (map.containsKey(item.getTenMon())) {
                    ChiTietHoaDon ex = map.get(item.getTenMon());
                    ex.setSoLuong(ex.getSoLuong() + item.getSoLuong());
                    ex.setThanhTien(ex.getSoLuong() * ex.getDonGia());
                } else
                    map.put(item.getTenMon(), item);
            }
            targetItems = new ArrayList<>(map.values());
            db.updateItem(UpdateItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(targetInvoiceId)))
                    .updateExpression("SET itemsJson = :j")
                    .expressionAttributeValues(Map.of(":j", av(JsonUtil.toJson(targetItems))))
                    .build());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i mergeItems: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void splitItems(String sourceInvoiceId, String targetInvoiceId, List<ChiTietHoaDon> itemsToMove) {
        try {
            List<ChiTietHoaDon> sourceItems = getChiTietHoaDon(sourceInvoiceId);
            for (ChiTietHoaDon moveItem : itemsToMove) {
                sourceItems.removeIf(item -> {
                    if (item.getTenMon().equals(moveItem.getTenMon())) {
                        int slConLai = item.getSoLuong() - moveItem.getSoLuong();
                        if (slConLai <= 0)
                            return true;
                        item.setSoLuong(slConLai);
                        item.setThanhTien(item.getSoLuong() * item.getDonGia());
                        return false;
                    }
                    return false;
                });
            }
            updateItems(sourceInvoiceId, sourceItems);
            updateItems(targetInvoiceId, itemsToMove);
            System.out.println("[DAO:Invoices] splitItems âœ“ " + sourceInvoiceId + " â†’ " + targetInvoiceId);
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i splitItems: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void updateItems(String invoiceId, List<ChiTietHoaDon> items) {
        db.updateItem(UpdateItemRequest.builder()
                .tableName(TBL)
                .key(Map.of("invoiceId", av(invoiceId)))
                .updateExpression("SET itemsJson = :j")
                .expressionAttributeValues(Map.of(":j", av(JsonUtil.toJson(items))))
                .build());
    }

    // â”€â”€â”€ 9. XÃ³a hÃ³a Ä‘Æ¡n Ä‘Ã£ gá»™p (vÃ  giáº£i phÃ³ng bÃ n)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void deleteMerged(String invoiceId) {
        try {
            db.deleteItem(DeleteItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .build());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i deleteMerged: " + e.getMessage());
        }
    }

    // â”€â”€â”€ Helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private AttributeValue av(String s) {
        return AttributeValue.builder().s(s != null ? s : "").build();
    }

    private AttributeValue avn(double n) {
        return AttributeValue.builder().n(String.valueOf(n)).build();
    }

    private HoaDon mapToHoaDon(Map<String, AttributeValue> m) {
        HoaDon hd = new HoaDon();
        if (m.containsKey("invoiceId"))
            hd.setMaHD(m.get("invoiceId").s());
        if (m.containsKey("tableId"))
            hd.setMaBan(m.get("tableId").s());
        if (m.containsKey("status"))
            hd.setTrangThai(m.get("status").s());
        if (m.containsKey("gioVao")) {
            try {
                hd.setGioVao(LocalDateTime.parse(m.get("gioVao").s()));
            } catch (Exception ignored) {
            }
        } else if (m.containsKey("createdAt")) {
            try {
                hd.setGioVao(LocalDateTime.parse(m.get("createdAt").s()));
            } catch (Exception ignored) {
            }
        }
        if (m.containsKey("createdAt")) {
            try {
                hd.setNgayLap(LocalDateTime.parse(m.get("createdAt").s()));
            } catch (Exception ignored) {
            }
        }
        if (m.containsKey("total"))
            hd.setTongTienThanhToan(Double.parseDouble(m.get("total").n()));
        if (m.containsKey("tienCoc"))
            hd.setTienCoc(Double.parseDouble(m.get("tienCoc").n()));
        if (m.containsKey("employeeId"))
            hd.setTenNhanVien(m.get("employeeId").s());
        if (m.containsKey("promoId"))
            hd.setMaUuDai(m.get("promoId").s());
        if (m.containsKey("parentInvoiceId"))
            hd.setMaHDGoc(m.get("parentInvoiceId").s());
        if (m.containsKey("customerPhone") || m.containsKey("customerName")) {
            KhachHang kh = new KhachHang();
            if (m.containsKey("customerPhone"))
                kh.setSoDT(m.get("customerPhone").s());
            if (m.containsKey("customerName"))
                kh.setTenKH(m.get("customerName").s());
            hd.setKhachHang(kh);
        }
        return hd;
    }

    /**
     * 🔥 HÀM MỚI: Sinh mã hóa đơn theo quy tắc HD + ddMMyy + xxxx
     */
    public synchronized String generateNextId() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String datePart = today.format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
        List<HoaDon> todayInvoices = findByDate(today);
        int nextSeq = todayInvoices.size() + 1;
        return String.format("HD%s%04d", datePart, nextSeq);
    }
}
