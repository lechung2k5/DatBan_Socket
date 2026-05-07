package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.NhanVien;
import entity.VaiTro;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
// import java.sql.Time; // ð¥ Bá»
import java.time.LocalDate;
// import java.time.LocalTime; // ð¥ Bá»
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern; // Import Pattern
public class NhanVienUI {
    //<editor-fold desc="FXML Declarations">
    @FXML private TextField txtMaNV;
    @FXML private TextField txtHoTen;
    @FXML private TextField txtSDT;
    @FXML private ComboBox<String> cbxChucVu;
    @FXML private DatePicker dpNgaySinh;
    @FXML private ComboBox<String> cbxCaLam; // Sáº½ dùng cho ca YÃU THÃCH
    @FXML private PasswordField pfMatKhau;
    @FXML private ComboBox<String> cbxTrangThai;
    @FXML private TextField txtTimKiem;
    @FXML private TableView<NhanVien> tblNhanVien;
    @FXML private TableColumn<NhanVien, String> colMaNV;
    @FXML private TableColumn<NhanVien, String> colHoTen;
    @FXML private TableColumn<NhanVien, String> colSDT;
    @FXML private TableColumn<NhanVien, String> colChucVu;
    @FXML private TableColumn<NhanVien, String> colNgaySinh;
    @FXML private TableColumn<NhanVien, String> colCaLam; // (FXML id váº«n lÃ  colCaLam)
    @FXML private TableColumn<NhanVien, String> colTrangThai;
    // Khai báo Äúng 6 nút
    @FXML private Button btnThem; // Nút "Thêm" (Lưu má»i)
    @FXML private Button btnSua;  // Nút "Sá»­a" (Cáº­p nháº­t ngay)
    @FXML private Button btnXoa;
    @FXML private Button btnLuu;  // Nút "Lưu" (Không dùng)
    @FXML private Button btnHuy;
    @FXML private Button btnXoaRong; // Nút "Xóa rá»ng" (Báº¯t Äáº§u thêm)
    //</editor-fold>
    private ObservableList<NhanVien> nhanVienList;
    // DAO removed
    private NhanVien currentSelectedNhanVien = null;
    // Trạng thái giao diá»n: VIEWING (Äang xem/sá»­a) hoáº·c ADDING (Äang thêm má»i)
    private enum EditState { VIEWING, ADDING }
    private EditState currentState = EditState.VIEWING;
    @FXML
    private void initialize() {
        nhanVienList = FXCollections.observableArrayList();
        // Cáº¥u hình cá»t
        colMaNV.setCellValueFactory(new PropertyValueFactory<>("maNV"));
        colHoTen.setCellValueFactory(new PropertyValueFactory<>("hoTen"));
        colSDT.setCellValueFactory(new PropertyValueFactory<>("sdt"));
        colChucVu.setCellValueFactory(new PropertyValueFactory<>("chucVu"));
        colNgaySinh.setCellValueFactory(new PropertyValueFactory<>("ngaySinh"));
        // ð¥ THAY Äá»I QUAN TRá»NG: Bind cá»t vÃ o "caLamYeuThich"
        colCaLam.setCellValueFactory(new PropertyValueFactory<>("caLamYeuThich"));
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("trangThai"));
        // Cáº¥u hình ComboBox
        ObservableList<String> vaiTroStrings = FXCollections.observableArrayList();
        for (VaiTro vaiTro : VaiTro.values()) { vaiTroStrings.add(vaiTro.getTenVaiTro()); }
        cbxChucVu.setItems(vaiTroStrings);
        cbxTrangThai.setItems(FXCollections.observableArrayList("Äang lÃ m", "Nghá» viá»c"));
        // Dùng danh sách code cứng cho Ca lÃ m YÃU THÃCH
        cbxCaLam.setItems(FXCollections.observableArrayList("Sáng", "Chiá»u", "Tá»i", "Nguyên ngÃ y"));
        cbxCaLam.setPromptText("Chá»n ca yêu thÃ­ch...");
        // Cáº¥u hình tìm kiếm
        FilteredList<NhanVien> filteredList = new FilteredList<>(nhanVienList, p -> true);
        txtTimKiem.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(nv -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerCaseFilter = newVal.toLowerCase();
                return nv.getMaNV().toLowerCase().contains(lowerCaseFilter) ||
                nv.getHoTen().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<NhanVien> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tblNhanVien.comparatorProperty());
        tblNhanVien.setItems(sortedList);
        // Listener cho bảng: Khi chá»n dòng -> VIEWING, hiá»n thá» data, má» khóa form
        tblNhanVien.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldSelection, newSelection) -> {
            if (currentState == EditState.ADDING) return; // Không lÃ m gì nếu Äang thêm
            currentSelectedNhanVien = newSelection;
            if (newSelection != null) {
                showNhanVienDetails(newSelection);
                setFormEditable(true); // Má» khóa form ngay khi chá»n
            } else {
            clearForm();
            setFormEditable(false); // Khóa form nếu không chá»n
        }
        updateUIState(EditState.VIEWING); // Luôn quay vá» VIEWING khi chá»n dòng
    }
    );
    loadNhanVienData(); // Tải dữ liá»u láº§n Äáº§u
}
//<editor-fold desc="State Management & UI Control">
private void setFormEditable(boolean editable) {
    txtMaNV.setEditable(false);
    txtHoTen.setEditable(editable);
    txtSDT.setEditable(editable);
    pfMatKhau.setEditable(editable);
    cbxChucVu.setDisable(!editable);
    dpNgaySinh.setDisable(!editable);
    cbxCaLam.setDisable(!editable); // ComboBox Äã Äưá»£c má»
    cbxTrangThai.setDisable(!editable);
}
private void updateUIState(EditState state) {
    this.currentState = state;
    boolean rowIsSelected = (currentSelectedNhanVien != null);
    if (state == EditState.ADDING) {
        // Khi Äang thêm má»i (sau khi báº¥m Xóa rá»ng)
        setFormEditable(true);      // Má» khóa form
        btnXoaRong.setDisable(true);  // Táº¯t "Xóa rá»ng"
        btnThem.setDisable(false);     // Báº­t "Thêm" (Lưu má»i)
        btnSua.setDisable(true);
        btnLuu.setDisable(true);      // Táº¯t nút "Lưu"
        btnXoa.setDisable(true);
        btnHuy.setDisable(false);
        tblNhanVien.setDisable(true);  // Khóa bảng
    } else { // VIEWING state (Äang xem hoáº·c Äang sá»­a trên dòng Äã chá»n)
    setFormEditable(rowIsSelected); // Form sá»­a Äưá»£c nếu có dòng Äưá»£c chá»n
    btnXoaRong.setDisable(false); // Báº­t "Xóa rá»ng"
    btnThem.setDisable(true);      // Táº¯t "Thêm" (Lưu má»i)
    btnSua.setDisable(!rowIsSelected); // Báº­t Sá»­a/Xóa/Hủy nếu có dòng Äưá»£c chá»n
    btnLuu.setDisable(true);      // Táº¯t nút "Lưu"
    btnXoa.setDisable(!rowIsSelected);
    btnHuy.setDisable(!rowIsSelected); // Hủy chá» báº­t khi có dòng Äang chá»n (Äá» revert)
    tblNhanVien.setDisable(false); // Má» khóa bảng
}
}
//</editor-fold>
//<editor-fold desc="Event Handlers">
@FXML
private void handleXoaRong(ActionEvent event) { // Nút "Xóa rá»ng" -> Báº¯t Äáº§u thêm
    tblNhanVien.getSelectionModel().clearSelection();
    clearForm();
    txtMaNV.setText(generateNewMaNV());
    pfMatKhau.setPromptText("Báº¯t buá»c nháº­p");
    updateUIState(EditState.ADDING); // Chuyá»n sang trạng thái ADDING
    txtHoTen.requestFocus();
}
@FXML
private void handleThem(ActionEvent event) { // Nút "Thêm" -> Lưu nhân viên má»i
    if (currentState != EditState.ADDING || !validateInput()) return;
    NhanVien nv = createNhanVienFromForm();
    Response res = Client.sendWithParams(CommandType.UPDATE_EMPLOYEE, Map.of("employee", nv));
    if (res.getStatusCode() == 200) {
        showInfoAlert("ThÃ nh công", "Äã thêm nhân viên má»i!");
        loadNhanVienData();
    } else {
    showErrorAlert("Tháº¥t bại", "Thêm má»i không thÃ nh công: " + res.getMessage());
}
}
@FXML
private void handleSua(ActionEvent event) { // Nút "Sá»­a" -> Cáº­p nháº­t ngay dòng Äang chá»n
    if (currentSelectedNhanVien == null || currentState != EditState.VIEWING || !validateInput()) return;
    NhanVien nv = createNhanVienFromForm();
    Response res = Client.sendWithParams(CommandType.UPDATE_EMPLOYEE, Map.of("employee", nv));
    if (res.getStatusCode() == 200) {
        showInfoAlert("ThÃ nh công", "Äã cáº­p nháº­t thông tin nhân viên!");
        loadNhanVienData();
    } else {
    showErrorAlert("Tháº¥t bại", "Cáº­p nháº­t không thÃ nh công: " + res.getMessage());
}
}
@FXML
private void handleLuu(ActionEvent event) { // Nút "Lưu" -> Không lÃ m gì cả
    System.out.println("Nút Lưu không có chức nÄng trong quy trình nÃ y.");
}
@FXML
private void handleXoa(ActionEvent event) { // Nút "Xóa"
    if (currentSelectedNhanVien == null || currentState != EditState.VIEWING) return;
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nháº­n xóa");
    confirm.setContentText("Bạn có cháº¯c cháº¯n muá»n xóa nhân viên '" + currentSelectedNhanVien.getHoTen() + "'?");
    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        currentSelectedNhanVien.setTrangThai("Nghá» viá»c");
        Response res = Client.sendWithParams(CommandType.UPDATE_EMPLOYEE, Map.of("employee", currentSelectedNhanVien));
        if (res.getStatusCode() == 200) {
            showInfoAlert("ThÃ nh công", "Äã xóa (ngừng hoạt Äá»ng) nhân viên!");
            loadNhanVienData();
        } else {
        showErrorAlert("Tháº¥t bại", "Xóa nhân viên không thÃ nh công: " + res.getMessage());
    }
}
}
@FXML
private void handleHuy(ActionEvent event) { // Nút "Hủy"
    if (currentState == EditState.ADDING) {
        loadNhanVienData(); // Quay vá» trạng thái xem ban Äáº§u
    } else if (currentSelectedNhanVien != null) {
    showNhanVienDetails(currentSelectedNhanVien); // Khôi phục dữ liá»u gá»c của dòng Äang chá»n
}
}
//</border-fold>
//<editor-fold desc="Data & Utility Methods">
private void loadNhanVienData() {
    int selectedIndex = tblNhanVien.getSelectionModel().getSelectedIndex();
    NhanVien selectedNVBeforeLoad = currentSelectedNhanVien;
    try {
        Response res = Client.send(CommandType.GET_EMPLOYEES, null);
        if (res.getStatusCode() == 200) {
            List<NhanVien> listFromDB = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), NhanVien.class);
            nhanVienList.setAll(listFromDB);
        }
        if (selectedNVBeforeLoad != null) {
            for (int i = 0; i < nhanVienList.size(); i++) {
                if (nhanVienList.get(i).getMaNV().equals(selectedNVBeforeLoad.getMaNV())) {
                    tblNhanVien.getSelectionModel().select(i);
                    break;
                }
            }
        }
    } catch (Exception e) {
    showErrorAlert("Lá»i tải dữ liá»u", e.getMessage());
    currentSelectedNhanVien = null;
}
currentSelectedNhanVien = tblNhanVien.getSelectionModel().getSelectedItem();
if (currentSelectedNhanVien == null) {
    clearForm();
}
updateUIState(EditState.VIEWING);
}
private void showNhanVienDetails(NhanVien nhanVien) {
    if (nhanVien == null) {
        clearForm();
        return;
    }
    txtMaNV.setText(nhanVien.getMaNV());
    txtHoTen.setText(nhanVien.getHoTen());
    txtSDT.setText(nhanVien.getSdt());
    cbxChucVu.setValue(nhanVien.getChucVu());
    cbxTrangThai.setValue(nhanVien.getTrangThai());
    pfMatKhau.setText("");
    pfMatKhau.setPromptText("Äá» trá»ng nếu không muá»n Äá»i máº­t kháº©u");
    // Hiá»n thá» ca lÃ m YÃU THÃCH
    cbxCaLam.setValue(nhanVien.getCaLamYeuThich());
    try {
        dpNgaySinh.setValue(LocalDate.parse(nhanVien.getNgaySinh(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    } catch (Exception e) {
    dpNgaySinh.setValue(null);
}
}
private NhanVien createNhanVienFromForm() {
    NhanVien nv;
    if (currentState == EditState.VIEWING && currentSelectedNhanVien != null) {
        nv = currentSelectedNhanVien; // Dùng trực tiếp Äá»i tưá»£ng Äã chá»n
    }
    else {
        nv = new NhanVien();
        nv.setMaNV(txtMaNV.getText().trim());
    }
    nv.setHoTen(txtHoTen.getText().trim());
    nv.setSdt(txtSDT.getText().trim());
    nv.setChucVu(cbxChucVu.getValue());
    if (dpNgaySinh.getValue() != null) {
        nv.setNgaySinh(dpNgaySinh.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    } else {
    nv.setNgaySinh(null);
}
nv.setTrangThai(cbxTrangThai.getValue());
// Láº¥y ca lÃ m yêu thÃ­ch từ ComboBox
nv.setCaLamYeuThich(cbxCaLam.getValue());
String matKhau = pfMatKhau.getText();
if (!matKhau.isEmpty()) {
    nv.setMatKhau(matKhau);
} else if (currentState == EditState.ADDING) {
nv.setMatKhau(""); // Báº¯t buá»c máº­t kháº©u khi thêm má»i (logic validate)
} else if (currentSelectedNhanVien != null) {
// Khi Sá»­a, nếu máº­t kháº©u trá»ng -> giữ máº­t kháº©u cũ (láº¥y từ DAO)
nv.setMatKhau(currentSelectedNhanVien.getMatKhau());
}
return nv;
}
private String generateNewMaNV() {
    if (nhanVienList.isEmpty()) return "NV001";
    String maCuoi = nhanVienList.get(nhanVienList.size() - 1).getMaNV();
    try {
        int soMoi = Integer.parseInt(maCuoi.substring(2)) + 1;
        return String.format("NV%03d", soMoi);
    } catch (Exception e) { return "NV" + (nhanVienList.size() + 1); }
}
private boolean validateInput() {
    if (txtHoTen.getText().trim().isEmpty()) {
        showErrorAlert("Lá»i", "Há» tên không Äưá»£c Äá» trá»ng.");
        txtHoTen.requestFocus();
        return false;
    }
    if (!txtSDT.getText().trim().matches("^0\\d{9}$")) {
        showErrorAlert("Lá»i", "Sá» Äiá»n thoại phải báº¯t Äáº§u báº±ng 0 vÃ  có 10 chữ sá».");
        txtSDT.requestFocus();
        return false;
    }
    if (cbxChucVu.getValue() == null) {
        showErrorAlert("Lá»i", "Vui lòng chá»n chức vụ.");
        cbxChucVu.requestFocus();
        return false;
    }
    if (dpNgaySinh.getValue() == null) {
        showErrorAlert("Lá»i", "Vui lòng chá»n ngÃ y sinh.");
        dpNgaySinh.requestFocus();
        return false;
    }
    if (currentState == EditState.ADDING && pfMatKhau.getText().isEmpty()) {
        showErrorAlert("Lá»i", "Máº­t kháº©u lÃ  báº¯t buá»c khi thêm má»i.");
        pfMatKhau.requestFocus();
        return false;
    }
    if (cbxTrangThai.getValue() == null) {
        showErrorAlert("Lá»i", "Vui lòng chá»n trạng thái.");
        cbxTrangThai.requestFocus();
        return false;
    }
    // Không cáº§n validate cbxCaLam (vì có thá» Äá» trá»ng - không yêu thích)
    return true;
}
private void clearForm() {
    txtMaNV.setText("");
    txtHoTen.setText("");
    txtSDT.setText("");
    cbxChucVu.setValue(null);
    dpNgaySinh.setValue(null);
    cbxCaLam.setValue(null); // Xóa ca yêu thích
    cbxCaLam.setPromptText("Chọn ca yêu thích..."); // Đặt lại prompt
    pfMatKhau.setText("");
    pfMatKhau.setPromptText("");
    cbxTrangThai.setValue(null);
}
private void showErrorAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
private void showInfoAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
//</editor-fold>
}