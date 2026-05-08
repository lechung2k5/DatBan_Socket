package service;

import dao.InvoiceDAO;
import entity.HoaDon;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * NotificationBackgroundService - Chạy ngầm để kiểm tra các tác vụ thông báo tự động
 */
public class NotificationBackgroundService {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final InvoiceDAO invoiceDAO = new InvoiceDAO();

    public static void start() {
        // Chạy mỗi 5 phút
        scheduler.scheduleAtFixedRate(NotificationBackgroundService::checkPendingDeposits, 1, 5, TimeUnit.MINUTES);
        System.out.println("[BackgroundService] Đã khởi chạy trình kiểm tra thông báo.");
    }

    public static void stop() {
        scheduler.shutdown();
    }

    private static void checkPendingDeposits() {
        try {
            // Tìm các hóa đơn đang chờ thanh toán cọc
            List<HoaDon> pendingInvoices = invoiceDAO.findPending();
            for (HoaDon hd : pendingInvoices) {
                if ("ChoThanhToanCoc".equals(hd.getTrangThai())) {
                    // Nếu quá 15 phút mà chưa cọc (giả định)
                    // Ở đây tôi gửi thông báo nhắc nhở
                    if (hd.getKhachHang() != null && hd.getKhachHang().getSoDT() != null) {
                        NotificationService.sendNotification(
                            hd.getKhachHang().getSoDT(),
                            "Nhắc nhở đặt bàn",
                            "Đơn đặt bàn " + hd.getMaHD() + " của bạn vẫn đang chờ thanh toán cọc. Vui lòng hoàn tất để giữ bàn.",
                            "BOOKING"
                        );
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[BackgroundService] Lỗi kiểm tra cọc: " + e.getMessage());
        }
    }
}
