package entity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.io.Serializable;
public class CaTruc implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maCa;
    private LocalDate ngay;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private NhanVien nhanVien;
    // Constructors, Getters, and Setters
    public CaTruc() {
    }
    public CaTruc(String maCa, LocalDate ngay, LocalTime gioBatDau, LocalTime gioKetThuc, NhanVien nhanVien) {
        this.maCa = maCa;
        this.ngay = ngay;
        this.gioBatDau = gioBatDau;
        this.gioKetThuc = gioKetThuc;
        this.nhanVien = nhanVien;
    }
    public String getMaCa() { return maCa; }
    public void setMaCa(String maCa) { this.maCa = maCa; }
    public LocalDate getNgay() { return ngay; }
    public void setNgay(LocalDate ngay) { this.ngay = ngay; }
    public LocalTime getGioBatDau() { return gioBatDau; }
    public void setGioBatDau(LocalTime gioBatDau) { this.gioBatDau = gioBatDau; }
    public LocalTime getGioKetThuc() { return gioKetThuc; }
    public void setGioKetThuc(LocalTime gioKetThuc) { this.gioKetThuc = gioKetThuc; }
    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }
    @Override
    public String toString() {
        return String.format("Ca (%s - %s)",
        gioBatDau.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
        gioKetThuc.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
    }
}