package entity;
import java.io.Serializable;
public class TaiKhoan implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenDangNhap;
    private String matKhau;
    private VaiTro vaiTro; // Sử dụng Enum VaiTro
    private NhanVien nhanVien;
    // Constructors
    public TaiKhoan() {
    }
    public TaiKhoan(String tenDangNhap, String matKhau, VaiTro vaiTro, NhanVien nhanVien) {
        this.tenDangNhap = tenDangNhap;
        this.matKhau = matKhau;
        this.vaiTro = vaiTro;
        this.nhanVien = nhanVien;
    }
    // Getters and Setters (Đã cập nhật vaiTro)
    public String getTenDangNhap() {
        return tenDangNhap;
    }
    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }
    public String getMatKhau() {
        return matKhau;
    }
    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }
    public VaiTro getVaiTro() {
        return vaiTro;
    }
    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }
    public NhanVien getNhanVien() {
        return nhanVien;
    }
    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }
}