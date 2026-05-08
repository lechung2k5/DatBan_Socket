package entity;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.io.Serializable;
public class NhanVien implements Serializable {
    private static final long serialVersionUID = 1L;
    // Thuộc tính Entity (Serializable - Dữ liệu thô để gửi qua mạng)
    private String maNV_entity;
    private String tenNV_entity;
    private String soDT_entity;
    private String email_entity;
    private LocalDate ngaySinh_entity;
    private String diaChi_entity;
    private boolean gioiTinh_entity; // true = Nam, false = Nữ
    private boolean trangThai_entity; // true = Đang làm
    private String caLamYeuThich_entity;
    private String username_entity;
    private String chucVu_entity;
    private String matKhau_entity;
    // Thuộc tính JavaFX (Transient - Không gửi qua mạng)
    private transient StringProperty maNV;
    private transient StringProperty hoTen;
    private transient StringProperty sdt;
    private transient StringProperty email;
    private transient StringProperty ngaySinh;
    private transient StringProperty diaChi;
    private transient StringProperty gioiTinh;
    private transient StringProperty trangThai;
    private transient StringProperty chucVu;
    private transient StringProperty matKhau;
    private transient StringProperty caLam;
    private transient StringProperty caLamYeuThich;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public NhanVien() {
        // Không gọi initProperties() ở đây để Gson có thể nạp dữ liệu vào các biến _entity trước.
    }
    /**
    * Khởi tạo lại các JavaFX Properties từ dữ liệu entity thô.
    * Cần thiết sau khi Deserialization vì các trường transient sẽ bị null.
    */
    private void initProperties() {
        if (maNV == null) maNV = new SimpleStringProperty(maNV_entity);
        if (hoTen == null) hoTen = new SimpleStringProperty(tenNV_entity);
        if (sdt == null) sdt = new SimpleStringProperty(soDT_entity);
        if (email == null) email = new SimpleStringProperty(email_entity);
        if (ngaySinh == null) ngaySinh = new SimpleStringProperty(ngaySinh_entity != null ? ngaySinh_entity.toString() : "");
        if (diaChi == null) diaChi = new SimpleStringProperty(diaChi_entity);
        if (gioiTinh == null) gioiTinh = new SimpleStringProperty(gioiTinh_entity ? "Nam" : "Nữ");
        if (trangThai == null) trangThai = new SimpleStringProperty(trangThai_entity ? "Đang làm" : "Nghỉ việc");
        if (caLamYeuThich == null) caLamYeuThich = new SimpleStringProperty(caLamYeuThich_entity);
        if (chucVu == null) chucVu = new SimpleStringProperty(chucVu_entity);
        if (matKhau == null) matKhau = new SimpleStringProperty(matKhau_entity);
        if (caLam == null) caLam = new SimpleStringProperty();
    }
    // --- Getters & Setters cho JavaFX Properties (Dùng cho UI Binding) ---
    public String getMaNV() { initProperties(); return maNV.get(); }
    public void setMaNV(String value) { initProperties(); this.maNV.set(value); this.maNV_entity = value; }
    public StringProperty maNVProperty() { initProperties(); return maNV; }
    public String getHoTen() { initProperties(); return hoTen.get(); }
    public void setHoTen(String value) { initProperties(); this.hoTen.set(value); this.tenNV_entity = value; }
    public StringProperty hoTenProperty() { initProperties(); return hoTen; }
    public String getSdt() { initProperties(); return sdt.get(); }
    public void setSdt(String value) { initProperties(); this.sdt.set(value); this.soDT_entity = value; }
    public StringProperty sdtProperty() { initProperties(); return sdt; }
    public String getEmail() { initProperties(); return email.get(); }
    public void setEmail(String value) { initProperties(); this.email.set(value); this.email_entity = value; }
    public StringProperty emailProperty() { initProperties(); return email; }
    public String getNgaySinh() { initProperties(); return ngaySinh.get(); }
    public void setNgaySinh(String value) {
        initProperties();
        this.ngaySinh.set(value);
        this.ngaySinh_entity = parseDate(value);
    }
    public StringProperty ngaySinhProperty() { initProperties(); return ngaySinh; }
    public String getDiaChi() { initProperties(); return diaChi.get(); }
    public void setDiaChi(String value) { initProperties(); this.diaChi.set(value); this.diaChi_entity = value; }
    public StringProperty diaChiProperty() { initProperties(); return diaChi; }
    public String getGioiTinh() { initProperties(); return gioiTinh.get(); }
    public void setGioiTinh(String value) {
        initProperties();
        this.gioiTinh.set(value);
        this.gioiTinh_entity = "Nam".equalsIgnoreCase(value);
    }
    public StringProperty gioiTinhProperty() { initProperties(); return gioiTinh; }
    public String getTrangThai() { initProperties(); return trangThai.get(); }
    public void setTrangThai(String value) {
        initProperties();
        this.trangThai.set(value);
        this.trangThai_entity = "Đang làm".equalsIgnoreCase(value);
    }
    public StringProperty trangThaiProperty() { initProperties(); return trangThai; }
    public String getChucVu() { initProperties(); return chucVu.get(); }
    public void setChucVu(String value) { initProperties(); this.chucVu.set(value); this.chucVu_entity = value; }
    public StringProperty chucVuProperty() { initProperties(); return chucVu; }
    public String getMatKhau() { initProperties(); return matKhau.get(); }
    public void setMatKhau(String value) { initProperties(); this.matKhau.set(value); this.matKhau_entity = value; }
    public StringProperty matKhauProperty() { initProperties(); return matKhau; }
    public String getCaLam() { initProperties(); return caLam.get(); }
    public void setCaLam(String value) { initProperties(); this.caLam.set(value); }
    public StringProperty caLamProperty() { initProperties(); return caLam; }
    public String getCaLamYeuThich() { initProperties(); return caLamYeuThich.get(); }
    public void setCaLamYeuThich(String value) {
        initProperties();
        this.caLamYeuThich.set(value);
        this.caLamYeuThich_entity = value;
    }
    public StringProperty caLamYeuThichProperty() { initProperties(); return caLamYeuThich; }
    // --- Getters & Setters cho Entity (Dùng cho DAO/DB) ---
    public String getTenNV_entity() { return tenNV_entity; }
    public String getSoDT_entity() { return soDT_entity; }
    public String getEmail_entity() { return email_entity; }
    public LocalDate getNgaySinh_entity() { return ngaySinh_entity; }
    public String getDiaChi_entity() { return diaChi_entity; }
    public boolean getGioiTinh_entity() { return gioiTinh_entity; }
    public boolean getTrangThai_entity() { return trangThai_entity; }
    public String getCaLamYeuThich_entity() { return caLamYeuThich_entity; }
    public String getUsername_entity() { return username_entity; }
    public void setUsername_entity(String username) { this.username_entity = username; }
    public String getChucVu_entity() { return chucVu_entity; }
    public String getMatKhau_entity() { return matKhau_entity; }
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
        return null;
    }
}
}