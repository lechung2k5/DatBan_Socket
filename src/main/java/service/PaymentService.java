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
            // 3.5. Xử lý Tích điểm và Hạng Thành viên tự động
            dao.CustomerDAO customerDAO = new dao.CustomerDAO();
            HoaDon hd = invoiceDAO.findById(maHD);
            entity.KhachHang kh = null;
            if (hd != null && hd.getKhachHang() != null && hd.getKhachHang().getSoDT() != null) {
                kh = customerDAO.findByPhone(hd.getKhachHang().getSoDT());
            }
            
            if (kh != null) {
                // Tự động áp dụng giảm giá theo hạng nếu chưa có discount
                if (discount == 0) {
                    if ("Diamond".equals(kh.getThanhVien())) {
                        discount = totalAmount * 0.15; // Giảm 15% tổng bill
                    } else if ("Gold".equals(kh.getThanhVien()) && totalAmount > 200000) {
                        discount = totalAmount * 0.10; // Giảm 10% nếu > 200k
                    }
                }
                
                double amountAfterDiscount = totalAmount - discount;
                
                // Tính điểm cộng
                int diemCong = (int) (amountAfterDiscount / 100000);
                if (amountAfterDiscount > 300000) diemCong += 1;
                
                kh.setDiemTichLuy(kh.getDiemTichLuy() + diemCong);
                
                // Xét thăng hạng
                String hangCu = kh.getThanhVien();
                if (kh.getDiemTichLuy() >= 450) kh.setThanhVien("Diamond");
                else if (kh.getDiemTichLuy() >= 200) kh.setThanhVien("Gold");
                else kh.setThanhVien("Member");
                
                customerDAO.update(kh);
                
                // Thông báo email
                if (kh.getEmail() != null && !kh.getEmail().isEmpty()) {
                    String content = "<h3>Cảm ơn quý khách đã dùng bữa tại Nhà hàng Tứ Hữu!</h3>"
                        + "<p>Số điểm được cộng: <b>" + diemCong + "</b></p>"
                        + "<p>Tổng điểm tích lũy: <b>" + kh.getDiemTichLuy() + "</b></p>"
                        + "<p>Hạng thành viên hiện tại: <b>" + kh.getThanhVien() + "</b></p>";
                    if (!kh.getThanhVien().equals(hangCu)) {
                        content += "<p style='color:green;'>Chúc mừng! Bạn đã được thăng hạng lên <b>" + kh.getThanhVien() + "</b></p>";
                    }
                    utils.EmailUtil.sendEmail(kh.getEmail(), "Thông báo điểm tích lũy - Nhà hàng Tứ Hữu", content);
                }
                totalAmount = amountAfterDiscount; // Cập nhật tổng tiền để lưu vào DB
            }

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
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "Cập nhật bàn", maBan != null ? maBan : "ALL"));
            Service.broadcast(new RealTimeEvent(CommandType.CHECK_OUT, "Hóa đơn đã thanh toán", maHD));
            
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
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_INVOICE, "Cập nhật hóa đơn", maHD));

            System.out.println("[PaymentService] Xác nhận cọc thành công: " + maHD);
            return Response.ok("Xác nhận tiền cọc thành công");
        } catch (Exception e) {
            System.err.println("[PaymentService] Lỗi handleConfirmDeposit: " + e.getMessage());
            return Response.error("Lỗi khi xác nhận cọc: " + e.getMessage());
        }
    }
}