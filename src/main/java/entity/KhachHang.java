package entity;
import java.time.LocalDate;
import java.io.Serializable;
public class KhachHang implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maKH;
    private String tenKH;
    private String soDT;
    private String email;
    private LocalDate ngayDangKy;
    private String diaChi;
    private String thanhVien;
    private int diemTichLuy;
    private String matKhau;
    // Constructors
    public KhachHang() {
    }
    public KhachHang(String maKH, String tenKH, String soDT, String email, LocalDate ngayDangKy, String thanhVien) {
        this.maKH = maKH;
        this.tenKH = tenKH;
        this.soDT = soDT;
        this.email = email;
        this.ngayDangKy = ngayDangKy;
        this.thanhVien = thanhVien;
    }
    public KhachHang(String maKH, String tenKH, String soDT, String email, LocalDate ngayDangKy, String diaChi, String thanhVien) {
        this.maKH = maKH;
        this.tenKH = tenKH;
        this.soDT = soDT;
        this.email = email;
        this.ngayDangKy = ngayDangKy;
        this.diaChi = diaChi;
        this.thanhVien = thanhVien;
    }
    // Getters and Setters
    public String getMaKH() { return maKH; }
    public void setMaKH(String maKH) { this.maKH = maKH; }
    public String getTenKH() { return tenKH; }
    public void setTenKH(String tenKH) { this.tenKH = tenKH; }
    public String getSoDT() { return soDT; }
    public void setSoDT(String soDT) { this.soDT = soDT; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getNgayDangKy() { return ngayDangKy; }
    public void setNgayDangKy(LocalDate ngayDangKy) { this.ngayDangKy = ngayDangKy; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getThanhVien() { return thanhVien; }
    public void setThanhVien(String thanhVien) { this.thanhVien = thanhVien; }
    public int getDiemTichLuy() { return diemTichLuy; }
    public void setDiemTichLuy(int diemTichLuy) { this.diemTichLuy = diemTichLuy; }
    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
    // Alias for server-side compatibility
    public String getHangThanhVien() { return thanhVien; }
    @Override
    public String toString() {
        return "KhachHang{" +
            "maKH='" + maKH + '\'' +
            ", tenKH='" + tenKH + '\'' +
            ", soDT='" + soDT + '\'' +
            ", ngayDangKy='" + ngayDangKy + '\'' +
            '}';
    }
}