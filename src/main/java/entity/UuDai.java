package entity;
import java.time.LocalDate;
import java.io.Serializable;
public class UuDai implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maUuDai;
    private String tenUuDai;
    private String moTa;
    private double giaTri;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    // Constructor đầy đủ
    public UuDai(String maUuDai, String tenUuDai, String moTa, double giaTri,
    LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        this.maUuDai = maUuDai;
        this.tenUuDai = tenUuDai;
        this.moTa = moTa;
        this.giaTri = giaTri;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
    }
    // Constructor mặc định
    public UuDai() {
    }
    // Getters và Setters
    public String getMaUuDai() {
        return maUuDai;
    }
    public void setMaUuDai(String maUuDai) {
        this.maUuDai = maUuDai;
    }
    public String getTenUuDai() {
        return tenUuDai;
    }
    public void setTenUuDai(String tenUuDai) {
        this.tenUuDai = tenUuDai;
    }
    public String getMoTa() {
        return moTa;
    }
    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
    public double getGiaTri() {
        return giaTri;
    }
    public void setGiaTri(double giaTri) {
        this.giaTri = giaTri;
    }
    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }
    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }
    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }
    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }
    // Phương thức xác định trạng thái
    public String getTrangThai() {
        if (ngayBatDau == null || ngayKetThuc == null) {
            return "Chưa xác định";
        }
        LocalDate now = LocalDate.now();
        if (now.isBefore(ngayBatDau)) {
            return "Sắp diễn ra";
        } else if (now.isAfter(ngayKetThuc)) {
        return "Đã hết hạn";
    } else {
    return "Đang áp dụng";
}
}
@Override
public String toString() {
    return "UuDai{" +
        "maUuDai='" + maUuDai + '\'' +
        ", tenUuDai='" + tenUuDai + '\'' +
        ", moTa='" + moTa + '\'' +
        ", giaTri=" + giaTri +
        ", ngayBatDau=" + ngayBatDau +
        ", ngayKetThuc=" + ngayKetThuc +
        '}';
}
}