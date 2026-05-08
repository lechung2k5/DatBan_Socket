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
                // 🔥 Nếu maHD là mã tạm (TMP), cần tạo HĐ thật trước
                if (maHD.startsWith("TMP")) {
                    HoaDon sourceHd = invoiceDAO.findById(maHDGoc);
                    HoaDon newHd = new HoaDon();
                    String realId = invoiceDAO.generateNextId();
                    newHd.setMaHD(realId);
                    newHd.setNgayLap(java.time.LocalDateTime.now());
                    newHd.setGioVao(sourceHd != null ? sourceHd.getGioVao() : java.time.LocalDateTime.now());
                    newHd.setKhachHang(sourceHd != null ? sourceHd.getKhachHang() : null);
                    newHd.setTrangThai("HoaDonTam");
                    newHd.setTenNhanVien(sourceHd != null ? sourceHd.getTenNhanVien() : "N/A");
                    newHd.setMaHDGoc(maHDGoc);
                    newHd.setTienCoc(0.0);
                    
                    invoiceDAO.insert(newHd, new java.util.ArrayList<>());
                    maHD = realId; // Đổi sang ID thật để checkout bên dưới
                }

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

            // 3. Kiểm tra loại hóa đơn (Gốc hay Phụ)
            entity.HoaDon currentHd = invoiceDAO.findById(maHD);
            boolean isSubInvoice = (maHDGoc != null && !maHDGoc.isEmpty()) 
                                || (currentHd != null && currentHd.getMaHDGoc() != null && !currentHd.getMaHDGoc().isEmpty());

            // 3.5. Xử lý Tích điểm, Hạng Thành viên và Email thông báo (Từ nhánh giaminh)
            if (currentHd != null && currentHd.getKhachHang() != null) {
                String phone = currentHd.getKhachHang().getSoDT();
                dao.CustomerDAO customerDAO = new dao.CustomerDAO();
                entity.KhachHang kh = customerDAO.findByPhone(phone);
                
                if (kh != null) {
                    // Tự động áp dụng giảm giá theo hạng nếu chưa có discount từ mobile
                    if (discount == 0) {
                        if ("Diamond".equals(kh.getThanhVien())) {
                            discount = totalAmount * 0.15;
                        } else if ("Gold".equals(kh.getThanhVien()) && totalAmount > 200000) {
                            discount = totalAmount * 0.10;
                        }
                    }
                    
                    double amountAfterDiscount = totalAmount - discount;
                    
                    // Tính điểm cộng: 100k = 1 điểm, > 300k tặng +1 điểm
                    int pointsAdded = (int) (amountAfterDiscount / 100000);
                    if (amountAfterDiscount > 300000) pointsAdded += 1;
                    
                    String oldTier = kh.getThanhVien();
                    customerDAO.addPointsAndAdjustLevel(phone, pointsAdded);
                    
                    // Lấy lại thông tin sau khi cập nhật để gửi email
                    entity.KhachHang updatedKh = customerDAO.findByPhone(phone);
                    
                    // Thông báo email (Feature từ giaminh)
                    if (updatedKh != null && updatedKh.getEmail() != null && !updatedKh.getEmail().isEmpty()) {
                        String emailContent = "<h3>Cảm ơn quý khách đã dùng bữa tại Nhà hàng Tứ Hữu!</h3>"
                            + "<p>Mã hóa đơn: <b>" + maHD + "</b></p>"
                            + "<p>Số điểm được cộng: <b>" + pointsAdded + "</b></p>"
                            + "<p>Tổng điểm tích lũy: <b>" + updatedKh.getDiemTichLuy() + "</b></p>"
                            + "<p>Hạng thành viên hiện tại: <b>" + updatedKh.getThanhVien() + "</b></p>";
                        if (!updatedKh.getThanhVien().equals(oldTier)) {
                            emailContent += "<p style='color:green;'>Chúc mừng! Bạn đã được thăng hạng lên <b>" + updatedKh.getThanhVien() + "</b></p>";
                        }
                        utils.EmailUtil.sendEmail(updatedKh.getEmail(), "Thông báo điểm tích lũy - Nhà hàng Tứ Hữu", emailContent);
                    }
                    
                    // 🔥 QUAN TRỌNG: Gửi thông báo real-time cho mobile
                    NotificationService.sendNotification(
                        phone,
                        "Cảm ơn quý khách",
                        "Hóa đơn " + maHD + " đã được thanh toán. Tổng tiền: " + String.format("%,.0f", amountAfterDiscount) + " VNĐ. Bạn được cộng " + pointsAdded + " điểm!",
                        "SYSTEM"
                    );
                    Service.broadcast(new RealTimeEvent(CommandType.UPDATE_CUSTOMER, "Cập nhật khách hàng", phone));
                    
                    totalAmount = amountAfterDiscount; // Cập nhật để lưu vào DB
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
            
            // 🔥 THÔNG BÁO CHO QUẢN LÝ
            HoaDon hd = invoiceDAO.findById(maHD);
            if (hd != null) {
                String customerName = (hd.getKhachHang() != null) ? hd.getKhachHang().getTenKH() : "Khách hàng";
                String tableId = (hd.getBan() != null) ? hd.getBan().getMaBan() : "N/A";
                NotificationService.sendNotification(
                    "MANAGER", 
                    "Khách đã thanh toán cọc", 
                    "Hóa đơn " + maHD + " (Bàn " + tableId + ") của " + customerName + " đã được xác nhận thanh toán.",
                    "BOOKING"
                );

                // 🔥 THÔNG BÁO CHO KHÁCH HÀNG (Real-time & Persistence)
                if (hd.getKhachHang() != null && hd.getKhachHang().getSoDT() != null) {
                    NotificationService.sendNotification(
                        hd.getKhachHang().getSoDT(),
                        "Đặt bàn thành công",
                        "Cảm ơn quý khách! Bàn " + tableId + " của quý khách đã được xác nhận thành công.",
                        "BOOKING"
                    );
                }
            }

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