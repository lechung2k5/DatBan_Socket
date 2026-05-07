package service;
import utils.CacheService;
import dao.InvoiceDAO;
import dao.TableDAO;
import network.Request;
import network.Response;
import network.Service;
import network.RealTimeEvent;
import network.CommandType;
import utils.JsonUtil;
import java.util.List;
import entity.HoaDon;

/**
 * PaymentService - Quản lý nghiệp vụ thanh toán
 */
public class PaymentService {
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final TableDAO tableDAO = new TableDAO();

    public Response handleCheckout(Request request) {
        try {
            String maHD = (String) request.getParam("maHD");
            String pttt = (String) request.getParam("pttt");
            String maNV = (String) request.getParam("maNhanVien");
            String maBan = (String) request.getParam("maBan");
            String maUuDai = (String) request.getParam("maUuDai");
            Double totalAmount = Double.valueOf(String.valueOf(request.getParam("totalAmount")));
            Double totalFood = request.getParam("totalFood") != null ? Double.valueOf(String.valueOf(request.getParam("totalFood"))) : 0.0;
            Double serviceFee = request.getParam("serviceFee") != null ? Double.valueOf(String.valueOf(request.getParam("serviceFee"))) : 0.0;
            Double vat = request.getParam("vat") != null ? Double.valueOf(String.valueOf(request.getParam("vat"))) : 0.0;
            Double discount = request.getParam("discount") != null ? Double.valueOf(String.valueOf(request.getParam("discount"))) : 0.0;

            // 1. Xử lý tách bàn nếu có thông tin (Split-and-Pay)
            String maHDGoc = (String) request.getParam("maHDGoc");
            if (maHDGoc != null && !maHDGoc.isEmpty()) {
                List<entity.ChiTietHoaDon> monTach = JsonUtil.fromJsonList(
                        JsonUtil.toJson(request.getParam("monTach")),
                        entity.ChiTietHoaDon.class
                );
                if (monTach != null && !monTach.isEmpty()) {
                    invoiceDAO.splitItems(maHDGoc, maHD, monTach);
                }
            }

            // 2. Cập nhật mã ưu đãi (nếu có)
            if (maUuDai != null && !maUuDai.isEmpty()) {
                invoiceDAO.updatePromo(maHD, maUuDai, 0.0);
            }

            // 3. Tìm các hóa đơn phụ liên quan (nếu có) để thanh toán cùng lúc
            List<HoaDon> subInvoices = invoiceDAO.findByParentId(maHD);
            
            // 4. Thanh toán hóa đơn chính
            invoiceDAO.checkout(maHD, pttt != null ? pttt : "TIEN_MAT", maNV, totalAmount, totalFood, serviceFee, vat, discount);
            if (maBan != null && !maBan.isEmpty()) {
                tableDAO.updateStatus(maBan, "Trong");
            }

            // 5. Thanh toán tất cả hóa đơn phụ và giải phóng bàn của chúng
            for (HoaDon sub : subInvoices) {
                invoiceDAO.checkout(sub.getMaHD(), pttt != null ? pttt : "TIEN_MAT", maNV, 0.0, 0.0, 0.0, 0.0, 0.0); 
                if (sub.getMaBan() != null && !sub.getMaBan().isEmpty()) {
                    tableDAO.updateStatus(sub.getMaBan(), "Trong");
                }
            }

            
            
            CacheService.invalidateTables();
            
            // 🔥 Broadcast sự kiện cập nhật bàn và hóa đơn
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "[TABLE]:" + (maBan != null ? maBan : "ALL")));
            Service.broadcast(new RealTimeEvent(CommandType.CHECK_OUT, "[INVOICE]:" + maHD));
            
            System.out.println("[PaymentService] Thanh toán thành công: " + maHD);
            return Response.ok("Thanh toán thành công");
        } catch (Exception e) {
            System.err.println("[PaymentService] Lỗi handleCheckout: " + e.getMessage());
            return Response.error("Lỗi khi thanh toán: " + e.getMessage());
        }
    }

    public Response handleConfirmDeposit(Request request) {
        try {
            String maHD = (String) request.getParam("maHD");
            if (maHD == null || maHD.isEmpty()) return Response.error("Thiếu mã hóa đơn");
            
            invoiceDAO.updateStatus(maHD, "Dat", false);
            
            // 🔥 Broadcast sự kiện xác nhận cọc (coi như cập nhật hóa đơn)
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_INVOICE, "[INVOICE]:" + maHD));

            System.out.println("[PaymentService] Xác nhận cọc thành công: " + maHD);
            return Response.ok("Xác nhận tiền cọc thành công");
        } catch (Exception e) {
            System.err.println("[PaymentService] Lỗi handleConfirmDeposit: " + e.getMessage());
            return Response.error("Lỗi khi xác nhận cọc: " + e.getMessage());
        }
    }
}