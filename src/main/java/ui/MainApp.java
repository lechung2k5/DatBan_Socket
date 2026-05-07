package ui;
import entity.TaiKhoan;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import java.io.IOException;
import java.time.LocalDate;
// 🔥 Import Preferences và Exception
import java.util.prefs.Preferences;
import java.time.format.DateTimeParseException;
import java.util.prefs.BackingStoreException; // Thêm import này
public class MainApp extends Application {
    private Stage primaryStage;
    private Stage preloaderStage;
    private Image appIcon;
    private static TaiKhoan loggedInUser = null;
    public static void setLoggedInUser(TaiKhoan user) {
        loggedInUser = user;
    }
    public static TaiKhoan getLoggedInUser() {
        return loggedInUser;
    }
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setResizable(false);
        try {
            appIcon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            primaryStage.getIcons().add(appIcon);
        } catch (Exception e) {
        System.err.println("Không thể tải file icon: " + e.getMessage());
    }
    showPreloader();
}
/**
* Hiển thị màn hình Preloader (Splash Screen)
*/
public void showPreloader() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Preloader.fxml"));
        Parent root = loader.load();
        PreloaderController controller = loader.getController();
        controller.setMainApp(this);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/preloader.css").toExternalForm());
        preloaderStage = new Stage();
        if (appIcon != null) {
            preloaderStage.getIcons().add(appIcon);
        }
        preloaderStage.initStyle(StageStyle.TRANSPARENT);
        preloaderStage.setTitle("Đang tải...");
        preloaderStage.setScene(scene);
        preloaderStage.show();
    } catch (IOException e) {
    e.printStackTrace();
}
}
public void gotoLogin() {
    try {
        if (preloaderStage != null) {
            preloaderStage.close();
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DangNhap.fxml"));
        Parent root = loader.load();
        DangNhap dangNhapController = loader.getController();
        dangNhapController.setMainApp(this);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/dangNhap.css").toExternalForm());
        primaryStage.setTitle("Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.show();
    } catch (IOException e) {
    e.printStackTrace();
}
}
public void gotoQuenMatKhau() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuenMatKhau.fxml"));
        Parent root = loader.load();
        QuenMatKhau quenMatKhauController = loader.getController();
        quenMatKhauController.setMainApp(this);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/QuenMatKhau.css").toExternalForm());
        primaryStage.setTitle("Quên mật khẩu");
        primaryStage.setScene(scene);
        primaryStage.show();
    } catch (IOException e) {
    e.printStackTrace();
}
}
public void gotoMainScreen() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManHinhChinh.fxml"));
        Parent root = loader.load();
        ManHinhChinh manHinhChinhController = loader.getController();
        manHinhChinhController.setMainApp(this);
        if (loggedInUser != null) {
            manHinhChinhController.setUserInfo(loggedInUser);
        } else {
        System.err.println("Lỗi: Không có thông tin người dùng đăng nhập!");
        // Có thể quay lại màn hình đăng nhập ở đây
        // gotoLogin();
        // return;
    }
    Scene scene = new Scene(root);
    scene.getStylesheets().add(getClass().getResource("/css/manHinhChinh.css").toExternalForm());
    scene.getStylesheets().add(getClass().getResource("/css/DashBoard.css").toExternalForm());
    // Thêm các file CSS khác nếu cần
    Stage mainStage = new Stage();
    if (appIcon != null) {
        mainStage.getIcons().add(appIcon);
    }
    mainStage.setTitle("Quản lý nhà hàng Tứ Hữu"); // Sửa tiêu đề
    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    mainStage.setX(screenBounds.getMinX());
    mainStage.setY(screenBounds.getMinY());
    mainStage.setWidth(screenBounds.getWidth());
    mainStage.setHeight(screenBounds.getHeight());
    mainStage.setResizable(true); // Cho phép thay đổi kích thước
    mainStage.setScene(scene);
    mainStage.show();
    // Đóng cửa sổ đăng nhập
    if (primaryStage != null) {
        primaryStage.close();
    }
} catch (IOException e) {
e.printStackTrace();
// Hiển thị lỗi nghiêm trọng cho người dùng
}
}
// --- 🔥 HÀM LƯU TIỀN ĐẦU CA DÙNG PREFERENCES ---
/**
* Lưu số tiền kiểm kê đầu ca, ngày và mã NV vào User Preferences.
* @param amount Số tiền kiểm kê.
* @param maNV Mã nhân viên thực hiện kiểm kê.
*/
public static void setInitialCashCount(double amount, String maNV) {
    // Lấy node Preferences cho lớp MainApp
    Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
    LocalDate today = LocalDate.now();
    String dateString = today.toString(); // Định dạng YYYY-MM-DD
    // Ghi các giá trị
    prefs.putDouble("initialCashCount", amount);
    prefs.put("initialCashCountDate", dateString);
    prefs.put("initialCashCountUser", maNV);
    try {
        // Ép buộc ghi dữ liệu xuống bộ nhớ lưu trữ (quan trọng)
        prefs.flush();
        System.out.println("MainApp (Prefs): Đã lưu tiền đầu ca = " + amount + " cho NV " + maNV + " vào ngày " + dateString);
    } catch (BackingStoreException e) { // Bắt lỗi cụ thể hơn
    System.err.println("MainApp (Prefs): Lỗi nghiêm trọng khi flush Preferences: " + e.getMessage());
    e.printStackTrace(); // In chi tiết lỗi
    // Có thể hiển thị thông báo lỗi cho người dùng ở đây
} catch (Exception e) { // Bắt các lỗi khác (ít khả năng)
System.err.println("MainApp (Prefs): Lỗi không xác định khi flush Preferences: " + e.getMessage());
e.printStackTrace();
}
}
// --- 🔥 HÀM LẤY TIỀN ĐẦU CA DÙNG PREFERENCES ---
/**
* Lấy số tiền kiểm kê đầu ca đã lưu cho NV và ngày hôm nay từ User Preferences.
* @param maNV Mã nhân viên.
* @return Số tiền đầu ca, hoặc -1.0 nếu chưa kiểm kê, không khớp NV/ngày, hoặc có lỗi.
*/
public static double getInitialCashCount(String maNV) {
    Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
    LocalDate today = LocalDate.now();
    String todayString = today.toString();
    // Lấy các giá trị đã lưu, với giá trị mặc định nếu không tồn tại
    double storedAmount = prefs.getDouble("initialCashCount", -1.0);
    String storedDateString = prefs.get("initialCashCountDate", null);
    String storedUser = prefs.get("initialCashCountUser", null);
    System.out.println("--- Bên trong getInitialCashCount (Prefs) ---");
    System.out.println("Yêu cầu cho mã NV: " + maNV);
    System.out.println("NV đã lưu (Prefs): " + storedUser);
    System.out.println("Ngày đã lưu (Prefs): " + storedDateString);
    System.out.println("Ngày hôm nay: " + todayString);
    boolean userMatch = maNV != null && maNV.equals(storedUser);
    boolean dateMatch = false;
    if (storedDateString != null) {
        try {
            LocalDate storedDate = LocalDate.parse(storedDateString); // Chuyển chuỗi về LocalDate
            dateMatch = storedDate.equals(today);
        } catch (DateTimeParseException e) {
        System.err.println("MainApp (Prefs): Lỗi parse ngày đã lưu: " + storedDateString + ". Coi như không khớp.");
        dateMatch = false;
    }
}
System.out.println("Khớp NV? " + userMatch);
System.out.println("Khớp Ngày? " + dateMatch);
System.out.println("Giá trị tiền đã lưu? " + storedAmount);
System.out.println("---------------------------------");
// Điều kiện kiểm tra đầy đủ: Khớp User, Khớp Ngày, và Số tiền hợp lệ (>= 0)
if (userMatch && dateMatch && storedAmount >= 0) {
    System.out.println("MainApp (Prefs): TÌM THẤY tiền đầu ca hợp lệ = " + storedAmount + " cho NV " + maNV);
    return storedAmount;
}
System.out.println("MainApp (Prefs): KHÔNG tìm thấy tiền đầu ca hợp lệ cho NV " + maNV + " hôm nay.");
return -1.0; // Trả về -1 nếu không khớp hoặc chưa lưu
}
// --- Hàm main (giữ nguyên) ---
public static void main(String[] args) {
    launch(args);
}
}