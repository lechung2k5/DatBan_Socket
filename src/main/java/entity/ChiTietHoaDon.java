package entity;
import java.io.Serializable;
/**
* Lớp Entity (Model) đại diện cho một dòng trong bảng Chi Tiết Hóa Đơn.
* Đã loại bỏ JavaFX Properties để hỗ trợ Serialization qua Socket.
*/
public class ChiTietHoaDon implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenMon;
    private int soLuong;
    private double donGia;
    private double thanhTien;
    public ChiTietHoaDon() {
    }
    public ChiTietHoaDon(String tenMon, int soLuong, double donGia, double thanhTien) {
        this.tenMon = tenMon;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.thanhTien = thanhTien;
    }
    // --- Getters & Setters ---
    public String getTenMon() { return tenMon; }
    public void setTenMon(String value) { this.tenMon = value; }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int value) { this.soLuong = value; }
    public double getDonGia() { return donGia; }
    public void setDonGia(double value) { this.donGia = value; }
    public double getThanhTien() { return thanhTien; }
    public void setThanhTien(double value) { this.thanhTien = value; }
    // 🔥 Helper for UI compatibility (Nếu cần)
    public void setMonAn(MonAn monAn) {
        if (monAn != null) {
            this.tenMon = monAn.getTenMon();
            this.donGia = monAn.getGiaBan();
        }
    }
}