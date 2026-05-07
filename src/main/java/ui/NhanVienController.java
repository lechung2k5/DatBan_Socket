package ui;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.event.ActionEvent;
import java.io.IOException;
public class NhanVienController {
    @FXML private BorderPane contentPane;
    @FXML private Button btnDanhSach;
    @FXML private Button btnPhanCa;
    @FXML
    private void initialize() throws IOException {
        handleChuyenSangDanhSach(); // Load DanhSachNV.fxml as default view
    }
    @FXML
    private void handleChuyenSangDanhSach() throws IOException {
        loadScreen("/fxml/DanhSachNV.fxml");
        btnDanhSach.getStyleClass().removeAll("tab-button-active");
        btnPhanCa.getStyleClass().removeAll("tab-button-active");
        btnDanhSach.getStyleClass().add("tab-button-active");
        System.out.println("Danh sách NV active: " + btnDanhSach.getStyleClass());
    }
    @FXML
    private void handleChuyenSangPhanCa() throws IOException {
        loadScreen("/fxml/PhanCaTruc.fxml");
        btnDanhSach.getStyleClass().removeAll("tab-button-active");
        btnPhanCa.getStyleClass().removeAll("tab-button-active");
        btnPhanCa.getStyleClass().add("tab-button-active");
        System.out.println("Phân ca trực active: " + btnPhanCa.getStyleClass());
    }
    private void loadScreen(String fxmlPath) throws IOException {
        // Đảm bảo đường dẫn FXML đúng (nên nằm trong resources/fxml/)
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        contentPane.setCenter(root);
    }
}