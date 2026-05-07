package entity;
import java.io.Serializable;
public class DanhMucMon implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maDM;
    private String tenDM;
    // Constructors
    public DanhMucMon() {
    }
    public DanhMucMon(String maDM, String tenDM) {
        this.maDM = maDM;
        this.tenDM = tenDM;
    }
    // Getters and Setters
    public String getMaDM() { return maDM; }
    public void setMaDM(String maDM) { this.maDM = maDM; }
    public String getTenDM() { return tenDM; }
    public void setTenDM(String tenDM) { this.tenDM = tenDM; }
    @Override
    public String toString() {
        return tenDM;
    }
}