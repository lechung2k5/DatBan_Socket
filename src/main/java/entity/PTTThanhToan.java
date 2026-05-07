package entity;
/**
* Enum đại diện cho các Phương Thức Thanh Toán.
*/
public enum PTTThanhToan {
    TIEN_MAT("TienMat", "Tiền Mặt"),          // Mã trong DB, Tên hiển thị
    NGAN_HANG("NganHang", "Ngân Hàng"),
    VI_DIEN_TU("ViDienTu", "Ví Điện Tử");
    private final String dbValue; // Giá trị lưu trong CSDL (vd: "TienMat")
    private final String displayName; // Tên hiển thị trên giao diện (vd: "Tiền Mặt")
    PTTThanhToan(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }
    public String getDbValue() {
        return dbValue;
    }
    public String getDisplayName() {
        return displayName;
    }
    /**
    * Tìm Enum tương ứng dựa vào giá trị lưu trong CSDL.
    * @param dbValue Giá trị từ cột ptThanhToan trong DB (vd: "TienMat")
    * @return Enum PTTThanhToan tương ứng, hoặc null nếu không tìm thấy.
    */
    public static PTTThanhToan fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (PTTThanhToan pt : values()) {
            if (pt.dbValue.equalsIgnoreCase(dbValue)) {
                return pt;
            }
        }
        return null; // Hoặc ném Exception nếu muốn
    }
    /**
    * Tìm Enum tương ứng dựa vào tên hiển thị.
    * @param displayName Tên hiển thị (vd: "Tiền Mặt")
    * @return Enum PTTThanhToan tương ứng, hoặc null nếu không tìm thấy.
    */
    public static PTTThanhToan fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (PTTThanhToan pt : values()) {
            if (pt.displayName.equalsIgnoreCase(displayName)) {
                return pt;
            }
        }
        return null; // Hoặc ném Exception
    }
    @Override
    public String toString() {
        // Mặc định trả về tên hiển thị khi dùng trong ComboBox
        return displayName;
    }
}