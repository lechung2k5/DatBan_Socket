package ui;
// ========================================================================
// IMPORTS
// ========================================================================
import entity.TaiKhoan;
import entity.VaiTro;
import entity.HoaDon;
import entity.KhachHang;
// === IMPORTS CLEANED UP ===
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.InputStream; // Thêm để đọc file từ resource
import java.nio.file.Files; // Thêm để copy file ra temp
import java.nio.file.StandardCopyOption; // Option copy
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// Imports cho chức năng mở file PDF
import java.awt.Desktop;
import java.io.File;
// Imports từ các chức năng khác (Email, DAO...)
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import network.CommandType;
import network.Client;
import utils.ClientSessionManager;
import javafx.util.Pair;
import utils.JsonUtil;
import network.Response;
public class ManHinhChinh {
    // ========================================================================
    // FXML FIELDS
    // ========================================================================
    @FXML private BorderPane contentArea;
    @FXML private VBox menuItems;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private ImageView profileImage;
    // Sidebar Buttons
    @FXML private Button manHinhChinhButton;
    @FXML private Button quanLyDatBanButton;
    @FXML private Button quanLyThongKeButton;
    @FXML private Button quanLyThucDonButton;
    @FXML private Button quanLyHoaDonButton;
    @FXML private Button quanLyNhanVienButton;
    @FXML private Button quanLyKhachHangButton;
    @FXML private Button quanLyKhuyenMaiButton;
    @FXML private Button quanLyTraCuuButton;
    @FXML private Button dangXuatButton;
    // Sidebar Icons
    @FXML private ImageView manHinhChinhIcon;
    @FXML private ImageView quanLyDatBanIcon;
    @FXML private ImageView quanLyThongKeIcon;
    @FXML private ImageView quanLyThucDonIcon;
    @FXML private ImageView quanLyHoaDonIcon;
    @FXML private ImageView quanLyNhanVienIcon;
    @FXML private ImageView quanLyKhachHangIcon;
    @FXML private ImageView quanLyKhuyenMaiIcon;
    @FXML private ImageView quanLyTraCuuIcon;
    @FXML private ImageView dangXuatIcon;
    // TopBar & Theme
    @FXML private HBox topBar;
    @FXML private VBox sidebar;
    @FXML private ImageView logoImageView;
    // Menu Items
    @FXML private MenuItem doiMatKhauMenuItem;
    @FXML private MenuItem caiDatMenuItem;
    @FXML private MenuItem toggleThemeMenuItem;
    @FXML private MenuItem dangXuatMenuItem;
    @FXML private MenuItem thoatMenuItem;
    @FXML private MenuItem guiChuongTrinhTVMenuItem;
    @FXML private MenuItem xemLogKiemKeTienMatMenuItem;
    @FXML private MenuItem huongDanMenuItem;
    @FXML private MenuItem gioiThieuMenuItem;
    // ========================================================================
    // BIẾN INSTANCE
    // ========================================================================
    private entity.NhanVien currentNhanVien;
    private MainApp mainApp;
    private Button activeButton;
    private final Map<Button, String> defaultIcons = new HashMap<>();
    private final Map<Button, String> activeIcons = new HashMap<>();
    private final Map<Button, ImageView> buttonIconMap = new HashMap<>();
    private Object currentController;
    private TaiKhoan currentUser;
    private DatBan datBanController;
    
