package ui;
import network.Client;
import utils.ClientSessionManager;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.VaiTro;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import network.CommandType;
import utils.JsonUtil;
import network.Response;
import java.util.Map;
/**
* DangNhap Controller - Phiên bản Distributed
* Gọi Server qua Client thay vì gọi trực tiếp DAO.
*/
public class DangNhap {
    @FXML
    private TextField txtMaNhanVien;
    @FXML
    private PasswordField txtPassword;
    private MainApp mainApp;
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
    @FXML
    private void initialize() {
        txtMaNhanVien.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setupKeyboardShortcuts();
            }
        });
    }
    private void setupKeyboardShortcuts() {
        txtMaNhanVien.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER || (event.isControlDown() && event.getCode() == KeyCode.S)) {
                handleDangNhapButtonAction(null);
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
            clearForm();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
        txtMaNhanVien.requestFocus();
        txtMaNhanVien.selectAll();
        event.consume();
    } else if (event.getCode() == KeyCode.F1) {
    showKeyboardShortcutsHelp();
    event.consume();
}
});
}
@FXML
void handleDangNhapButtonAction(ActionEvent event) {
    String maNhanVien = txtMaNhanVien.getText().trim();
    String password = txtPassword.getText();
    if (maNhanVien.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập Mã nhân viên / Tên đăng nhập.");
        txtMaNhanVien.requestFocus();
        return;
    }
    if (password.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập Mật khẩu.");
        txtPassword.requestFocus();
        return;
    }
    // --- GỌI API SERVER ---
    new Thread(() -> {
        try {
            Response res = Client.sendWithParams(CommandType.LOGIN, Map.of(
            "username", maNhanVien,
            "password", password
            ));
            Platform.runLater(() -> {
                if (res.getStatusCode() == 200) {
                    Map<String, Object> data = (Map<String, Object>) res.getData();
                    // 1. Lưu session token vào ClientSessionManager
                    String token = (String) data.get("token");
                    NhanVien employee = JsonUtil.convertValue(data.get("employee"), NhanVien.class);
                    ClientSessionManager.getInstance().login(token, employee);
                    // 2. Tạo TaiKhoan entity để tương thích với UI hiện tại
                    TaiKhoan tk = JsonUtil.convertValue(data.get("account"), TaiKhoan.class);
                    MainApp.setLoggedInUser(tk);
                    System.out.println("[GUI] Đăng nhập thành công. Chào " + employee.getHoTen());
                    if (mainApp != null) {
                        mainApp.gotoMainScreen();
                    }
                } else {
                showErrorAlert("Đăng nhập thất bại", res.getMessage());
                txtPassword.clear();
                txtPassword.requestFocus();
            }
        });
    } catch (Exception e) {
    Platform.runLater(() -> showErrorAlert("Lỗi kết nối", "Không thể kết nối đến máy chủ: " + e.getMessage()));
}
}).start();
}
@FXML
private void handleQuenMatKhau(ActionEvent event) {
    if (mainApp != null) {
        mainApp.gotoQuenMatKhau();
    }
}
private void clearForm() {
    txtMaNhanVien.clear();
    txtPassword.clear();
    txtMaNhanVien.requestFocus();
}
private void showKeyboardShortcutsHelp() {
    String helpText = """
    ⌨️ PHÍM TẮT ĐĂNG NHẬP:
    \s
    Enter hoặc Ctrl+S  →  Đăng nhập
    Esc                →  Xóa toàn bộ form
    Ctrl+F             →  Focus vào ô mã nhân viên
    F1                 →  Hiển thị trợ giúp này
    """;
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Hướng dẫn phím tắt");
    alert.setHeaderText("📖 Phm tt c sn");
    alert.setContentText(helpText);
    alert.showAndWait();
}
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
private void showErrorAlert(String title, String content) {
    showAlert(Alert.AlertType.ERROR, title, content);
}
}