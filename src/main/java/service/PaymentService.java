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

            System.out.println("[PaymentService] Received Checkout Request:");
            System.out.println(" - maHD: " + maHD);
            System.out.println(" - totalAmount: " + totalAmount);
            System.out.println(" - totalFood: " + totalFood);
            System.out.println(" - serviceFee: " + serviceFee);
            System.out.println(" - vat: " + vat);
            System.out.println(" - discount: " + discount);

            // 1. Xử lý tách bàn nếu có thông tin (Split-and-Pay)
            String maHDGoc = (String) request.getParam("maHDGoc");
            if (maHDGoc != null && !maHDGoc.isEmpty()) {
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
                    maHD = realId;
                }

                List<entity.ChiTietHoaDon> monTach = JsonUtil.fromJsonList(
                        JsonUtil.toJson(request.getParam("monTach")),
                        entity.ChiTietHoaDon.class
                );
                if (monTach != null && !monTach.isEmpty()) {
                    invoiceDAO.splitItems(maHDGoc, maHD, monTach);
                }
            }

            // 2. 🔥 TÍNH TOÁN CHIẾT KHẤU & TỔNG TIỀN CUỐI CÙNG
            entity.HoaDon currentHd = invoiceDAO.findById(maHD);
            boolean isSubInvoice = (maHDGoc != null && !maHDGoc.isEmpty()) 
                                || (currentHd != null && currentHd.getMaHDGoc() != null && !currentHd.getMaHDGoc().isEmpty());

            double serverDiscount = 0.0;
            if (currentHd != null && currentHd.getKhachHang() != null && discount == 0) {
                String phone = currentHd.getKhachHang().getSoDT();
                dao.CustomerDAO customerDAO = new dao.CustomerDAO();
                entity.KhachHang kh = customerDAO.findByPhone(phone);
                if (kh != null) {
                    if ("Diamond".equals(kh.getThanhVien())) {
                        serverDiscount = totalAmount * 0.15;
                    } else if ("Gold".equals(kh.getThanhVien()) && totalAmount > 200000) {
                        serverDiscount = totalAmount * 0.10;
                    }
                }
            }

            double finalAmount = (serverDiscount > 0) ? (totalAmount - serverDiscount) : totalAmount;
            double finalDiscount = (serverDiscount > 0) ? serverDiscount : discount;
            if (finalAmount < 0) finalAmount = 0;

            // 3. Cập nhật tích điểm và hạng thành viên
            if (currentHd != null && currentHd.getKhachHang() != null) {
                String phone = currentHd.getKhachHang().getSoDT();
                if (phone != null && !phone.isEmpty()) {
                    dao.CustomerDAO customerDAO = new dao.CustomerDAO();
                    entity.KhachHang kh = customerDAO.findByPhone(phone);
                    if (kh != null) {
                        // 🔥 TÍNH ĐIỂM: 1 điểm cho mỗi 10,000 VNĐ
                        int pointsAdded = (int) (finalAmount / 10000);
                        if (pointsAdded <= 0 && finalAmount > 0) pointsAdded = 1; // Tối thiểu 1 điểm nếu có thanh toán
                        
                        System.out.println("[PaymentService] Đang tích điểm cho " + phone + ": +" + pointsAdded + " điểm (HĐ: " + maHD + ")");
                        
                        customerDAO.addPointsAndAdjustLevel(phone, pointsAdded);
                        
                        entity.KhachHang updatedKh = customerDAO.findByPhone(phone);
                        if (updatedKh != null && updatedKh.getEmail() != null && !updatedKh.getEmail().isEmpty()) {
                            try {
                                String emailContent = "<h3>Cảm ơn quý khách đã dùng bữa tại Nhà hàng Tứ Hữu!</h3>"
                                    + "<p>Mã hóa đơn: <b>" + maHD + "</b></p>"
                                    + "<p>Số tiền thanh toán: <b>" + String.format("%,.0f", finalAmount) + " VNĐ</b></p>"
                                    + "<p>Số điểm cộng thêm: <b style='color: green;'>+" + pointsAdded + "</b></p>"
                                    + "<p>Tổng điểm hiện tại: <b>" + updatedKh.getDiemTichLuy() + "</b></p>"
                                    + "<p>Hạng thành viên: <b style='color: gold;'>" + updatedKh.getThanhVien() + "</b></p>";
                                utils.EmailUtil.sendEmail(updatedKh.getEmail(), "Xác nhận thanh toán & Tích điểm", emailContent);
                            } catch (Exception e) {
                                System.err.println("[PaymentService] Lỗi gửi mail tích điểm: " + e.getMessage());
                            }
                        }
                        NotificationService.sendNotification(phone, "Tích điểm thành công", 
                            "Hóa đơn " + maHD + " mang về cho bạn +" + pointsAdded + " điểm. Tổng điểm: " + updatedKh.getDiemTichLuy(), "SYSTEM");
                        Service.broadcast(new RealTimeEvent(CommandType.UPDATE_CUSTOMER, "Cập nhật khách hàng", phone));
                    } else {
                        System.out.println("[PaymentService] Không tìm thấy thông tin KH trong DB để tích điểm: " + phone);
                    }
                }
            } else {
                System.out.println("[PaymentService] Hóa đơn không có thông tin khách hàng, bỏ qua tích điểm.");
            }

            // 4. 🔥 THANH TOÁN
            invoiceDAO.checkout(maHD, pttt != null ? pttt : "TIEN_MAT", maNV, finalAmount, totalFood, serviceFee, vat, finalDiscount);
            if (maUuDai != null && !maUuDai.isEmpty()) {
                invoiceDAO.updatePromo(maHD, maUuDai, finalDiscount);
            }

            if (!isSubInvoice && maBan != null && !maBan.isEmpty()) {
                tableDAO.updateStatus(maBan, "Trong");
            }

            if (!isSubInvoice) {
                List<entity.HoaDon> subInvoices = invoiceDAO.findByParentId(maHD);
                for (entity.HoaDon sub : subInvoices) {
                    if (sub.getTrangThai() != entity.TrangThaiHoaDon.DA_THANH_TOAN) {
                        invoiceDAO.checkout(sub.getMaHD(), pttt != null ? pttt : "TIEN_MAT", maNV, 0.0, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }

            CacheService.invalidateTables();
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "Cập nhật bàn", maBan != null ? maBan : "ALL"));
            Service.broadcast(new RealTimeEvent(CommandType.CHECK_OUT, "Hóa đơn đã thanh toán", maHD));
            return Response.ok("Thanh toán thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    public Response handleConfirmDeposit(Request request) {
        try {
            String maHD = (String) request.getParam("maHD");
            if (maHD == null || maHD.isEmpty()) return Response.error("Thiếu mã hóa đơn");
            invoiceDAO.updateStatus(maHD, "Dat", false);
            HoaDon hd = invoiceDAO.findById(maHD);
            if (hd != null && hd.getKhachHang() != null && hd.getKhachHang().getSoDT() != null) {
                NotificationService.sendNotification(hd.getKhachHang().getSoDT(), "Đặt bàn thành công", "Bàn của quý khách đã được xác nhận.", "BOOKING");
            }
            Service.broadcast(new RealTimeEvent(CommandType.UPDATE_INVOICE, "Cập nhật hóa đơn", maHD));
            return Response.ok("Xác nhận tiền cọc thành công");
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }
}