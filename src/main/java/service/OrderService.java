package service;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import dao.InvoiceDAO;
import utils.JsonUtil;
import network.Request;
import network.Response;
import network.Service;
import network.RealTimeEvent;
import network.CommandType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
/**
* OrderService - Quản lý nghiệp vụ đặt bàn và gọi món
*/
public class OrderService {
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();

    public Response handleGenerateId(Request request) {
        try {
            return Response.ok(invoiceDAO.generateNextId());
        } catch (Exception e) {
            return Response.error("Lỗi sinh mã: " + e.getMessage());
        }
    }

    public Response handleCreateOrder(Request request) {
        try {
            HoaDon hd = JsonUtil.convertValue(request.getParam("hoaDon"), HoaDon.class);
            List<ChiTietHoaDon> chiTiet = aggregateItems(JsonUtil.fromJsonList(
            JsonUtil.toJson(request.getParam("chiTiet")),
            ChiTietHoaDon.class
            ));
            // ─── Tự động sinh mã HD nếu chưa có ───
            if (hd.getMaHD() == null || hd.getMaHD().isEmpty()) {
                hd.setMaHD(invoiceDAO.generateNextId());
            }

            // ─── Tự động tạo/cập nhật khách hàng nếu có thông tin ───
            if (hd.getKhachHang() != null && hd.getKhachHang().getSoDT() != null) {
                new dao.CustomerDAO().insert(hd.getKhachHang()); // DAO insert đã có logic check tồn tại (hoặc nên có)
            }
            invoiceDAO.insert(hd, chiTiet);
            System.out.println("[OrderService] Đã tạo hóa đơn: " + hd.getMaHD());
            
            // 🔥 THÔNG BÁO CHO QUẢN LÝ (Real-time & Persistence)
            String customerName = (hd.getKhachHang() != null) ? hd.getKhachHang().getTenKH() : "Khách vãng lai";
            
            // Fallback: Nếu hd.getBan() null, thử lấy từ raw map (đề phòng Gson mapping issue)
            String tableId = (hd.getBan() != null) ? hd.getBan().getMaBan() : "N/A";
            if (tableId.equals("N/A")) {
                Object hoaDonParam = request.getParam("hoaDon");
                if (hoaDonParam instanceof java.util.Map) {
                    java.util.Map<String, Object> rawHoaDon = (java.util.Map<String, Object>) hoaDonParam;
                    // Trường hợp 1: maBan ở root hoaDon
                    if (rawHoaDon.containsKey("maBan")) {
                        tableId = (String) rawHoaDon.get("maBan");
                    } 
                    // Trường hợp 2: maBan lồng trong object ban
                    else if (rawHoaDon.get("ban") instanceof java.util.Map) {
                        java.util.Map<String, Object> rawBan = (java.util.Map<String, Object>) rawHoaDon.get("ban");
                        if (rawBan.containsKey("maBan")) {
                            tableId = (String) rawBan.get("maBan");
                        }
                    }
                    
                    if (!tableId.equals("N/A")) {
                        hd.setMaBan(tableId); // Cập nhật ngược lại object hd
                    }
                }
            }

            NotificationService.sendNotification(
                "MANAGER", 
                "Yêu cầu đặt bàn mới", 
                "Khách hàng " + customerName + " vừa đặt bàn " + tableId + " (Đơn: " + hd.getMaHD() + ")", 
                "BOOKING"
            );

            // 🔥 THÔNG BÁO CHO KHÁCH HÀNG
            if (hd.getKhachHang() != null && hd.getKhachHang().getSoDT() != null) {
                NotificationService.sendNotification(
                    hd.getKhachHang().getSoDT(),
                    "Đặt bàn đang chờ xác nhận",
                    "Đơn đặt bàn tại bàn " + tableId + " (" + hd.getMaHD() + ") đang chờ xác nhận. Vui lòng thanh toán cọc để hoàn tất.",
                    "BOOKING"
                );
            }

            // 🔥 Broadcast sự kiện tạo đơn mới (Legacy broadcast)
            Service.broadcast(new RealTimeEvent(CommandType.CREATE_ORDER, "[ORDER]:" + hd.getMaHD()));
            if (hd.getMaBan() != null) {
                utils.CacheService.invalidateTables();
                Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "[TABLE]:" + hd.getMaBan()));
            }

