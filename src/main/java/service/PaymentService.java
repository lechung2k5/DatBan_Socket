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
            // 🔥 SỬA: Nếu có maHDGoc trong params hoặc trong DB thì đều là hóa đơn phụ
            entity.HoaDon currentHd = invoiceDAO.findById(maHD);
            boolean isSubInvoice = (maHDGoc != null && !maHDGoc.isEmpty()) 
                                || (currentHd != null && currentHd.getMaHDGoc() != null && !currentHd.getMaHDGoc().isEmpty());

            // 4. Thanh toán hóa đơn chính
            invoiceDAO.checkout(maHD, pttt != null ? pttt : "TIEN_MAT", maNV, totalAmount, totalFood, serviceFee, vat, discount);
            
            // 5. CHỈ giải phóng bàn nếu đây là hóa đơn GỐC (không có parentId)
            if (!isSubInvoice && maBan != null && !maBan.isEmpty()) {
                tableDAO.updateStatus(maBan, "Trong");
            }

            // 6. Nếu là hóa đơn GỐC, tự động thanh toán các hóa đơn phụ còn sót lại (nếu có)
            if (!isSubInvoice) {
                List<HoaDon> subInvoices = invoiceDAO.findByParentId(maHD);
                for (HoaDon sub : subInvoices) {
                    if (sub.getTrangThai() != entity.TrangThaiHoaDon.DA_THANH_TOAN) {
                        invoiceDAO.checkout(sub.getMaHD(), pttt != null ? pttt : "TIEN_MAT", maNV, 0.0, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }

            // 7. 🔥 Tự động lưu/cập nhật thông tin Khách hàng (Atomic Upsert + Tích điểm)
            if (currentHd != null && currentHd.getKhachHang() != null) {
                String phone = currentHd.getKhachHang().getSoDT();
                String name = currentHd.getKhachHang().getTenKH();
                if (phone != null && !phone.isEmpty()) {
                    dao.CustomerDAO customerDAO = new dao.CustomerDAO();
                    customerDAO.upsert(phone, name);

                    // 🏆 TÍCH ĐIỂM: 100k = 1 điểm, > 300k tặng +1 điểm
                    int pointsAdded = (int) (totalAmount / 100000);
                    if (totalAmount > 300000) pointsAdded += 1;
                    
                    if (pointsAdded > 0) {
                        customerDAO.addPointsAndAdjustLevel(phone, pointsAdded);
                    }

                    Service.broadcast(new RealTimeEvent(CommandType.UPDATE_CUSTOMER, "Cập nhật khách hàng", phone));
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