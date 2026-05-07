package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class ConnectDB {
    // --- Cấu hình cho SQL Server ---
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=NhaHang;encrypt=false;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "sapassword";
    // -------------------------------
    /**
    * Phương thức này giờ sẽ tạo và trả về một KẾT NỐI MỚI mỗi khi được gọi.
    * Nó không còn quản lý một biến 'connection' tĩnh nữa.
    * @return Một đối tượng Connection mới hoặc null nếu thất bại.
    */
    public static Connection getConnection() {
        try {
            // Trả về một kết nối MỚI mỗi lần
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
        System.err.println("❌ Lỗi kết nối CSDL: " + e.getMessage());
        e.printStackTrace();
        return null;
    }
}
/**
* Phương thức này không còn cần thiết vì 'try-with-resources'
* trong các lớp DAO sẽ tự động đóng kết nối.
*/
// public static void closeConnection() { ... }
}