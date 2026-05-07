package entity;
import com.google.gson.annotations.SerializedName;
// Enum VaiTro khớp 100% với CSDL
public enum VaiTro {
    @SerializedName("Admin")
    ADMIN("Admin"),
    @SerializedName("QuanLy")
    QUAN_LY("QuanLy"),
    @SerializedName("ThuNgan")
    NHAN_VIEN_THU_NGAN("ThuNgan");
    private final String tenVaiTro; // Sẽ lưu "Admin", "QuanLy", "NhanVienThuNgan"
    VaiTro(String tenVaiTro) {
        this.tenVaiTro = tenVaiTro;
    }
    // Hàm này trả về tên để hiển thị trên giao diện
    public String getTenVaiTro() {
        // Chuyển đổi sang tiếng Việt có dấu để hiển thị
        switch (this) {
            case ADMIN:
            return "Admin";
            case QUAN_LY:
            return "Quản lý";
            case NHAN_VIEN_THU_NGAN:
            return "Nhân viên thu ngân";
            default:
            return tenVaiTro;
        }
    }
    // Hàm này trả về giá trị trong database (không dấu)
    public String getValueInDB() {
        return tenVaiTro;
    }
    @Override
    public String toString() {
        return getTenVaiTro(); // Hiển thị tiếng Việt có dấu
    }
    // Phương thức chuyển đổi từ String trong DB sang Enum
    /**
    * 🔥 HÀM ĐàSỬA: Chuyển đổi từ String sang Enum.
    * So sánh với cả giá trị DB (không dấu) và giá trị hiển thị (có dấu).
    */
    public static VaiTro fromString(String text) {
        System.out.println("DEBUG: Mapping VaiTro for input: " + text);
        if (text == null) return null;
        String trimmedText = text.trim();
        for (VaiTro vt : VaiTro.values()) {
            if (vt.name().equalsIgnoreCase(trimmedText) ||
            vt.tenVaiTro.equalsIgnoreCase(trimmedText) ||
            vt.getTenVaiTro().equalsIgnoreCase(trimmedText)) {
                return vt;
            }
        }
        System.out.println("DEBUG: Failed to map VaiTro: " + text);
        return null;
    }
    // ========================================================================
    // 🔥 PHẦN PHÂN QUYỀN - THEO YÊU CẦU CHÍNH XÁC
    // ========================================================================
    /**
    * Kiểm tra quyền truy cập Dashboard
    * ✅ ADMIN: Có quyền
    * ✅ QUẢN LÝ: Có quyền
    * ✅ THU NGÂN: Có quyền
    */
    public boolean coQuyenDashboard() {
        return true; // Tất cả đều có quyền
    }
    /**
    * Kiểm tra quyền Quản lý Đặt bàn
    * ✅ ADMIN: Toàn quyền
    * ❌ QUẢN LÝ: KHÔNG có quyền
    * ✅ THU NGÂN: TOÀN QUYỀN (Tạo/Sửa/Hủy/Đổi bàn)
    */
    public boolean coQuyenQuanLyDatBan() {
        // Cả Admin, Quản lý và Thu ngân đều có quyền này
        return this == ADMIN || this == QUAN_LY || this == NHAN_VIEN_THU_NGAN;
    }
    /**
    * Kiểm tra quyền Thống kê & Báo cáo
    * ✅ ADMIN: Toàn quyền
    * ✅ QUẢN LÝ: Toàn quyền
    * ❌ THU NGÂN: KHÔNG có quyền
    */
    public boolean coQuyenThongKe() {
        return this == ADMIN || this == QUAN_LY;
    }
    /**
    * Kiểm tra quyền Quản lý Thực đơn
    * ✅ ADMIN: Toàn quyền (Thêm/Sửa/Xóa)
    * ✅ QUẢN LÝ: Toàn quyền (Thêm/Sửa/Xóa)
    * ❌ THU NGÂN: KHÔNG có quyền (chỉ xem qua phần gọi món)
    */
    public boolean coQuyenQuanLyThucDon() {
        return this == ADMIN || this == QUAN_LY;
    }
    /**
    * Kiểm tra quyền Quản lý Hóa đơn
    * ✅ ADMIN: Toàn quyền
    * ❌ QUẢN LÝ: KHÔNG có quyền
    * ✅ THU NGÂN: TOÀN QUYỀN (Tạo/Sửa/Tách/Thanh toán)
    */
    public boolean coQuyenQuanLyHoaDon() {
        return this == ADMIN || this == NHAN_VIEN_THU_NGAN;
    }
    /**
    * Kiểm tra quyền Quản lý Nhân viên
    * ✅ ADMIN: Toàn quyền
    * ✅ QUẢN LÝ: Toàn quyền (Thêm/Sửa/Phân ca)
    * ❌ THU NGÂN: KHÔNG có quyền
    */
    public boolean coQuyenQuanLyNhanVien() {
        return this == ADMIN || this == QUAN_LY;
    }
    /**
    * Kiểm tra quyền Quản lý Khách hàng
    * ✅ ADMIN: Toàn quyền
    * ❌ QUẢN LÝ: KHÔNG có quyền
    * ✅ THU NGÂN: TOÀN QUYỀN (Thêm/Sửa/Tích điểm)
    */
    public boolean coQuyenQuanLyKhachHang() {
        return this == ADMIN || this == NHAN_VIEN_THU_NGAN;
    }
    /**
    * Kiểm tra quyền Quản lý Ưu đãi
    * ✅ ADMIN: Toàn quyền (Thêm/Sửa/Xóa)
    * ✅ QUẢN LÝ: Toàn quyền (Thêm/Sửa/Xóa)
    * ❌ THU NGÂN: KHÔNG có quyền (chỉ áp dụng khi thanh toán)
    */
    public boolean coQuyenQuanLyUuDai() {
        return this == ADMIN || this == QUAN_LY;
    }
    /**
    * Kiểm tra quyền Tra cứu
    * ✅ ADMIN: Có quyền
    * ✅ QUẢN LÝ: Có quyền
    * ✅ THU NGÂN: Có quyền
    */
    public boolean coQuyenTraCuu() {
        return this == ADMIN || this == QUAN_LY;
    }
    /**
    * Lấy mô tả quyền hạn của vai trò
    */
    public String getMoTaQuyen() {
        switch (this) {
            case ADMIN:
            return "Admin: Tất cả quyền - Toàn quyền hệ thống";
            case QUAN_LY:
            return "Quản lý: Dashboard, Thống kê, Thực đơn, Nhân viên, Ưu đãi, Tra cứu";
            case NHAN_VIEN_THU_NGAN:
            return "Thu ngân: Dashboard, Đặt bàn, Hóa đơn, Khách hàng, Tra cứu";
            default:
            return "Không xác định";
        }
    }
    /**
    * Kiểm tra xem có phải là Admin không
    */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    /**
    * Kiểm tra xem có phải là Quản lý không
    */
    public boolean isQuanLy() {
        return this == QUAN_LY;
    }
    /**
    * Kiểm tra xem có phải là Thu ngân không
    */
    public boolean isThuNgan() {
        return this == NHAN_VIEN_THU_NGAN;
    }
    /**
    * In ra thông tin chi tiết phân quyền
    */
    public void inChiTietPhanQuyen() {
        System.out.println("========================================");
        System.out.println("🔐 PHÂN QUYỀN: " + getTenVaiTro());
        System.out.println("========================================");
        System.out.println((coQuyenDashboard() ? "✅" : "❌") + " Dashboard");
        System.out.println((coQuyenQuanLyDatBan() ? "✅" : "❌") + " Quản lý Đặt bàn");
        System.out.println((coQuyenThongKe() ? "✅" : "❌") + " Thống kê & Báo cáo");
        System.out.println((coQuyenQuanLyThucDon() ? "✅" : "❌") + " Quản lý Thực đơn");
        System.out.println((coQuyenQuanLyHoaDon() ? "✅" : "❌") + " Quản lý Hóa đơn");
        System.out.println((coQuyenQuanLyNhanVien() ? "✅" : "❌") + " Quản lý Nhân viên");
        System.out.println((coQuyenQuanLyKhachHang() ? "✅" : "❌") + " Quản lý Khách hàng");
        System.out.println((coQuyenQuanLyUuDai() ? "✅" : "❌") + " Quản lý Ưu đãi");
        System.out.println((coQuyenTraCuu() ? "✅" : "❌") + " Tra cứu");
        System.out.println("========================================");
    }
}