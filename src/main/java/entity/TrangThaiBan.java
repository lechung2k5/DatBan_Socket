package entity;
import com.google.gson.annotations.SerializedName;
/**
* Enum đại diện cho các Trạng Thái Bàn.
* Đã cập nhật để đồng bộ với các DAO và Controller.
*/
public enum TrangThaiBan {
    @SerializedName("Trong")
    TRONG("Trong", "Trống"),                      // dbValue, displayName
    @SerializedName("DangSuDung")
    DANG_SU_DUNG("DangSuDung", "Đang sử dụng"),
    @SerializedName("Dat")
    DA_DAT("Dat", "Đã đặt"); // Sử dụng "Dat" làm dbValue khớp với script SQL INSERT
    private final String dbValue;
    private final String displayName;
    TrangThaiBan(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }
    /**
    * Lấy giá trị được lưu trữ trong cơ sở dữ liệu (ví dụ: "Trong", "DangSuDung", "Dat").
    * @return Giá trị dbValue.
    */
    public String getDbValue() {
        return dbValue;
    }
    /**
    * Lấy tên hiển thị thân thiện trên giao diện (ví dụ: "Trống", "Đang sử dụng", "Đã đặt").
    * @return Giá trị displayName.
    */
    public String getDisplayName() {
        return displayName;
    }
    /**
    * Chuyển đổi giá trị String từ cơ sở dữ liệu thành Enum TrangThaiBan tương ứng.
    * Xử lý các giá trị phổ biến ("Trong", "DangSuDung", "Dat").
    * @param dbValue Giá trị String đọc từ cột `trangThai` của bảng Ban.
    * @return Enum TrangThaiBan tương ứng, mặc định là TRONG nếu không khớp hoặc null.
    */
    public static TrangThaiBan fromDbValue(String dbValue) {
        if (dbValue == null) {
            return TRONG; // Mặc định là Trống nếu DB là NULL
        }
        // Chuẩn hóa chuỗi đầu vào (bỏ qua khoảng trắng, viết hoa, bỏ gạch dưới)
        String standardizedDbValue = dbValue.trim().toUpperCase().replace("_", "");
        // Ánh xạ chính xác các giá trị từ SQL
        switch (standardizedDbValue) {
            case "TRONG":
                return TRONG;
            case "DANGSUDUNG":
            case "DANGPHUCVU":
                return DANG_SU_DUNG;
            case "DAT":
            case "DADAT":
                return DA_DAT;
            default:
                System.err.println("WARNING: Giá trị TrangThaiBan không xác định từ DB: '" + dbValue + "'. Mặc định thành TRONG.");
                return TRONG;
        }
    }
    /**
    * Trả về tên hiển thị khi gọi toString() (tiện lợi cho việc hiển thị).
    */
    @Override
    public String toString() {
        return displayName;
    }
}