            return Response.ok(hd.getMaHD());
        } catch (Exception e) {
        System.err.println("[OrderService] Lỗi handleCreateOrder: " + e.getMessage());
        return Response.error("Lỗi khi tạo đơn hàng: " + e.getMessage());
    }
}
public Response handleGetInvoice(Request request) {
    try {
        String maHD = (String) request.getParam("maHD");
        HoaDon hd = invoiceDAO.findById(maHD);
        if (hd != null) return Response.ok(hd);
        return Response.error("Không tìm thấy hóa đơn: " + maHD);
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleGetInvoicesToday(Request request) {
    try {
        Object dateParam = request.getParam("date");
        LocalDate date = (dateParam instanceof String) ? LocalDate.parse((String) dateParam) : LocalDate.now();
        List<HoaDon> list = invoiceDAO.findByDate(date);
        return Response.ok(list);
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleGetInvoicesPending(Request request) {
    try {
        List<HoaDon> list = invoiceDAO.findPending();
        return Response.ok(list);
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleGetActiveInvoices(Request request) {
    try {
        String type = (String) request.getParam("type");
        if ("ACTIVE_TABLE_IDS".equals(type)) {
            return Response.ok(invoiceDAO.findActiveTableIds());
        }
        String status = (String) request.getParam("status");
        return Response.ok(invoiceDAO.findByStatus(status));
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleUpdateInvoicePromo(Request request) {
    try {
        String maHD = (String) request.getParam("maHD");
        String maUuDai = (String) request.getParam("maUuDai");
        Double giaTri = Double.valueOf(String.valueOf(request.getParam("giaTri")));
        invoiceDAO.updatePromo(maHD, maUuDai, giaTri);
        
        // 🔥 Broadcast sự kiện cập nhật hóa đơn (để người dùng khác thấy khuyến mãi mới)
        Service.broadcast(new RealTimeEvent(CommandType.UPDATE_INVOICE, "Cập nhật khuyến mãi", maHD));

        return Response.ok("Cập nhật khuyến mãi thành công");
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleUpdateInvoice(Request request) {
    try {
        String maHD = (String) request.getParam("maHD");
        String soDT = (String) request.getParam("soDT");
        String tenKH = (String) request.getParam("tenKH");
        Double tienCoc = request.getParam("tienCoc") != null ? Double.valueOf(String.valueOf(request.getParam("tienCoc"))) : null;
        String maBan = (String) request.getParam("maBan");
        String gioVao = (String) request.getParam("gioVao");
        String status = (String) request.getParam("trangThai");
        String itemsJson = (String) request.getParam("itemsJson");
        String maUuDai = (String) request.getParam("maUuDai");
        Double giaTriUuDai = request.getParam("giaTriUuDai") != null ? Double.valueOf(String.valueOf(request.getParam("giaTriUuDai"))) : null;

        if (itemsJson != null) {
            List<ChiTietHoaDon> items = JsonUtil.fromJsonList(itemsJson, ChiTietHoaDon.class);
            itemsJson = JsonUtil.toJson(aggregateItems(items));
        }
        
        invoiceDAO.updateInfoExtended(maHD, soDT, tenKH, tienCoc, maBan, gioVao, status, itemsJson, maUuDai, giaTriUuDai);
        
        // 🔥 Broadcast sự kiện cập nhật hóa đơn
        Service.broadcast(new RealTimeEvent(CommandType.UPDATE_INVOICE, "Cập nhật hóa đơn", maHD));
        if (maBan != null) {
            utils.CacheService.invalidateTables();
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "Cập nhật bàn", maBan));
        }

        return Response.ok("Cập nhật thông tin hóa đơn thành công");
    } catch (Exception e) {
        return Response.error("Lỗi: " + e.getMessage());
    }
}
public Response handleCancelInvoice(Request request) {
    try {
        String maHD = (String) request.getParam("maHD");
        String trangThai = (String) request.getParam("trangThai");
        boolean setGioRa = Boolean.TRUE.equals(request.getParam("setGioRa"));
        // 1. Lấy thông tin hóa đơn để biết mã bàn
        HoaDon hd = invoiceDAO.findById(maHD);
        // 2. Cập nhật trạng thái hóa đơn
        invoiceDAO.updateStatus(maHD, trangThai, setGioRa);
        // 3. Nếu có bàn, cập nhật trạng thái bàn tương ứng với Hóa đơn
        if (hd != null && hd.getMaBan() != null && !hd.getMaBan().isEmpty()) {
            String tableStatus = "Trong"; // Mặc định là Trống (cho trường hợp Hủy)
            if (trangThai.equals("Dat")) {
                tableStatus = "DaDat";
            } else if (trangThai.equals("DangSuDung")) {
                tableStatus = "DangDung";
            }
            
            new dao.TableDAO().updateStatus(hd.getMaBan(), tableStatus);
            utils.CacheService.invalidateTables();
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "Cập nhật trạng thái bàn", hd.getMaBan()));
        }
        
        Service.broadcast(new RealTimeEvent(CommandType.UPDATE_INVOICE, "Hủy hóa đơn", maHD));
        
        return Response.ok("Hủy/Cập nhật hóa đơn thành công");
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleGetInvoicesByCustomer(Request request) {
    try {
        String maKH = (String) request.getParam("maKH");
        return Response.ok(invoiceDAO.findByCustomer(maKH));
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleMergeInvoices(Request request) {
    try {
        String targetId = (String) request.getParam("targetId");
        String sourceId = (String) request.getParam("sourceId");
        invoiceDAO.mergeItems(targetId, sourceId);
        
        // 🔥 Broadcast sự kiện gộp hóa đơn
        Service.broadcast(new RealTimeEvent(CommandType.MERGE_INVOICES, "[INVOICE]:" + targetId));
        Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "[TABLE]:ALL"));
        
        return Response.ok("Gộp hóa đơn thành công");
    } catch (Exception e) {
    return Response.error("Lỗi gộp: " + e.getMessage());
}
}
public Response handleCleanupMerged(Request request) {
    try {
        String maHD = (String) request.getParam("maHD");
        invoiceDAO.deleteMerged(maHD);
        return Response.ok("Dọn dẹp hóa đơn gộp thành công");
    } catch (Exception e) {
    return Response.error("Lỗi dọn dẹp: " + e.getMessage());
}
}
public Response handleGetInvoiceDetails(Request request) {
    try {
        String maHD = (String) request.getParam("maHD");
        return Response.ok(invoiceDAO.getChiTietHoaDon(maHD));
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleSplitInvoice(Request request) {
    try {
        String sourceId = (String) request.getParam("sourceId");
        String targetId = (String) request.getParam("targetId");
        List<ChiTietHoaDon> itemsToMove = JsonUtil.fromJsonList(
        JsonUtil.toJson(request.getParam("itemsToMove")),
        ChiTietHoaDon.class
        );
        if (targetId == null || targetId.isEmpty() || targetId.startsWith("TMP")) {
            HoaDon sourceHd = invoiceDAO.findById(sourceId);
            HoaDon newHd = new HoaDon();
            newHd.setMaHD(invoiceDAO.generateNextId());
            newHd.setNgayLap(LocalDateTime.now());
            newHd.setGioVao(sourceHd.getGioVao());
            newHd.setKhachHang(sourceHd.getKhachHang());
            newHd.setTrangThai("HoaDonTam");
            newHd.setTenNhanVien(sourceHd.getTenNhanVien());
            newHd.setMaHDGoc(sourceId);
            newHd.setTienCoc(0.0); // 🔥 ÉP TIỀN CỌC VỀ 0 CHO HÓA ĐƠN TÁCH
            invoiceDAO.insert(newHd, new ArrayList<>());
            targetId = newHd.getMaHD();
        }
        invoiceDAO.splitItems(sourceId, targetId, itemsToMove);
        
        // 🔥 Broadcast sự kiện tách hóa đơn
        Service.broadcast(new RealTimeEvent(CommandType.SPLIT_INVOICE, "Tách hóa đơn", sourceId));
        Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "Cập nhật bàn", "ALL"));

        return Response.ok(targetId);
    } catch (Exception e) {
    return Response.error("Lỗi tách: " + e.getMessage());
}
}
public Response handleGetInvoicesAll(Request request) {
    try {
        return Response.ok(invoiceDAO.findAll());
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
    public Response handleGetSubInvoices(Request request) {
        try {
            String maHDGoc = (String) request.getParam("maHDGoc");
            if (maHDGoc == null || maHDGoc.isEmpty()) return Response.error("Thiếu mã hóa đơn gốc");
            return Response.ok(invoiceDAO.findByParentId(maHDGoc));
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    private List<ChiTietHoaDon> aggregateItems(List<ChiTietHoaDon> items) {
        if (items == null) return new ArrayList<>();
        java.util.Map<String, ChiTietHoaDon> map = new java.util.LinkedHashMap<>();
        for (ChiTietHoaDon it : items) {
            String key = it.getMaMon() != null ? it.getMaMon() : it.getTenMon();
            if (map.containsKey(key)) {
                ChiTietHoaDon existing = map.get(key);
                existing.setSoLuong(existing.getSoLuong() + it.getSoLuong());
                existing.setThanhTien(existing.getSoLuong() * existing.getDonGia());
            } else {
                map.put(key, it);
            }
        }
        return new ArrayList<>(map.values());
    }
}