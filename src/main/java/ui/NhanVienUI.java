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
import javafx.application.Platform;
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
    @FXML private ComboBox<String> cbxCaLam; // Sáº½ dùng cho ca YíU THíCH
    @FXML private PasswordField pfMatKhau;
    @FXML private ComboBox<String> cbxTrangThai;
    @FXML private TextField txtTimKiem;
    @FXML private TableView<NhanVien> tblNhanVien;
    @FXML private TableColumn<NhanVien, String> colMaNV;
    @FXML private TableColumn<NhanVien, String> colHoTen;
    @FXML private TableColumn<NhanVien, String> colSDT;
    @FXML private TableColumn<NhanVien, String> colChucVu;
    @FXML private TableColumn<NhanVien, String> colNgaySinh;
    @FXML private TableColumn<NhanVien, String> colCaLam; // (FXML id váº«n là colCaLam)
    @FXML private TableColumn<NhanVien, String> colTrangThai;
    // Khai báo Äúng 6 nút
    @FXML private Button btnThem; // Nút "Thêm" (Lưu má»i)
    @FXML private Button btnSua;  // Nút "Sửa" (Cập nhật ngay)
    @FXML private Button btnXoa;
    @FXML private Button btnLuu;  // Nút "Lưu" (Không dùng)
    @FXML private Button btnHuy;
    @FXML private Button btnXoaRong; // Nút "Xóa rá»ng" (Bắt Äầu thêm)
    //</editor-fold>
    private ObservableList<NhanVien> nhanVienList;
    // DAO removed
    private NhanVien currentSelectedNhanVien = null;
    // Trạng thái giao diá»n: VIEWING (Äang xem/sửa) hoặc ADDING (Äang thêm má»i)
    private enum EditState { VIEWING, ADDING }
    private EditState currentState = EditState.VIEWING;
    @FXML
    private void initialize() {
        nhanVienList = FXCollections.observableArrayList();
        // Cấu hình cá»t
        colMaNV.setCellValueFactory(new PropertyValueFactory<>("maNV"));
        colHoTen.setCellValueFactory(new PropertyValueFactory<>("hoTen"));
        colSDT.setCellValueFactory(new PropertyValueFactory<>("sdt"));
        colChucVu.setCellValueFactory(new PropertyValueFactory<>("chucVu"));
        colNgaySinh.setCellValueFactory(new PropertyValueFactory<>("ngaySinh"));
        // ð¥ THAY Äá»I QUAN TRá»NG: Bind cá»t vào "caLamYeuThich"
        colCaLam.setCellValueFactory(new PropertyValueFactory<>("caLamYeuThich"));
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("trangThai"));
        // Cấu hình ComboBox
        ObservableList<String> vaiTroStrings = FXCollections.observableArrayList();
        for (VaiTro vaiTro : VaiTro.values()) { vaiTroStrings.add(vaiTro.getTenVaiTro()); }
        cbxChucVu.setItems(vaiTroStrings);
        cbxTrangThai.setItems(FXCollections.observableArrayList("Äang làm", "Nghá» viá»c"));
        // Dùng danh sách code cứng cho Ca làm YíU THíCH
        cbxCaLam.setItems(FXCollections.observableArrayList("Sáng", "Chiá»u", "Tá»i", "Nguyên ngày"));
        cbxCaLam.setPromptText("Chá»n ca yêu thích...");
        // Cấu hình tìm kiếm
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

        // 🔥 LẮNG NGHE REAL-TIME (Tải lại danh sách nhân viên khi có thay đổi)
        network.RealTimeClient.getInstance().addListener(event -> {
            if (event.getType() == CommandType.UPDATE_EMPLOYEE) {
                Platform.runLater(this::loadNhanVienData);
            }
        });

        // Listener cho bảng: Khi chọn dòng -> VIEWING, hiá»ƒn thị data, mở khóa form
        tblNhanVien.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldSelection, newSelection) -> {
            if (currentState == EditState.ADDING) return; // Không làm gì nếu đang thêm
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
    loadNhanVienData(); // Tải dữ liá»u lần Äầu
}
//<editor-fold desc="State Management & UI Control">
private void setFormEditable(boolean editable) {
    txtMaNV.setEditable(false);
    txtHoTen.setEditable(editable);
    txtSDT.setEditable(editable);
    pfMatKhau.setEditable(editable);
    cbxChucVu.setDisable(!editable);
    dpNgaySinh.setDisable(!editable);
    cbxCaLam.setDisable(!editable); // ComboBox Äã Äược má»
    cbxTrangThai.setDisable(!editable);
}
private void updateUIState(EditState state) {
    this.currentState = state;
    boolean rowIsSelected = (currentSelectedNhanVien != null);
    if (state == EditState.ADDING) {
        // Khi Äang thêm má»i (sau khi bấm Xóa rá»ng)
        setFormEditable(true);      // Má» khóa form
        btnXoaRong.setDisable(true);  // Tắt "Xóa rá»ng"
        btnThem.setDisable(false);     // Bật "Thêm" (Lưu má»i)
        btnSua.setDisable(true);
        btnLuu.setDisable(true);      // Tắt nút "Lưu"
        btnXoa.setDisable(true);
        btnHuy.setDisable(false);
        tblNhanVien.setDisable(true);  // Khóa bảng
    } else { // VIEWING state (Äang xem hoặc Äang sửa trên dòng Äã chá»n)
    setFormEditable(rowIsSelected); // Form sửa Äược nếu có dòng Äược chá»n
    btnXoaRong.setDisable(false); // Bật "Xóa rá»ng"
    btnThem.setDisable(true);      // Tắt "Thêm" (Lưu má»i)
    btnSua.setDisable(!rowIsSelected); // Bật Sửa/Xóa/Hủy nếu có dòng Äược chá»n
    btnLuu.setDisable(true);      // Tắt nút "Lưu"
    btnXoa.setDisable(!rowIsSelected);
    btnHuy.setDisable(!rowIsSelected); // Hủy chá» bật khi có dòng Äang chá»n (Äá» revert)
    tblNhanVien.setDisable(false); // Má» khóa bảng
}
}
//</editor-fold>
//<editor-fold desc="Event Handlers">
@FXML
private void handleXoaRong(ActionEvent event) { // Nút "Xóa rá»ng" -> Bắt Äầu thêm
    tblNhanVien.getSelectionModel().clearSelection();
    clearForm();
    txtMaNV.setText(generateNewMaNV());
    pfMatKhau.setPromptText("Bắt buá»c nhập");
    updateUIState(EditState.ADDING); // Chuyá»n sang trạng thái ADDING
    txtHoTen.requestFocus();
}
@FXML
private void handleThem(ActionEvent event) { // Nút "Thêm" -> Lưu nhân viên má»i
    if (currentState != EditState.ADDING || !validateInput()) return;
    NhanVien nv = createNhanVienFromForm();
    Response res = Client.sendWithParams(CommandType.UPDATE_EMPLOYEE, Map.of("employee", nv));
    if (res.getStatusCode() == 200) {
        showInfoAlert("Thành công", "Äã thêm nhân viên má»i!");
        loadNhanVienData();
    } else {
    showErrorAlert("Thất bại", "Thêm má»i không thành công: " + res.getMessage());
}
}
@FXML
private void handleSua(ActionEvent event) { // Nút "Sửa" -> Cập nhật ngay dòng Äang chá»n
    if (currentSelectedNhanVien == null || currentState != EditState.VIEWING || !validateInput()) return;
    NhanVien nv = createNhanVienFromForm();
    Response res = Client.sendWithParams(CommandType.UPDATE_EMPLOYEE, Map.of("employee", nv));
    if (res.getStatusCode() == 200) {
        showInfoAlert("Thành công", "Äã cập nhật thông tin nhân viên!");
        loadNhanVienData();
    } else {
    showErrorAlert("Thất bại", "Cập nhật không thành công: " + res.getMessage());
}
}
@FXML
private void handleLuu(ActionEvent event) { // Nút "Lưu" -> Không làm gì cả
    System.out.println("Nút Lưu không có chức nÄng trong quy trình này.");
}
@FXML
private void handleXoa(ActionEvent event) { // Nút "Xóa"
    if (currentSelectedNhanVien == null || currentState != EditState.VIEWING) return;
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận xóa");
    confirm.setContentText("Bạn có chắc chắn muá»n xóa nhân viên '" + currentSelectedNhanVien.getHoTen() + "'?");
    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        currentSelectedNhanVien.setTrangThai("Nghá» viá»c");
        Response res = Client.sendWithParams(CommandType.UPDATE_EMPLOYEE, Map.of("employee", currentSelectedNhanVien));
        if (res.getStatusCode() == 200) {
            showInfoAlert("Thành công", "Äã xóa (ngừng hoạt Äá»ng) nhân viên!");
            loadNhanVienData();
        } else {
        showErrorAlert("Thất bại", "Xóa nhân viên không thành công: " + res.getMessage());
    }
}
}
@FXML
private void handleHuy(ActionEvent event) { // Nút "Hủy"
    if (currentState == EditState.ADDING) {
        loadNhanVienData(); // Quay vá» trạng thái xem ban Äầu
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
    pfMatKhau.setPromptText("Äá» trá»ng nếu không muá»n Äá»i mật kháº©u");
    // Hiá»n thá» ca làm YíU THíCH
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
        nv = currentSelectedNhanVien; // Dùng trực tiếp Äá»i tượng Äã chá»n
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
// Lấy ca làm yêu thích từ ComboBox
nv.setCaLamYeuThich(cbxCaLam.getValue());
String matKhau = pfMatKhau.getText();
if (!matKhau.isEmpty()) {
    nv.setMatKhau(matKhau);
} else if (currentState == EditState.ADDING) {
nv.setMatKhau(""); // Bắt buá»c mật kháº©u khi thêm má»i (logic validate)
} else if (currentSelectedNhanVien != null) {
// Khi Sửa, nếu mật kháº©u trá»ng -> giữ mật kháº©u cũ (lấy từ DAO)
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
        showErrorAlert("Lá»i", "Há» tên không Äược Äá» trá»ng.");
        txtHoTen.requestFocus();
        return false;
    }
    if (!txtSDT.getText().trim().matches("^0\\d{9}$")) {
        showErrorAlert("Lá»i", "Sá» Äiá»n thoại phải bắt Äầu báº±ng 0 và có 10 chữ sá».");
        txtSDT.requestFocus();
        return false;
    }
    if (cbxChucVu.getValue() == null) {
        showErrorAlert("Lá»i", "Vui lòng chá»n chức vụ.");
        cbxChucVu.requestFocus();
        return false;
    }
    if (dpNgaySinh.getValue() == null) {
        showErrorAlert("Lá»i", "Vui lòng chá»n ngày sinh.");
        dpNgaySinh.requestFocus();
        return false;
    }
    if (currentState == EditState.ADDING && pfMatKhau.getText().isEmpty()) {
        showErrorAlert("Lá»i", "Mật kháº©u là bắt buá»c khi thêm má»i.");
        pfMatKhau.requestFocus();
        return false;
    }
    if (cbxTrangThai.getValue() == null) {
        showErrorAlert("Lá»i", "Vui lòng chá»n trạng thái.");
        cbxTrangThai.requestFocus();
        return false;
    }
    // Không cần validate cbxCaLam (vì có thá» Äá» trá»ng - không yêu thích)
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