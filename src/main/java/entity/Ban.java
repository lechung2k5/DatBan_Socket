package entity;
import java.util.Objects; // <<< THÊM IMPORT NÀY
import java.io.Serializable;
public class Ban implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maBan; //
    private String viTri; //
    private int sucChua; //
    private LoaiBan loaiBan; //
    private TrangThaiBan trangThai; //
    // Constructors
    public Ban() { //
    }
    public Ban(String maBan, String viTri, int sucChua, LoaiBan loaiBan, TrangThaiBan trangThai) { //
        this.maBan = maBan; //
        this.viTri = viTri; //
        this.sucChua = sucChua; //
        this.loaiBan = loaiBan; //
        this.trangThai = trangThai; //
    }
    // Getters and Setters
    public String getMaBan() { return maBan; } //
    public void setMaBan(String maBan) { this.maBan = maBan; } //
    public String getViTri() { return viTri; } //
    public void setViTri(String viTri) { this.viTri = viTri; } //
    public int getSucChua() { return sucChua; } //
    public void setSucChua(int sucChua) { this.sucChua = sucChua; } //
    public LoaiBan getLoaiBan() { return loaiBan; } //
    public void setLoaiBan(LoaiBan loaiBan) { this.loaiBan = loaiBan; } //
    public TrangThaiBan getTrangThai() { return trangThai; } //
    public void setTrangThai(TrangThaiBan trangThai) { this.trangThai = trangThai; } //
    // === THÊM equals() VÀ hashCode() ===
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Nếu cùng địa chỉ bộ nhớ -> true
        if (o == null || getClass() != o.getClass()) return false; // Nếu khác class hoặc null -> false
        Ban ban = (Ban) o;
        // Chỉ cần so sánh dựa trên mã bàn là đủ
        return Objects.equals(maBan, ban.maBan); // So sánh maBan
    }
    @Override
    public int hashCode() {
        // Chỉ cần hash dựa trên mã bàn
        return Objects.hash(maBan); // Hash maBan
    }
    // ===================================
    // (toString() nếu có)
    @Override
    public String toString() {
        return "Ban{" +
            "maBan='" + maBan + '\'' +
            ", trangThai=" + trangThai +
            '}';
    }
}