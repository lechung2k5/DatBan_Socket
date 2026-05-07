package ui;
import network.Client;
import network.CommandType;
import network.Response;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import java.io.IOException;
public class QuenMatKhau {
    @FXML private TextField txtMaNhanVien;
    @FXML private PasswordField pfMatKhauMoi;
    @FXML private PasswordField pfXacNhanMatKhau;
    @FXML private Button btnXacNhanMaNV;
    @FXML private Button btnDoiMatKhau;
    @FXML private HBox hboxMatKhauMoi;
    @FXML private HBox hboxXacNhanMK;
    private MainApp mainApp;
    // private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO(); // Removed
    @FXML
    private void initialize() {
        // Ban đầu ẩn các trường mật khẩu và nút Đổi MK
        setPasswordFieldVisibility(false);
        // Đợi scene được tạo xong
        txtMaNhanVien.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setupKeyboardShortcuts();
            }
        });
    }
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
    /**
    * 🔥 HÀM MỚI: Thiết lập phím tắt
    */
    private void setupKeyboardShortcuts() {
        txtMaNhanVien.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Enter hoặc Ctrl+S: Xác nhận (bước 1) hoặc Đổi mật khẩu (bước 2)
            if (event.getCode() == KeyCode.ENTER ||
            (event.isControlDown() && event.getCode() == KeyCode.S)) {
                if (btnXacNhanMaNV.isVisible()) {
                    // Bước 1: Xác nhận mã nhân viên
                    handleXacNhan(null);
                } else if (btnDoiMatKhau.isVisible()) {
                // Bước 2: Đổi mật khẩu
                handleDoiMatKhau(null);
            }
            event.consume();
        }
        // Esc: Quay lại đăng nhập
        else if (event.getCode() == KeyCode.ESCAPE) {
            try {
                handleQuayLai(null);
            } catch (IOException e) {
            e.printStackTrace();
        }
        event.consume();
    }
    // Ctrl+F: Focus vào ô mã nhân viên (nếu chưa bị khóa)
    else if (event.isControlDown() && event.getCode() == KeyCode.F) {
        if (txtMaNhanVien.isEditable()) {
            txtMaNhanVien.requestFocus();
            txtMaNhanVien.selectAll();
        }
        event.consume();
    }
    // F1: Hiển thị trợ giúp phím tắt
    else if (event.getCode() == KeyCode.F1) {
        showKeyboardShortcutsHelp();
        event.consume();
    }
    // Ctrl+R: Reset form (về bước 1)
    else if (event.isControlDown() && event.getCode() == KeyCode.R) {
        resetForm();
        event.consume();
    }
});
}
@FXML
private void handleXacNhan(ActionEvent event) {
    String maNhanVien = txtMaNhanVien.getText().trim();
    if (maNhanVien.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập mã nhân viên.");
        txtMaNhanVien.requestFocus();
        return;
    }
    Response res = Client.sendWithParams(CommandType.GET_EMPLOYEES, null); // Use GET_EMPLOYEES to check for now, or add FIND_BY_ID
    boolean taiKhoanTonTai = false;
    if (res.getStatusCode() == 200) {
        java.util.List<entity.NhanVien> list = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), entity.NhanVien.class);
        taiKhoanTonTai = list.stream().anyMatch(e -> e.getMaNV().equals(maNhanVien));
    }
    if (taiKhoanTonTai) {
        showAlert(Alert.AlertType.INFORMATION, "Tìm thấy tài khoản",
        "Tìm thấy tài khoản '" + maNhanVien + "'. Vui lòng nhập mật khẩu mới.");
        setPasswordFieldVisibility(true);
        txtMaNhanVien.setEditable(false);
        pfMatKhauMoi.requestFocus();
    } else {
    showAlert(Alert.AlertType.ERROR, "Không tìm thấy",
    "Không tìm thấy tài khoản nào ứng với mã nhân viên '" + maNhanVien + "'.");
    txtMaNhanVien.requestFocus();
    txtMaNhanVien.selectAll();
}
}
@FXML
private void handleDoiMatKhau(ActionEvent event) {
    String maNhanVien = txtMaNhanVien.getText().trim();
    String matKhauMoi = pfMatKhauMoi.getText();
    String xacNhanMK = pfXacNhanMatKhau.getText();
    // Validate mật khẩu mới
    if (matKhauMoi.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập mật khẩu mới.");
        pfMatKhauMoi.requestFocus();
        return;
    }
    if (xacNhanMK.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng xác nhận mật khẩu mới.");
        pfXacNhanMatKhau.requestFocus();
        return;
    }
    if (!matKhauMoi.equals(xacNhanMK)) {
        showAlert(Alert.AlertType.WARNING, "Lỗi Nhập Liệu", "Mật khẩu xác nhận không khớp.");
        pfXacNhanMatKhau.requestFocus();
        pfXacNhanMatKhau.selectAll();
        return;
    }
    // Gọi API để cập nhật mật khẩu
    Response res = Client.sendWithParams(CommandType.FORGOT_PASSWORD_UPDATE, java.util.Map.of("username", maNhanVien, "newPassword", matKhauMoi));
    if (res.getStatusCode() == 200) {
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi mật khẩu thành công!");
        if (mainApp != null) {
            mainApp.gotoLogin();
        }
    } else {
    showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi: " + res.getMessage());
}
}
@FXML
private void handleQuayLai(ActionEvent event) throws IOException {
    if (mainApp != null) {
        resetForm();
        mainApp.gotoLogin();
    }
}
/**
* 🔥 HÀM MỚI: Reset form về trạng thái ban đầu
*/
private void resetForm() {
    setPasswordFieldVisibility(false);
    txtMaNhanVien.clear();
    txtMaNhanVien.setEditable(true);
    pfMatKhauMoi.clear();
    pfXacNhanMatKhau.clear();
    txtMaNhanVien.requestFocus();
}
/**
* 🔥 HÀM MỚI: Hiển thị hướng dẫn phím tắt
*/
private void showKeyboardShortcutsHelp() {
    String helpText = """
    ⌨️ PHÍM TẮT QUÊN MẬT KHẨU:
    Enter hoặc Ctrl+S  →  Xác nhận / Đổi mật khẩu
    Esc                →  Quay lại đăng nhập
    Ctrl+R             →  Reset form (về bước 1)
    Ctrl+F             →  Focus vào ô mã nhân viên
    F1                 →  Hiển thị trợ giúp này
    💡 Gợi ý: Nhấn Tab để di chuyển giữa các ô nhập liệu
    """;
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Hướng dẫn phím tắt");
    alert.setHeaderText("📖 Phím tắt có sẵn");
    alert.setContentText(helpText);
    alert.showAndWait();
}
/**
* Ẩn/hiện các thành phần liên quan đến đổi mật khẩu
*/
private void setPasswordFieldVisibility(boolean visible) {
    hboxMatKhauMoi.setVisible(visible);
    hboxMatKhauMoi.setManaged(visible);
    hboxXacNhanMK.setVisible(visible);
    hboxXacNhanMK.setManaged(visible);
    btnDoiMatKhau.setVisible(visible);
    btnDoiMatKhau.setManaged(visible);
    btnXacNhanMaNV.setVisible(!visible);
    btnXacNhanMaNV.setManaged(!visible);
}
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
}