    // 🔥 CACHE MÀN HÌNH ĐỂ TRÁNH RE-LOAD FXML VÀ DƯ THỪA LISTENER (MEMORY LEAK)
    private final Map<String, Parent> screenCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    // === DAOs REMOVED ===
    private boolean isDarkMode = false;
    private final String darkStyleClass = "dark-mode";
    // --- CẤU HÌNH ĐƯỜNG DẪN PDF (Tương đối từ thư mục resources) ---
    // Đảm bảo bạn đã copy file vào src/main/resources/pdf/
    private static final String GIOI_THIEU_PDF_PATH = "/pdf/gioithieu.pdf";
    private static final String HUONG_DAN_PDF_PATH = "/pdf/huongdan.pdf";
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
    @FXML
    public void initialize() {
        // Map Buttons
        buttonIconMap.put(manHinhChinhButton, manHinhChinhIcon);
        buttonIconMap.put(quanLyDatBanButton, quanLyDatBanIcon);
        buttonIconMap.put(quanLyThongKeButton, quanLyThongKeIcon);
        buttonIconMap.put(quanLyThucDonButton, quanLyThucDonIcon);
        buttonIconMap.put(quanLyHoaDonButton, quanLyHoaDonIcon);
        buttonIconMap.put(quanLyNhanVienButton, quanLyNhanVienIcon);
        buttonIconMap.put(quanLyKhachHangButton, quanLyKhachHangIcon);
        buttonIconMap.put(quanLyKhuyenMaiButton, quanLyKhuyenMaiIcon);
        buttonIconMap.put(quanLyTraCuuButton, quanLyTraCuuIcon);
        buttonIconMap.put(dangXuatButton, dangXuatIcon);
        // Map Icons - Hãy đảm bảo tên file trong src/main/resources/icons khớp với tên ở đây
        addIconMapping(manHinhChinhButton, "/icons/iconHome.png", "/icons/iconHome_White.png");
        addIconMapping(quanLyDatBanButton, "/icons/iconDatBan.png", "/icons/iconDatBan_White.png");
        addIconMapping(quanLyThongKeButton, "/icons/iconThongKe.png", "/icons/iconThongKe_White.png");
        addIconMapping(quanLyThucDonButton, "/icons/iconThucDon.png", "/icons/iconThucDon_White.png");
        addIconMapping(quanLyHoaDonButton, "/icons/iconHoaDon.png", "/icons/iconHoaDon_White.png");
        addIconMapping(quanLyNhanVienButton, "/icons/iconNhanVienMenu.png", "/icons/iconNhanVien_White.png");
        addIconMapping(quanLyKhachHangButton, "/icons/iconKhachHang.png", "/icons/iconKhachHang_White.png");
        addIconMapping(quanLyKhuyenMaiButton, "/icons/iconKhuyenMai.png", "/icons/iconKhuyenMai_White.png");
        addIconMapping(quanLyTraCuuButton, "/icons/iconTraCuu.png", "/icons/iconTraCuu_White.png");
        addIconMapping(dangXuatButton, "/icons/iconDangXuat.png", "/icons/iconDangXuat_White.png");
        // Load Logo - Đã sửa thành logo.png cho chuẩn
        try {
            if (logoImageView != null) {
                // Kiểm tra tên file logo của bạn trong src/main/resources/images
                logoImageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
            }
        } catch (Exception e) {
        System.err.println("Lỗi tải logo: " + e.getMessage());
    }
    setActiveButton(manHinhChinhButton);
    try {
        loadScreen("/fxml/Dashboard.fxml", "/css/Dashboard.css");
    } catch (IOException e) {
    e.printStackTrace();
}
contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
    if (newScene != null) {
        setupKeyboardShortcuts();
    }
});
}
@FXML
private void handleXemNhatKyKiemKe() {
    try {
        // Đường dẫn file log mà bạn đã lưu ở hàm nhập tiền
        java.io.File file = new java.io.File("nhat_ky_kiem_ke.txt");
        if (!file.exists()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Chưa có dữ liệu nhật ký kiểm kê nào được ghi lại.");
            return;
        }
        // NGHIỆP VỤ CHUẨN: Mở trực tiếp file nhật ký để xem lịch sử ca trước
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(file);
        } else {
        // Nếu không mở được file trực tiếp, hiện thông báo lỗi
        showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Máy tính không hỗ trợ mở tệp văn bản trực tiếp.");
    }
} catch (Exception e) {
e.printStackTrace();
showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở nhật ký kiểm kê.");
}
}
private double getDoanhThuTrongNgay() {
    try {
        Response res = Client.sendWithParams(CommandType.GET_STATS, Map.of("date", LocalDate.now().toString()));
        if (res.getStatusCode() == 200) {
            Map<String, Object> stats = (Map<String, Object>) res.getData();
            return Double.parseDouble(String.valueOf(stats.getOrDefault("totalRevenue", 0)));
        }
    } catch (Exception e) {
    System.err.println("Lỗi truy vấn doanh thu qua API: " + e.getMessage());
}
return 0;
}
public void chuyenSangTabDatBan(HoaDon hd) {
    try {
        setActiveButton(quanLyDatBanButton);
        loadScreen("/fxml/QuanLyDatBan.fxml", "/css/DatBan.css");
        if (this.datBanController != null) {
            if (hd != null) {
                this.datBanController.loadHoaDonToMainInterface(hd, false);
            } else {
            this.datBanController.clearFormDatBan();
        }
    }
} catch (Exception e) {
e.printStackTrace();
showAlert(Alert.AlertType.ERROR, "Lỗi chuyển tab", "Không thể chuyển sang màn hình Đặt bàn: " + e.getMessage());
}
}
private void setupKeyboardShortcuts() {
    contentArea.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case DIGIT1 -> { if (manHinhChinhButton.isVisible()) { try { handleManHinhChinh(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case DIGIT2 -> { if (quanLyDatBanButton.isVisible()) { try { handleQuanLyDatBan(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case DIGIT3 -> { if (quanLyHoaDonButton.isVisible()) { try { handleQuanLyHoaDon(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case DIGIT4 -> { if (quanLyKhachHangButton.isVisible()) { try { handleQuanLyKhachHang(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case DIGIT5 -> { if (quanLyThucDonButton.isVisible()) { try { handleQuanLyThucDon(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case DIGIT6 -> { if (quanLyKhuyenMaiButton.isVisible()) { try { handleQuanLyKhuyenMai(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case DIGIT7 -> { if (quanLyNhanVienButton.isVisible()) { try { handleQuanLyNhanVien(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case DIGIT8 -> { if (quanLyThongKeButton.isVisible()) { try { handleQuanLyThongKe(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case T -> { if (quanLyTraCuuButton.isVisible()) { try { handleQuanLyTraCuu(); } catch (IOException e) { e.printStackTrace(); } event.consume(); } }
                case Q -> { handleDangXuat(); event.consume(); }
                default -> {}
            }
        }
        else if (event.getCode() == KeyCode.F1) { handleHuongDan(); event.consume(); }
        else if (event.getCode() == KeyCode.F5) { refreshCurrentScreen(); event.consume(); }
    });
}
private void refreshCurrentScreen() {
    try {
        if (activeButton == manHinhChinhButton) loadScreen("/fxml/Dashboard.fxml", "/css/Dashboard.css");
        else if (activeButton == quanLyDatBanButton) loadScreen("/fxml/QuanLyDatBan.fxml", "/css/DatBan.css");
        else if (activeButton == quanLyHoaDonButton) loadScreen("/fxml/QuanLyHoaDon.fxml", "/css/HoaDon.css");
        else if (activeButton == quanLyKhachHangButton) loadScreen("/fxml/QuanLyKhachHang.fxml", "/css/KhachHang.css");
        else if (activeButton == quanLyThucDonButton) loadScreen("/fxml/QuanLyThucDon.fxml", "/css/ThucDon.css");
        else if (activeButton == quanLyKhuyenMaiButton) loadScreen("/fxml/QuanLyKhuyenMai.fxml", "/css/KhuyenMai.css");
        else if (activeButton == quanLyNhanVienButton) loadScreen("/fxml/QuanLyNhanVien.fxml", "/css/NhanVien.css");
        else if (activeButton == quanLyThongKeButton) loadScreen("/fxml/QuanLyThongKe.fxml", "/css/ThongKe.css");
        else if (activeButton == quanLyTraCuuButton) loadScreen("/fxml/QuanLyTraCuu.fxml", "/css/TraCuu.css");
    } catch (IOException e) {
    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể làm mới trang: " + e.getMessage());
}
}
public void setUserInfo(TaiKhoan user) {
    this.currentUser = user;
    if (user != null && user.getNhanVien() != null) {
        userNameLabel.setText(user.getNhanVien().getHoTen());
        userRoleLabel.setText(user.getVaiTro() != null ? user.getVaiTro().getTenVaiTro() : "Không xác định");
        apDungPhanQuyen();
    } else {
    userNameLabel.setText("Khách");
    userRoleLabel.setText("Chưa đăng nhập");
    apDungPhanQuyen();
}
}
private void apDungPhanQuyen() {
    if (currentUser == null || currentUser.getVaiTro() == null) {
        hienThiMenu(manHinhChinhButton, true);
        hienThiMenu(quanLyDatBanButton, false);
        hienThiMenu(dangXuatButton, false);
        return;
    }
    VaiTro vaiTro = currentUser.getVaiTro();
    System.out.println("DEBUG: User hien tai: " + currentUser.getTenDangNhap());
    System.out.println("DEBUG: Vai tro hien tai la: " + (vaiTro != null ? vaiTro.name() : "NULL"));
    // 🔥 ĐẶC CÁCH CHO ADMIN: Hiện tất cả các tab
    if (vaiTro == VaiTro.ADMIN) {
        hienThiMenu(manHinhChinhButton, true);
        hienThiMenu(quanLyDatBanButton, true);
        hienThiMenu(quanLyThongKeButton, true);
        hienThiMenu(quanLyThucDonButton, true);
        hienThiMenu(quanLyHoaDonButton, true);
        hienThiMenu(quanLyNhanVienButton, true);
        hienThiMenu(quanLyKhachHangButton, true);
        hienThiMenu(quanLyKhuyenMaiButton, true);
        hienThiMenu(quanLyTraCuuButton, true);
        return;
    }
    // --- PHÂN QUYỀN CHO CÁC VAI TRÒ KHÁC ---
    hienThiMenu(manHinhChinhButton, vaiTro.coQuyenDashboard());
    hienThiMenu(quanLyDatBanButton, vaiTro.coQuyenQuanLyDatBan());
    hienThiMenu(quanLyThongKeButton, vaiTro.coQuyenThongKe());
    hienThiMenu(quanLyThucDonButton, vaiTro.coQuyenQuanLyThucDon());
    hienThiMenu(quanLyHoaDonButton, vaiTro.coQuyenQuanLyHoaDon());
    hienThiMenu(quanLyNhanVienButton, vaiTro.coQuyenQuanLyNhanVien());
    hienThiMenu(quanLyKhachHangButton, vaiTro.coQuyenQuanLyKhachHang());
    hienThiMenu(quanLyKhuyenMaiButton, vaiTro.coQuyenQuanLyUuDai());
    hienThiMenu(quanLyTraCuuButton, vaiTro.coQuyenTraCuu());
    hienThiMenu(dangXuatButton, true);
    if (guiChuongTrinhTVMenuItem != null) guiChuongTrinhTVMenuItem.setDisable(!vaiTro.coQuyenQuanLyKhachHang());
    if (xemLogKiemKeTienMatMenuItem != null) xemLogKiemKeTienMatMenuItem.setDisable(!vaiTro.coQuyenThongKe());
}
private void hienThiMenu(Button button, boolean coQuyen) {
    if (button == null) return;
    button.setVisible(coQuyen);
    button.setManaged(coQuyen);
    button.setDisable(!coQuyen);
}
private boolean kiemTraQuyen(String tenChucNang, boolean coQuyen) {
    if (!coQuyen) {
        showAlert(Alert.AlertType.WARNING, "⚠️ KHÔNG CÓ QUYỀN TRUY CẬP", "Bạn không có quyền: " + tenChucNang);
        return false;
    }
    return true;
}
private void addIconMapping(Button button, String defaultIcon, String activeIcon) {
    defaultIcons.put(button, defaultIcon);
    activeIcons.put(button, activeIcon);
}
private void updateMenuStyles() {
    buttonIconMap.forEach((button, icon) -> {
        if (button == null || icon == null) return;
        if(button.isVisible()) {
            String iconPath = (button == activeButton) ? activeIcons.get(button) : defaultIcons.get(button);
            if (iconPath != null) {
                try {
                    URL iconUrl = getClass().getResource(iconPath);
                    if (iconUrl != null) icon.setImage(new Image(iconUrl.toExternalForm()));
                } catch (Exception e) {}
            }
            button.getStyleClass().setAll((button == activeButton) ? "menu-button-active" : "menu-button");
        }
    });
}
private void setActiveButton(Button button) {
    this.activeButton = button;
    updateMenuStyles();
}
private void loadScreen(String fxmlPath, String cssPath) throws IOException {
    // 1. Kiểm tra Cache
    if (screenCache.containsKey(fxmlPath)) {
        Parent root = screenCache.get(fxmlPath);
        currentController = controllerCache.get(fxmlPath);
        
        // Nếu là tab Đặt bàn, đảm bảo controller tham chiếu đúng
        if (currentController instanceof DatBan) {
            this.datBanController = (DatBan) currentController;
            // 🔥 Proactive refresh khi quay lại tab
            Platform.runLater(() -> {
                this.datBanController.loadTableGrids();
                this.datBanController.loadBookingCards();
            });
        }
        
        contentArea.setCenter(root);
        return;
    }

    // 2. Nếu chưa có trong cache thì mới load mới
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    Parent root = loader.load();
    currentController = loader.getController();
    
    // Lưu vào cache
    screenCache.put(fxmlPath, root);
    controllerCache.put(fxmlPath, currentController);

    if (currentController instanceof DashboardController) {
        ((DashboardController) currentController).setMainController(this);
    }
    if (currentController instanceof DatBan) {
        this.datBanController = (DatBan) currentController;
    }
    root.getStylesheets().clear();
    URL globalCssUrl = getClass().getResource("/css/manHinhChinh.css");
    if (globalCssUrl != null) root.getStylesheets().add(globalCssUrl.toExternalForm());
    if (cssPath != null && !cssPath.isEmpty()) {
        URL specificCssUrl = getClass().getResource(cssPath);
        if (specificCssUrl != null) root.getStylesheets().add(specificCssUrl.toExternalForm());
    }
    contentArea.setCenter(root);
}
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
@FXML private void handleManHinhChinh() throws IOException { setActiveButton(manHinhChinhButton); loadScreen("/fxml/Dashboard.fxml", "/css/Dashboard.css"); }
@FXML
private void handleQuanLyDatBan() throws IOException {
    if (currentUser == null || currentUser.getVaiTro() == null || !kiemTraQuyen("Quản lý Đặt bàn", currentUser.getVaiTro().coQuyenQuanLyDatBan())) return;
    setActiveButton(quanLyDatBanButton);
    loadScreen("/fxml/QuanLyDatBan.fxml", "/css/DatBan.css");
}
@FXML private void handleQuanLyThongKe() throws IOException { if (currentUser == null || !kiemTraQuyen("Thống kê & Báo cáo", currentUser.getVaiTro().coQuyenThongKe())) return; setActiveButton(quanLyThongKeButton); loadScreen("/fxml/QuanLyThongKe.fxml", "/css/ThongKe.css"); }
@FXML
private void handleQuanLyThucDon() throws IOException {
    if (currentUser == null || currentUser.getVaiTro() == null || !kiemTraQuyen("Quản lý Thực đơn", currentUser.getVaiTro().coQuyenQuanLyThucDon())) return;
    setActiveButton(quanLyThucDonButton);
    loadScreen("/fxml/QuanLyThucDon.fxml", "/css/ThucDon.css");
}
@FXML
private void handleQuanLyHoaDon() throws IOException {
    if (currentUser == null || currentUser.getVaiTro() == null || !kiemTraQuyen("Quản lý Hóa đơn", currentUser.getVaiTro().coQuyenQuanLyHoaDon())) return;
    setActiveButton(quanLyHoaDonButton);
    loadScreen("/fxml/QuanLyHoaDon.fxml", "/css/HoaDon.css");
}
@FXML
private void handleQuanLyNhanVien() throws IOException {
    if (currentUser == null || currentUser.getVaiTro() == null || !kiemTraQuyen("Quản lý Nhân viên", currentUser.getVaiTro().coQuyenQuanLyNhanVien())) return;
    setActiveButton(quanLyNhanVienButton);
    loadScreen("/fxml/QuanLyNhanVien.fxml", "/css/NhanVien.css");
}
@FXML private void handleQuanLyKhachHang() throws IOException { if (currentUser == null || !kiemTraQuyen("Quản lý Khách hàng", currentUser.getVaiTro().coQuyenQuanLyKhachHang())) return; setActiveButton(quanLyKhachHangButton); loadScreen("/fxml/QuanLyKhachHang.fxml", "/css/KhachHang.css"); }
@FXML private void handleQuanLyKhuyenMai() throws IOException { if (currentUser == null || !kiemTraQuyen("Quản lý Ưu đãi", currentUser.getVaiTro().coQuyenQuanLyUuDai())) return; setActiveButton(quanLyKhuyenMaiButton); loadScreen("/fxml/QuanLyKhuyenMai.fxml", "/css/KhuyenMai.css"); }
@FXML private void handleQuanLyTraCuu() throws IOException { setActiveButton(quanLyTraCuuButton); loadScreen("/fxml/QuanLyTraCuu.fxml", "/css/TraCuu.css"); }
@FXML private void handleDangXuat() {
    setActiveButton(dangXuatButton);
    // Gửi logout tới server
    Client.send(CommandType.LOGOUT, null);
    // Xóa session ở client
    ClientSessionManager.getInstance().logout();
    MainApp.setLoggedInUser(null);
    if (mainApp != null) {
        Stage currentStage = (Stage) dangXuatButton.getScene().getWindow();
        if (currentStage != null) currentStage.close();
        Platform.runLater(() -> { try { mainApp.start(new Stage()); } catch (Exception e) {} });
    }
}
@FXML
private void handleDoiMatKhau() {
    Optional<Pair<String, String>> result = showChangePasswordDialog();
    result.ifPresent(passwords -> {
        String oldPassword = passwords.getKey();
        String newPassword = passwords.getValue();
        Map<String, Object> params = new HashMap<>();
        params.put("username", currentUser.getTenDangNhap());
        params.put("oldPassword", oldPassword);
        params.put("newPassword", newPassword);
        Response res = Client.sendWithParams(CommandType.CHANGE_PASSWORD, params);
        if (res.getStatusCode() == 200) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi mật khẩu thành công!");
        } else {
        showAlert(Alert.AlertType.WARNING, "Thất bại", res.getMessage());
    }
});
}
private Optional<Pair<String, String>> showChangePasswordDialog() {
    Dialog<Pair<String, String>> dialog = new Dialog<>();
    dialog.setTitle("Đổi mật khẩu");
    ButtonType changeButtonType = new ButtonType("Xác nhận", ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);
    GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
    PasswordField oldPassword = new PasswordField();
    PasswordField newPassword = new PasswordField();
    PasswordField confirmPassword = new PasswordField();
    grid.add(new Label("Mật khẩu cũ:"), 0, 0); grid.add(oldPassword, 1, 0);
    grid.add(new Label("Mật khẩu mới:"), 0, 1); grid.add(newPassword, 1, 1);
    grid.add(new Label("Xác nhận MK mới:"), 0, 2); grid.add(confirmPassword, 1, 2);
    dialog.getDialogPane().setContent(grid);
    dialog.setResultConverter(dialogButton -> {
        if (dialogButton == changeButtonType) return new Pair<>(oldPassword.getText(), newPassword.getText());
        return null;
    });
    return dialog.showAndWait();
}
@FXML
private void handleToggleTheme() {
    isDarkMode = !isDarkMode;
    if (topBar == null || sidebar == null) return;
    try {
        if (isDarkMode) {
            topBar.getStyleClass().add(darkStyleClass);
            sidebar.getStyleClass().add(darkStyleClass);
            toggleThemeMenuItem.setText("Chuyển chế độ Sáng");
            if (logoImageView != null) logoImageView.setImage(new Image(getClass().getResourceAsStream("/images/DarkmodeLOGO.jpg")));
        } else {
        topBar.getStyleClass().remove(darkStyleClass);
        sidebar.getStyleClass().remove(darkStyleClass);
        toggleThemeMenuItem.setText("Chuyển chế độ Tối");
        if (logoImageView != null) logoImageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
    }
} catch (Exception e) {}
}
@FXML private void handleThoat() { Platform.exit(); }
// === CHỨC NĂNG GỬI EMAIL (ĐàKHÔI PHỤC) ===
@FXML
private void handleGuiChuongTrinhTV() {
    ObservableList<KhachHang> dsKhachHangCoEmail;
    try {
        Response res = Client.send(CommandType.GET_CUSTOMERS, null);
        if (res.getStatusCode() == 200) {
            List<KhachHang> allKhachHang = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), KhachHang.class);
            List<KhachHang> filteredList = allKhachHang.stream().filter(kh -> kh.getEmail() != null && !kh.getEmail().trim().isEmpty()).collect(Collectors.toList());
            dsKhachHangCoEmail = FXCollections.observableArrayList(filteredList);
        } else {
        return;
    }
} catch (Exception e) { return; }
if (dsKhachHangCoEmail.isEmpty()) { showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không tìm thấy khách hàng nào có địa chỉ email."); return; }
Dialog<Map<String, Object>> dialog = new Dialog<>();
dialog.setTitle("Gửi Email Chương trình Thành viên");
ButtonType sendButtonType = new ButtonType("Gửi đi", ButtonData.OK_DONE);
dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);
GridPane grid = new GridPane();
grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 20, 10, 10));
ListView<KhachHang> recipientListView = new ListView<>(dsKhachHangCoEmail);
recipientListView.setPrefHeight(150);
Map<KhachHang, BooleanProperty> selectionMap = new HashMap<>();
dsKhachHangCoEmail.forEach(kh -> selectionMap.put(kh, new SimpleBooleanProperty(false)));
recipientListView.setCellFactory(lv -> new ListCell<KhachHang>() {
    private final CheckBox checkBox = new CheckBox();
    private final Label label = new Label();
    private final HBox hbox = new HBox(5, checkBox, label);
    { hbox.setAlignment(Pos.CENTER_LEFT); checkBox.setOnAction(event -> { if (getItem() != null) selectionMap.get(getItem()).set(checkBox.isSelected()); }); }
    @Override protected void updateItem(KhachHang kh, boolean empty) { super.updateItem(kh, empty); if (empty || kh == null) { setText(null); setGraphic(null); } else { checkBox.setSelected(selectionMap.get(kh).get()); label.setText(kh.getTenKH() + " (" + kh.getEmail() + ")"); setGraphic(hbox); } }
});
CheckBox selectAllCheckBox = new CheckBox("Chọn tất cả");
selectAllCheckBox.setOnAction(e -> { selectionMap.values().forEach(prop -> prop.set(selectAllCheckBox.isSelected())); recipientListView.refresh(); });
TextField subjectField = new TextField("Thông báo Chương trình Thành viên Mới!");
TextArea contentArea = new TextArea();
contentArea.setPromptText("Ví dụ: Giảm giá 20%...");
grid.add(new Label("Người nhận:"), 0, 0); grid.add(recipientListView, 1, 0); grid.add(selectAllCheckBox, 1, 1);
grid.add(new Label("Tiêu đề:"), 0, 2); grid.add(subjectField, 1, 2);
grid.add(new Label("Nội dung:"), 0, 3); grid.add(contentArea, 1, 3);
dialog.getDialogPane().setContent(grid);
dialog.setResultConverter(dialogButton -> {
    if (dialogButton == sendButtonType) {
        Map<String, Object> res = new HashMap<>();
        res.put("recipients", selectionMap.entrySet().stream().filter(en -> en.getValue().get()).map(en -> en.getKey().getEmail()).collect(Collectors.toList()));
        res.put("subject", subjectField.getText());
        res.put("body", contentArea.getText());
        return res;
    }
    return null;
});
Optional<Map<String, Object>> result = dialog.showAndWait();
if (result.isPresent()) {
    Map<String, Object> emailData = result.get();
    List<String> recipients = (List<String>) emailData.get("recipients");
    String subject = (String) emailData.get("subject");
    String bodyContent = (String) emailData.get("body");
    if (recipients.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Thông báo", "Bạn chưa chọn khách hàng nào để gửi email."); return; }
    if (subject.trim().isEmpty() || bodyContent.trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập Tiêu đề và Nội dung email."); return; }
    // Cấu hình Email (Thay đổi thông tin thật của bạn)
    final String username = "nhahangtuhuu@gmail.com";
    final String password = "rnwm bkli pycf bjcv";
    Properties prop = new Properties();
    prop.put("mail.smtp.host", "smtp.gmail.com");
    prop.put("mail.smtp.port", "587");
    prop.put("mail.smtp.auth", "true");
    prop.put("mail.smtp.starttls.enable", "true");
    // CẤU HÌNH FIX LỖI TLS
    prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
    prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
    Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    });
    new Thread(() -> {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            Address[] bccAddresses = new Address[recipients.size()];
            for (int i = 0; i < recipients.size(); i++) bccAddresses[i] = new InternetAddress(recipients.get(i));
            message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            message.setSubject(subject);
            message.setText("Kính gửi quý khách hàng,\n\n" + bodyContent + "\n\nTrân trọng,\nNhà hàng Tứ Hữu");
            Transport.send(message);
            Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi email đến " + recipients.size() + " khách hàng."));
        } catch (MessagingException e) {
        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi Gửi Email", "Không thể gửi email: " + e.getMessage()));
        e.printStackTrace();
    }
}).start();
}
}
@FXML private void handleXemLogKiemKeTienMat() {
    // 1. Tự động lấy doanh thu thực tế từ DB
    double tienHeThong = getDoanhThuTrongNgay();
    // 2. Tạo giao diện nhập liệu đẹp hơn một chút
    TextInputDialog dialog = new TextInputDialog("");
    dialog.setTitle("Hệ thống Quản lý Tứ Hữu - Kiểm kê tài chính");
    dialog.setHeaderText("BÁO CÁO DOANH THU TRONG NGÀY\n"
    + "Số tiền trên máy tính toán: " + String.format("%,.0f VNĐ", tienHeThong));
    dialog.setContentText("Nhập số tiền mặt thực tế tại quầy:");
    // Thêm icon nếu cần (tùy thuộc vào resources của bạn)
    Optional<String> result = dialog.showAndWait();
    result.ifPresent(tienNhap -> {
        try {
            // Loại bỏ dấu phẩy nếu người dùng nhập kiểu 1,000,000
            String cleanInput = tienNhap.replace(",", "").replace(".", "");
            double tienThucTe = Double.parseDouble(cleanInput);
            double chenhLech = tienThucTe - tienHeThong;
            // 3. Ghi log vào file text (Không sửa DB để giữ nguyên báo cáo)
            saveKiemKeLog(tienHeThong, tienThucTe);
            // 4. Hiển thị thông báo kết quả chi tiết
            String tinhTrang = (chenhLech == 0) ? "Khớp hoàn toàn" : (chenhLech > 0 ? "Dư tiền" : "Thiếu tiền");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kết quả kiểm kê");
            alert.setHeaderText("Tình trạng: " + tinhTrang);
            alert.setContentText(String.format(
            "Hệ thống: %,.0f VNĐ\nThực tế: %,.0f VNĐ\nChênh lệch: %,.0f VNĐ\n\nĐã lưu vào nhật ký hệ thống.",
            tienHeThong, tienThucTe, chenhLech
            ));
            alert.showAndWait();
        } catch (NumberFormatException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng chỉ nhập số tiền (ví dụ: 500000).");
    }
});
}
// === CHỨC NĂNG MỞ PDF (ĐàNÂNG CẤP - CHẠY ĐƯỢC TRÊN MỌI MÁY) ===
private void saveKiemKeLog(double heThong, double thucTe) {
    java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    String thoiGian = dtf.format(java.time.LocalDateTime.now());
    // Xử lý an toàn cho mọi nhân viên
    String tenNV = "Nhân viên trực ca";
    try {
        if (this.currentNhanVien != null && this.currentNhanVien.getHoTen() != null) {
            tenNV = this.currentNhanVien.getHoTen();
        }
    } catch (Exception e) {
    // Nếu có lỗi khi truy cập đối tượng nhân viên, vẫn giữ tên mặc định
}
String logLine = String.format("[%s] NV: %-20s | Máy: %15s | Thực: %15s | Lệch: %15s",
thoiGian,
tenNV,
String.format("%,.0f", heThong),
String.format("%,.0f", thucTe),
String.format("%,.0f", thucTe - heThong));
try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter("nhat_ky_kiem_ke.txt", true))) {
    writer.write(logLine);
    writer.newLine();
} catch (java.io.IOException e) {
System.err.println("Lỗi ghi nhật ký: " + e.getMessage());
}
}
@FXML
private void handleHuongDan() {
    try {
        Stage stage = new Stage();
        stage.setTitle("Hướng Dẫn Sử Dụng - Future Vision");
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        // Load từ resources
        URL url = getClass().getResource("/html/huong_dan.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        }
        Scene scene = new Scene(webView);
        stage.setScene(scene);
        stage.setMaximized(true); // Mở full màn hình cho đẳng cấp
        stage.show();
    } catch (Exception e) {
    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở tài liệu hướng dẫn.");
}
}
@FXML
private void handleGioiThieu() {
    try {
        Stage stage = new Stage();
        stage.setTitle("Hệ Thống Tứ Hữu - Future Vision 2025");
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        // Nạp file từ resources (đã sửa đường dẫn theo ý bạn)
        URL url = getClass().getResource("/html/gioi_thieu.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
        webEngine.loadContent("<h1 style='color:white; background:#0f172a; height:100vh; display:flex; align-items:center; justify-content:center;'>Lỗi: Không tìm thấy file HTML!</h1>");
    }
    Scene scene = new Scene(webView);
    stage.setScene(scene);
    // --- ĐẲNG CẤP FULL MÀN HÌNH TẠI ĐÂY ---
    stage.setMaximized(true); // Mở bung toàn màn hình ngay lập tức
    stage.setFullScreen(false); // Không dùng FullScreen để vẫn thấy thanh taskbar (chuyên nghiệp hơn)
    stage.show();
} catch (Exception e) {
e.printStackTrace();
}
}
private void moFilePDF(String resourcePath) {
    if (!Desktop.isDesktopSupported()) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Hệ thống không hỗ trợ mở file PDF.");
        return;
    }
    try {
        // 1. Kiểm tra resource
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy file trong resources: " + resourcePath +
            "\n\nHãy thử Clean & Rebuild Project.");
            return;
        }
        // 2. Tạo file tạm thời để mở
        String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        // Thêm timestamp để tránh trùng tên nếu mở nhiều lần
        File tempFile = File.createTempFile("NhaHangTuHuu_" + System.currentTimeMillis() + "_", fileName);
        tempFile.deleteOnExit(); // Tự xóa khi tắt chương trình
        // 3. Copy dữ liệu ra file tạm
        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        // 4. Mở file
        Desktop.getDesktop().open(tempFile);
    } catch (Exception e) {
    e.printStackTrace();
    showAlert(Alert.AlertType.ERROR, "Lỗi mở file", "Chi tiết lỗi: " + e.getMessage());
}
}
}