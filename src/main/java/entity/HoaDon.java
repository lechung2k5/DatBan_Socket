package entity;
import java.time.LocalDateTime;
/**
* Lớp Entity (Model) đại diện cho đối tượng Hóa Đơn.
* Đã cập nhật hoàn chỉnh: Thêm Setters cho các trường tính toán để đồng bộ với UI.
*/
import java.io.Serializable;
public class HoaDon implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maHD;
    private LocalDateTime ngayLap;
    private PTTThanhToan hinhThucTT;
    private TrangThaiHoaDon trangThai;
    private String maUuDai;
    private KhachHang khachHang;
    private NhanVien nhanVien;
    private String tenNhanVien;
    private Ban ban;
    private LocalDateTime gioVao;
    private LocalDateTime gioRa;
    private double tongCongMonAn;
    private double tienCoc;
    // Các trường tính toán
    private double phiDichVu;
    private double thueVAT;
    private double khuyenMai;
    private double tongTienThanhToan;
    private String maHDGoc;
    private java.util.List<ChiTietHoaDon> chiTietHoaDon = new java.util.ArrayList<>();
    // Constructors
    public HoaDon() {
        // Constructor rỗng
    }
    // Hàm tính toán tổng tiền (Logic mặc định nếu không set từ ngoài)
    public void calculateTotals() {
        // 1. Phí dịch vụ 5% trên tổng món
        this.phiDichVu = this.tongCongMonAn * 0.05;
        // 2. Thuế VAT 8% trên (tổng món + phí dịch vụ) - Đồng bộ với UI
        this.thueVAT = (this.tongCongMonAn + this.phiDichVu) * 0.08;
        // 3. Tổng thanh toán
        this.tongTienThanhToan = this.tongCongMonAn + this.phiDichVu + this.thueVAT - this.tienCoc - this.khuyenMai;
        if (this.tongTienThanhToan < 0) {
            this.tongTienThanhToan = 0;
        }
    }
    // --- Getters ---
    public String getMaHD() { return maHD; }
    public LocalDateTime getNgayLap() { return ngayLap; }
    public PTTThanhToan getHinhThucTT() { return hinhThucTT; }
    public TrangThaiHoaDon getTrangThai() { return trangThai; }
    public String getMaUuDai() { return maUuDai; }
    public KhachHang getKhachHang() { return khachHang; }
    public NhanVien getNhanVien() { return nhanVien; }
    public String getTenNhanVien() { return tenNhanVien; }
    public Ban getBan() { return ban; }
    public LocalDateTime getGioVao() { return gioVao; }
    public LocalDateTime getGioRa() { return gioRa; }
    public double getTongCongMonAn() { return tongCongMonAn; }
    public double getTienCoc() { return tienCoc; }
    public double getPhiDichVu() { return phiDichVu; }
    public double getThueVAT() { return thueVAT; }
    public double getKhuyenMai() { return khuyenMai; }
    public double getTongTienThanhToan() { return tongTienThanhToan; }
    public String getMaHDGoc() { return maHDGoc; }
    // --- Getters tiện ích cho UI ---
    public String getMaBan() {
        return (this.ban != null) ? this.ban.getMaBan() : null;
    }
    public String getSoDienThoaiKH() {
        return (this.khachHang != null) ? this.khachHang.getSoDT() : null;
    }
    // --- Setters ---
    public void setMaHD(String maHD) { this.maHD = maHD; }
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }
    public void setHinhThucTT(PTTThanhToan hinhThucTT) { this.hinhThucTT = hinhThucTT; }
    public void setMaHDGoc(String maHDGoc) { this.maHDGoc = maHDGoc; }
    public void setTrangThai(TrangThaiHoaDon trangThai) { this.trangThai = trangThai; }
    public void setTrangThai(String trangThaiDbValue) {
        this.trangThai = TrangThaiHoaDon.fromDbValue(trangThaiDbValue);
    }
    public void setMaUuDai(String maUuDai) { this.maUuDai = maUuDai; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }
    public void setTenNhanVien(String tenNhanVien) { this.tenNhanVien = tenNhanVien; }
    public void setBan(Ban ban) { this.ban = ban; }
    public void setMaBan(String maBan) {
        if (this.ban == null) this.ban = new Ban();
        this.ban.setMaBan(maBan);
    }
    public void setGioVao(LocalDateTime gioVao) { this.gioVao = gioVao; }
    public void setGioRa(LocalDateTime gioRa) { this.gioRa = gioRa; }
    public void setTongCongMonAn(double tongCongMonAn) {
        this.tongCongMonAn = tongCongMonAn;
    }
    public void setTienCoc(double tienCoc) {
        this.tienCoc = tienCoc;
    }
    public void setKhuyenMai(double khuyenMai) {
        this.khuyenMai = khuyenMai;
    }
    // 🔥 [THÊM MỚI] Các Setter cho trường tính toán (Để khớp với DatBan.java)
    public void setPhiDichVu(double phiDichVu) {
        this.phiDichVu = phiDichVu;
    }
    public void setThueVAT(double thueVAT) {
        this.thueVAT = thueVAT;
    }
    public void setTongTienThanhToan(double tongTienThanhToan) {
        this.tongTienThanhToan = tongTienThanhToan;
    }
    public java.util.List<ChiTietHoaDon> getChiTietHoaDon() {
        return chiTietHoaDon;
    }
    public void setChiTietHoaDon(java.util.List<ChiTietHoaDon> chiTietHoaDon) {
        this.chiTietHoaDon = chiTietHoaDon;
    }
}