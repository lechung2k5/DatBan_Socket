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
 * InvoiceDAO Â— Hóa Ä‘ơn vá»›i embedded items (document-style NoSQL - CLO1)
 *
 * DynamoDB Table: "Invoices"
 * PK: invoiceId (String)
 *
 * Äáº·c trưng NoSQL: Danh sách món Äƒn (ChiTietHoaDon) Ä‘ưá»£c lưu
 * dưá»›i dạng
 * JSON string bên trong field "itemsJson" Â— thá»ƒ hiá»‡n document-style.
 */
public class InvoiceDAO {
    private final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "Invoices";

    // â”€â”€â”€ 1. Tạo hóa Ä‘ơn má»›i (insert vá»›i embedded items)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void insert(HoaDon hd, List<ChiTietHoaDon> chiTietList) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("invoiceId", av(hd.getMaHD()));
            // â”€â”€â”€ Tự Ä‘á»™ng cáº­p nháº­t trạng thái bÃ n â”€â”€â”€
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
            // ðŸ”‘ NoSQL embedded items Â— lưu list dưá»›i dạng JSON string
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

    // â”€â”€â”€ 2. Tìm theo ID
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void delete(String invoiceId) {
        try {
            db.deleteItem(DeleteItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .build());
            System.out.println("[DAO:Invoices] delete -> " + invoiceId);
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lỗi delete: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public HoaDon findById(String invoiceId) {
        try {
            GetItemResponse res = db.getItem(GetItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .consistentRead(true)
                    .build());
            if (res.hasItem() && !res.item().isEmpty())
                return mapToHoaDon(res.item());
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i findById: " + e.getMessage());
        }
        return null;
    }

    // â”€â”€â”€ 3. Láº¥y danh sách theo ngÃ y (Scan + filter Â— CLO1 StatsDAO
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

    // â”€â”€â”€ 4. Láº¥y hóa Ä‘ơn theo trạng thái
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public List<HoaDon> findByStatus(String status) {
        try {
            ScanResponse res = db.scan(ScanRequest.builder()
                    .tableName(TBL)
                    .filterExpression("#s = :status")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(":status", av(status)))
                    .consistentRead(true)
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
                    .consistentRead(true)
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
            // Láº¥y danh sách maBan của các hóa Ä‘ơn chưa thanh toán (HoaDonTam,
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
        updateInfoExtended(invoiceId, customerPhone, null, tienCoc, null, null, null, null, null, null);
    }

    public void updateInfoExtended(String invoiceId, String customerPhone, String customerName, Double tienCoc, String tableId,
            String gioVao, String status, String itemsJson, String maUuDai, Double giaTriUuDai) {
        try {
            StringBuilder expr = new StringBuilder("SET ");
            Map<String, AttributeValue> vals = new HashMap<>();
            if (customerPhone != null) {
                expr.append("customerPhone = :c, ");
                vals.put(":c", av(customerPhone));
            }
            if (customerName != null) {
                expr.append("customerName = :cn, ");
                vals.put(":cn", av(customerName));
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
            if (maUuDai != null) {
                expr.append("maUuDai = :ud, ");
                vals.put(":ud", av(maUuDai));
            }
            if (giaTriUuDai != null) {
                expr.append("giaTriUuDai = :gt, ");
                vals.put(":gt", avn(giaTriUuDai));
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

    // â”€â”€â”€ 5. Cáº­p nháº­t trạng thái
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
    public void checkout(String invoiceId, String paymentMethod, String employeeId, double totalAmount, 
                         double totalFood, double serviceFee, double vat, double discount) {
        try {
            db.updateItem(UpdateItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .updateExpression(
                            "SET #s = :paid, paymentMethod = :pm, employeeId = :emp, #t = :total, checkoutAt = :now, " +
                            "tongCongMonAn = :tf, phiDichVu = :sf, thueVAT = :vat, khuyenMai = :dis")
                    .expressionAttributeNames(Map.of("#s", "status", "#t", "total"))
                    .expressionAttributeValues(Map.of(
                            ":paid", av("DaThanhToan"),
                            ":pm", av(paymentMethod),
                            ":emp", av(employeeId),
                            ":total", avn(totalAmount),
                            ":tf", avn(totalFood),
                            ":sf", avn(serviceFee),
                            ":vat", avn(vat),
                            ":dis", avn(discount),
                            ":now", av(java.time.LocalDateTime.now().toString())))
                    .build());
            System.out.println("[DAO:Invoices] checkout(" + invoiceId + ") ✓");
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lá»—i checkout: " + e.getMessage());
        }
    }

    // â”€â”€â”€ 6. Láº¥y chi tiết món Äƒn (từ embedded JSON)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public List<ChiTietHoaDon> getChiTietHoaDon(String invoiceId) {
        try {
            GetItemResponse res = db.getItem(GetItemRequest.builder()
                    .tableName(TBL)
                    .key(Map.of("invoiceId", av(invoiceId)))
                    .projectionExpression("itemsJson")
                    .consistentRead(true)
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

    // â”€â”€â”€ 7. Láº¥y danh sách hóa Ä‘ơn theo khách hÃ ng
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
            // 1. Lấy danh sách món hiện tại của hóa đơn gốc
            List<ChiTietHoaDon> sourceItems = getChiTietHoaDon(sourceInvoiceId);
            
            // 2. Thực hiện trừ món ở hóa đơn gốc (Dùng maMon ưu tiên, fallback sang tenMon)
            for (ChiTietHoaDon moveItem : itemsToMove) {
                String moveId = moveItem.getMaMon();
                String moveName = moveItem.getTenMon() != null ? moveItem.getTenMon().trim() : "";
                
                sourceItems.removeIf(item -> {
                    boolean isMatch = false;
                    if (moveId != null && item.getMaMon() != null) {
                        isMatch = item.getMaMon().equalsIgnoreCase(moveId);
                    } else if (item.getTenMon() != null) {
                        isMatch = item.getTenMon().trim().equalsIgnoreCase(moveName);
                    }

                    if (isMatch) {
                        int slConLai = item.getSoLuong() - moveItem.getSoLuong();
                        if (slConLai <= 0) return true; // Xóa hẳn món nếu tách hết
                        
                        item.setSoLuong(slConLai);
                        item.setThanhTien(item.getSoLuong() * item.getDonGia());
                        return false;
                    }
                    return false;
                });
            }

            // 3. Tính toán lại tổng tiền món ăn cho cả 2 hóa đơn
            double sourceTotal = sourceItems.stream().mapToDouble(ChiTietHoaDon::getThanhTien).sum();
            double targetTotal = itemsToMove.stream().mapToDouble(it -> it.getSoLuong() * it.getDonGia()).sum();

            // 4. Cập nhật hóa đơn gốc (Món mới + Tổng tiền mới)
            db.updateItem(UpdateItemRequest.builder()
                .tableName(TBL)
                .key(Map.of("invoiceId", av(sourceInvoiceId)))
                .updateExpression("SET itemsJson = :j, tongCongMonAn = :t")
                .expressionAttributeValues(Map.of(
                    ":j", av(JsonUtil.toJson(sourceItems)),
                    ":t", avn(sourceTotal)
                ))
                .build());

            // 5. Cập nhật hóa đơn tách (Món tách + Tổng tiền tách)
            db.updateItem(UpdateItemRequest.builder()
                .tableName(TBL)
                .key(Map.of("invoiceId", av(targetInvoiceId)))
                .updateExpression("SET itemsJson = :j, tongCongMonAn = :t")
                .expressionAttributeValues(Map.of(
                    ":j", av(JsonUtil.toJson(itemsToMove)),
                    ":t", avn(targetTotal)
                ))
                .build());

            System.out.println("[DAO:Invoices] splitItems ✓: " + sourceInvoiceId + " (" + sourceTotal + ") -> " + targetInvoiceId + " (" + targetTotal + ")");
        } catch (Exception e) {
            System.err.println("[DAO:Invoices] Lỗi splitItems: " + e.getMessage());
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

    // â”€â”€â”€ 9. Xóa hóa Ä‘ơn Ä‘ã gá»™p (vÃ  giải phóng bÃ n)
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
        try {
            if (m.containsKey("invoiceId"))
                hd.setMaHD(m.get("invoiceId").s());
            if (m.containsKey("tableId"))
                hd.setMaBan(m.get("tableId").s());
            if (m.containsKey("status"))
                hd.setTrangThai(m.get("status").s());
            
            // Map itemsJson and calculate total if needed
            if (m.containsKey("itemsJson")) {
                String itemsJson = m.get("itemsJson").s();
                try {
                    List<ChiTietHoaDon> items = JsonUtil.fromJsonList(itemsJson, ChiTietHoaDon.class);
                    hd.setChiTietHoaDon(items);
                    
                    // Nếu tổng tiền đang là 0, tính lại từ danh sách món
                    double calculatedTotal = items.stream()
                        .mapToDouble(it -> it.getSoLuong() * it.getDonGia())
                        .sum();
                    hd.setTongCongMonAn(calculatedTotal);
                } catch (Exception ignored) {}
            }
            
            // Map Ngày lập / Giờ vào
            if (m.containsKey("gioVao")) {
                try { hd.setGioVao(LocalDateTime.parse(m.get("gioVao").s())); } catch (Exception ignored) {}
            } else if (m.containsKey("createdAt")) {
                try { hd.setGioVao(LocalDateTime.parse(m.get("createdAt").s())); } catch (Exception ignored) {}
            }
            if (m.containsKey("createdAt")) {
                try { hd.setNgayLap(LocalDateTime.parse(m.get("createdAt").s())); } catch (Exception ignored) {}
            }
            if (m.containsKey("finishedAt")) {
                try { hd.setGioRa(LocalDateTime.parse(m.get("finishedAt").s())); } catch (Exception ignored) {}
            } else if (m.containsKey("checkoutAt")) {
                try { hd.setGioRa(LocalDateTime.parse(m.get("checkoutAt").s())); } catch (Exception ignored) {}
            }

            // 🔥 Lấy các khoản phí và tổng tiền món từ DB (phải làm TRƯỚC khi tính total dự phòng)
            if (m.containsKey("tongCongMonAn") && m.get("tongCongMonAn").n() != null) {
                double tFood = Double.parseDouble(m.get("tongCongMonAn").n());
                if (tFood > 0) {
                    hd.setTongCongMonAn(tFood);
                }
            }
            if (m.containsKey("phiDichVu") && m.get("phiDichVu").n() != null) hd.setPhiDichVu(Double.parseDouble(m.get("phiDichVu").n()));
            if (m.containsKey("thueVAT") && m.get("thueVAT").n() != null) hd.setThueVAT(Double.parseDouble(m.get("thueVAT").n()));
            if (m.containsKey("khuyenMai") && m.get("khuyenMai").n() != null) hd.setKhuyenMai(Double.parseDouble(m.get("khuyenMai").n()));

            // 🔥 Map Tổng tiền
            double total = 0;
            if (m.containsKey("total") && m.get("total").n() != null) total = Double.parseDouble(m.get("total").n());
            else if (m.containsKey("totalAmount") && m.get("totalAmount").n() != null) total = Double.parseDouble(m.get("totalAmount").n());
            
            // Nếu total vẫn bằng 0, sử dụng giá trị đã tính toán từ itemsJson hoặc từ DB
            if (total <= 0 && hd.getTongCongMonAn() > 0) {
                total = hd.getTongCongMonAn();
                double phiDV = hd.getPhiDichVu();
                double thue = hd.getThueVAT() > 0 ? hd.getThueVAT() : total * 0.1;
                double km = hd.getKhuyenMai();
                total = total + phiDV + thue - km;
            }
            hd.setTongTienThanhToan(total);

            // 🔥 Map Hình thức thanh toán
            String pm = null;
            if (m.containsKey("paymentMethod")) pm = m.get("paymentMethod").s();
            else if (m.containsKey("hinhThucTT")) pm = m.get("hinhThucTT").s();
            
            if (pm != null && !pm.isEmpty()) {
                hd.setHinhThucTT(PTTThanhToan.fromDbValue(pm));
            }

            if (m.containsKey("tienCoc") && m.get("tienCoc").n() != null)
                hd.setTienCoc(Double.parseDouble(m.get("tienCoc").n()));
            if (m.containsKey("employeeId"))
                hd.setTenNhanVien(m.get("employeeId").s());
            if (m.containsKey("promoId"))
                hd.setMaUuDai(m.get("promoId").s());
            if (m.containsKey("parentInvoiceId"))
                hd.setMaHDGoc(m.get("parentInvoiceId").s());

            if (m.containsKey("customerPhone") || m.containsKey("customerName")) {
                KhachHang kh = new KhachHang();
                if (m.containsKey("customerPhone")) kh.setSoDT(m.get("customerPhone").s());
                if (m.containsKey("customerName")) kh.setTenKH(m.get("customerName").s());
                hd.setKhachHang(kh);
            }
        } catch (Exception e) {
            System.err.println("[InvoiceDAO] Lỗi mapping HoaDon: " + e.getMessage());
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
        
        int maxSeq = 0;
        for (HoaDon hd : todayInvoices) {
            String id = hd.getMaHD();
            if (id != null && id.startsWith("HD" + datePart) && id.length() >= 12) {
                try {
                    int seq = Integer.parseInt(id.substring(8));
                    if (seq > maxSeq) maxSeq = seq;
                } catch (Exception ignored) {}
            }
        }
        
        return String.format("HD%s%04d", datePart, maxSeq + 1);
    }
}
