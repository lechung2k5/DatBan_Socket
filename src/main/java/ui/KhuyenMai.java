package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.UuDai;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public class KhuyenMai {
    @FXML private TableView<Promotion> tblKhuyenMai;
    @FXML private TableColumn<Promotion, String> colTenKM;
    @FXML private TableColumn<Promotion, String> colMaKM;
    @FXML private TableColumn<Promotion, String> colThoiGian;
    @FXML private TableColumn<Promotion, String> colTrangThai;
    @FXML private TableColumn<Promotion, String> colGiaTri;
    @FXML private TableColumn<Promotion, Void> colHanhDong;
    @FXML private TextField txtTenKM;
    @FXML private TextField txtGiaTri;
    @FXML private DatePicker datePickerStart;
    @FXML private DatePicker datePickerEnd;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button btnThem, btnSua, btnLuu;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private ObservableList<Promotion> promotionList = FXCollections.observableArrayList();
    // private final UuDaiDAO uuDaiDAO = new UuDaiDAO(); // Removed
    private Promotion selectedPromotion = null;
    private boolean isEditMode = false;
    private String currentMaKM = null;
    @FXML
    public void initialize() {
        setupTableColumns();
        loadDataFromDatabase();
        setupFilterComboBox();
        setupButtonEvents();
        setupInputValidation();
        setupTableSelectionEvent();
        resetFormState();

        // 🔥 LẮNG NGHE REAL-TIME (Tải lại danh sách khuyến mãi khi có thay đổi)
        network.RealTimeClient.getInstance().addListener(event -> {
            if (event.getType() == CommandType.UPDATE_PROMO) {
                javafx.application.Platform.runLater(this::loadDataFromDatabase);
            }
        });
    }
    // ==================== SETUP CíC THíNH PHáº¦N ====================
    private void setupTableColumns() {
        tblKhuyenMai.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        colTenKM.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMaKM.setCellValueFactory(new PropertyValueFactory<>("code"));
        colThoiGian.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("status"));
        colGiaTri.setCellValueFactory(new PropertyValueFactory<>("value"));
        colHanhDong.setCellFactory(param -> new TableCell<Promotion, Void>() {
            private final Button deleteButton = new Button("Xóa");
            private final HBox pane = new HBox(deleteButton);
            {
                deleteButton.getStyleClass().add("delete-button");
                pane.setAlignment(Pos.CENTER);
                deleteButton.setOnAction(event -> {
                    Promotion promo = getTableRow().getItem();
                    if (promo != null) {
                        xoaKhuyenMai(promo);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
                if (deleteButton != null) {
                    deleteButton.setDisable(isEditMode);
                }
            }
        });
    }
    private void setupFilterComboBox() {
        filterComboBox.setItems(FXCollections.observableArrayList(
        "Tất cả", "Đang áp dụng", "Sắp diễn ra", "Đã hết hạn"
        ));
        filterComboBox.setValue("Tất cả");
        filterComboBox.setOnAction(e -> filterPromotions(filterComboBox.getValue()));
    }
    private void setupButtonEvents() {
        btnThem.setOnAction(e -> themKhuyenMai());
        btnSua.setOnAction(e -> chinhSuaKhuyenMai());
        btnLuu.setOnAction(e -> luuKhuyenMai());
    }
    private void setupInputValidation() {
        // RíNG BUá»C: Tên khuyến mãi giá»i hạn Äá» dài
        txtTenKM.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 100) {
                txtTenKM.setText(oldVal);
            }
        });
        // RíNG BUá»C: Giá trá» giá»i hạn Äá» dài (cho phép nhập tự do, validate khi Lưu)
        txtGiaTri.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 10) {
                txtGiaTri.setText(oldVal);
            }
        });
    }
    private void setupTableSelectionEvent() {
        tblKhuyenMai.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isEditMode) {
                selectedPromotion = newVal;
                loadPromotionToForm(newVal);
            }
        });
        tblKhuyenMai.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && selectedPromotion != null && !isEditMode) {
                chinhSuaKhuyenMai();
            }
        });
    }
    // ==================== Xá»¬ Lí DATABASE ====================
    private void loadDataFromDatabase() {
        try {
            Response res = Client.send(CommandType.GET_PROMOS, null);
            if (res.getStatusCode() == 200) {
                List<UuDai> uuDaiList = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), UuDai.class);
                promotionList.clear();
                for (UuDai ud : uuDaiList) {
                    Promotion promo = new Promotion(
                    ud.getTenUuDai(),
                    ud.getMaUuDai(),
                    ud.getNgayBatDau(),
                    ud.getNgayKetThuc(),
                    ud.getTrangThai(),
                    String.format("%.0f%%", ud.getGiaTri())
                    );
                    promotionList.add(promo);
                }
                tblKhuyenMai.setItems(promotionList);
            } else {
            showAlert(Alert.AlertType.ERROR, "Lá»i", "Không thá» tải dữ liá»u: " + res.getMessage());
        }
    } catch (Exception e) {
    showAlert(Alert.AlertType.ERROR, "Lá»i", "Lá»i: " + e.getMessage());
    e.printStackTrace();
}
}
// ==================== Xá»¬ Lí CíC NíT ====================
private void themKhuyenMai() {
    if (isEditMode) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Bạn Äang trong chế Äá» chá»nh sửa");
        confirm.setContentText("Dữ liá»u chưa lưu sáº½ bá» mất. Bạn có muá»n tiếp tục?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
    }
    clearForm();
    setFormEditable(true);
    isEditMode = true;
    selectedPromotion = null;
    currentMaKM = ""; // Server handles ID generation or we can use a UUID temporarily
    txtTenKM.requestFocus();
    updateButtonStates(true, true, false);
    updateTableState(true);
    showAlert(Alert.AlertType.INFORMATION, "Thêm má»i khuyến mãi",
    "Mã khuyến mãi sáº½ Äược tự Äá»ng phát sinh khi Lưu.\nVui lòng nhập Äầy Äủ thông tin.");
}
private void chinhSuaKhuyenMai() {
    // RíNG BUá»C: Phải chá»n voucher trưá»c khi sửa
    if (selectedPromotion == null) {
        showAlert(Alert.AlertType.WARNING, "Chưa chá»n", "Vui lòng chá»n khuyến mãi cần sửa từ bảng.");
        return;
    }
    isEditMode = true;
    currentMaKM = selectedPromotion.getCode();
    setFormEditable(true);
    updateButtonStates(true, true, false);
    updateTableState(true);
    txtTenKM.requestFocus();
    txtTenKM.selectAll();
}
private void luuKhuyenMai() {
    // RíNG BUá»C: Kiá»m tra tất cả ràng buá»c trưá»c khi lưu
    if (!validateAllInput()) {
        return;
    }
    String tenKM = txtTenKM.getText().trim();
    double giaTri = Double.parseDouble(txtGiaTri.getText().trim());
    LocalDate ngayBatDau = datePickerStart.getValue();
    LocalDate ngayKetThuc = datePickerEnd.getValue();
    // RíNG BUá»C: Kiá»m tra trùng mã voucher (Äã xử lý trong DAO)
    UuDai uuDai = new UuDai();
    uuDai.setMaUuDai(currentMaKM);
    uuDai.setTenUuDai(tenKM);
    uuDai.setGiaTri(giaTri);
    uuDai.setNgayBatDau(ngayBatDau);
    uuDai.setNgayKetThuc(ngayKetThuc);
    Response res = Client.sendWithParams(CommandType.UPDATE_PROMO, Map.of("promo", uuDai));
    if (res.getStatusCode() == 200) {
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.INFORMATION, "Thêm thành công", "Äã thêm khuyến mãi má»i: " + tenKM);
        } else {
        showAlert(Alert.AlertType.INFORMATION, "Cập nhật thành công", "Äã cập nhật khuyến mãi: " + tenKM);
    }
} else {
showAlert(Alert.AlertType.ERROR, "Lá»i", "Lá»i: " + res.getMessage());
return;
}
// RíNG BUá»C: Ghi log lá»ch sử thao tác (Äã xử lý trong DAO)
loadDataFromDatabase();
resetFormState();
}
private void xoaKhuyenMai(Promotion promo) {
    // RíNG BUá»C: Chá» quản lý má»i có quyá»n xóa
    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
    confirmDialog.setTitle("Xác nhận xóa");
    confirmDialog.setHeaderText("Bạn chắc chắn muá»n xóa khuyến mãi này?");
    confirmDialog.setContentText("Khuyến mãi: " + promo.getName() + "\nMã: " + promo.getCode() +
    "\n\nHành Äá»ng này không thá» hoàn tác.");
    Optional<ButtonType> result = confirmDialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            Response res = Client.sendWithParams(CommandType.DELETE_PROMO, Map.of("id", promo.getCode()));
            if (res.getStatusCode() == 200) {
                showAlert(Alert.AlertType.INFORMATION, "Xóa thành công",
                "Äã xóa khuyến mãi \"" + promo.getName() + "\"");
                if (selectedPromotion != null &&
                selectedPromotion.getCode().equals(promo.getCode())) {
                    selectedPromotion = null;
                }
                loadDataFromDatabase();
                resetFormState();
            } else {
            showAlert(Alert.AlertType.ERROR, "Lá»i xóa", "Không thá» xóa: " + res.getMessage());
        }
    } catch (Exception e) {
    showAlert(Alert.AlertType.ERROR, "Lá»i",
    "Không thá» xóa khuyến mãi: " + e.getMessage() +
    "\n\nKhuyến mãi có thá» Äang Äược sử dụng trong hóa Äơn hoặc bá» ràng buá»c dữ liá»u.");
    e.printStackTrace();
}
}
}
// ==================== Xá»¬ Lí Lá»C ====================
private void filterPromotions(String filter) {
    if (filter.equals("Tất cả")) {
        loadDataFromDatabase();
    } else {
    Response res = Client.sendWithParams(CommandType.GET_PROMOS, Map.of("status", filter));
    if (res.getStatusCode() == 200) {
        List<UuDai> filteredList = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), UuDai.class);
        promotionList.clear();
        for (UuDai ud : filteredList) {
            Promotion promo = new Promotion(
            ud.getTenUuDai(),
            ud.getMaUuDai(),
            ud.getNgayBatDau(),
            ud.getNgayKetThuc(),
            ud.getTrangThai(),
            String.format("%.0f%%", ud.getGiaTri())
            );
            promotionList.add(promo);
        }
        tblKhuyenMai.setItems(promotionList);
    }
}
}
// ==================== Xá»¬ Lí FORM ====================
private void loadPromotionToForm(Promotion promo) {
    if (promo == null) return;
    Response res = Client.sendWithParams(CommandType.GET_PROMO_BY_ID, Map.of("id", promo.getCode()));
    if (res.getStatusCode() == 200) {
        UuDai uuDai = JsonUtil.fromJson(JsonUtil.toJson(res.getData()), UuDai.class);
        if (uuDai != null) {
            txtTenKM.setText(uuDai.getTenUuDai());
            txtGiaTri.setText(String.valueOf(uuDai.getGiaTri()));
            datePickerStart.setValue(uuDai.getNgayBatDau());
            datePickerEnd.setValue(uuDai.getNgayKetThuc());
        }
    }
}
private void clearForm() {
    txtTenKM.clear();
    txtGiaTri.clear();
    datePickerStart.setValue(null);
    datePickerEnd.setValue(null);
    datePickerStart.getEditor().clear();
    datePickerEnd.getEditor().clear();
}
private void resetFormState() {
    clearForm();
    setFormEditable(false);
    selectedPromotion = null;
    currentMaKM = null;
    isEditMode = false;
    updateButtonStates(false, false, true);
    updateTableState(false);
    tblKhuyenMai.getSelectionModel().clearSelection();
}
private void setFormEditable(boolean editable) {
    txtTenKM.setEditable(editable);
    txtGiaTri.setEditable(editable);
    datePickerStart.setDisable(!editable);
    datePickerEnd.setDisable(!editable);
}
private void updateButtonStates(boolean disableThem, boolean disableSua, boolean disableLuu) {
    btnThem.setDisable(disableThem);
    btnSua.setDisable(disableSua);
    btnLuu.setDisable(disableLuu);
}
private void updateTableState(boolean disable) {
    tblKhuyenMai.setDisable(disable);
    filterComboBox.setDisable(disable);
    tblKhuyenMai.refresh();
}
// ==================== VALIDATION - RíNG BUá»C ====================
private boolean validateAllInput() {
    return validateMaUuDai() &&
    validateTenKhuyenMai() &&
    validateGiaTri() &&
    validateNgayBatDau() &&
    validateNgayKetThuc() &&
    validateDateRange();
}
// RíNG BUá»C 1: Mã ưu Äãi (maUuDai)
// - Không Äược null
// - Không Äược rá»ng ("")
// - Äá»nh dạng: {id}, NOT NULL
private boolean validateMaUuDai() {
    if (currentMaKM == null || currentMaKM.trim().isEmpty()) {
        showAlert(Alert.AlertType.ERROR, "Lá»i há» thá»ng", "Mã ưu Äãi không Äược tạo. Vui lòng thử lại.");
        return false;
    }
    return true;
}
// RíNG BUá»C 2: Tên ưu Äãi (tenUuDai)
// - Không Äược null
// - Không Äược rá»ng ("")
// - Bắt buá»c nhập
private boolean validateTenKhuyenMai() {
    String tenKM = txtTenKM.getText().trim();
    if (tenKM.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin",
        "Tên khuyến mãi không Äược Äá» trá»ng.\nVui lòng nhập tên khuyến mãi.");
        txtTenKM.requestFocus();
        return false;
    }
    if (tenKM.length() < 5) {
        showAlert(Alert.AlertType.WARNING, "Tên quá ngắn",
        "Tên khuyến mãi phải có ít nhất 5 ký tự Äá» Äảm bảo Äầy Äủ thông tin.");
        txtTenKM.requestFocus();
        return false;
    }
    return true;
}
// RíNG BUá»C 3: Giá trá» ưu Äãi (giaTri)
// - Phải >= 0
// - Kiá»u dữ liá»u: double
// - Có thá» là % hoặc sá» tiá»n cụ thá»
// - Giá trá» từ 0 Äến 100 (%)
private boolean validateGiaTri() {
    String giaTriStr = txtGiaTri.getText().trim();
    if (giaTriStr.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin",
        "Giá trá» khuyến mãi không Äược Äá» trá»ng.\nVui lòng nhập giá trá» khuyến mãi (%).");
        txtGiaTri.requestFocus();
        return false;
    }
    try {
        double giaTri = Double.parseDouble(giaTriStr);
        // RíNG BUá»C: giaTri >= 0
        if (giaTri < 0) {
            showAlert(Alert.AlertType.WARNING, "Giá trá» không hợp lá»",
            "Giá trá» khuyến mãi không Äược âm.\nVui lòng nhập giá trá» từ 0 Äến 100.");
            txtGiaTri.requestFocus();
            return false;
        }
        // RíNG BUá»C: giaTri <= 100 (%)
        if (giaTri > 100) {
            showAlert(Alert.AlertType.WARNING, "Giá trá» không hợp lá»",
            "Giá trá» khuyến mãi không Äược vượt quá 100%.\nVui lòng nhập giá trá» từ 0 Äến 100.");
            txtGiaTri.requestFocus();
            return false;
        }
    } catch (NumberFormatException e) {
    showAlert(Alert.AlertType.WARNING, "Giá trá» không hợp lá»",
    "Giá trá» khuyến mãi phải là sá».\nVui lòng nhập sá» hợp lá» (ví dụ: 10, 15.5).");
    txtGiaTri.requestFocus();
    return false;
}
return true;
}
// RíNG BUá»C 4: Ngày bắt Äầu (ngayBatDau)
// - NOT NULL
// - Kiá»u dữ liá»u: Date
private boolean validateNgayBatDau() {
    if (datePickerStart.getValue() == null) {
        showAlert(Alert.AlertType.WARNING, "Thiếu ngày bắt Äầu",
        "Ngày bắt Äầu không Äược Äá» trá»ng.\nVui lòng chá»n ngày bắt Äầu áp dụng khuyến mãi.");
        datePickerStart.requestFocus();
        return false;
    }
    return true;
}
// RíNG BUá»C 5: Ngày kết thúc (ngayKetThuc)
// - NOT NULL
// - Phải >= ngayBatDau
// - Kiá»u dữ liá»u: Date
private boolean validateNgayKetThuc() {
    if (datePickerEnd.getValue() == null) {
        showAlert(Alert.AlertType.WARNING, "Thiếu ngày kết thúc",
        "Ngày kết thúc không Äược Äá» trá»ng.\nVui lòng chá»n ngày kết thúc áp dụng khuyến mãi.");
        datePickerEnd.requestFocus();
        return false;
    }
    return true;
}
// RíNG BUá»C 6: Khoảng thá»i gian
// - ngayKetThuc >= ngayBatDau
private boolean validateDateRange() {
    LocalDate ngayBatDau = datePickerStart.getValue();
    LocalDate ngayKetThuc = datePickerEnd.getValue();
    if (ngayBatDau != null && ngayKetThuc != null) {
        if (ngayKetThuc.isBefore(ngayBatDau)) {
            showAlert(Alert.AlertType.WARNING, "Ngày không hợp lá»",
            "Ngày kết thúc phải sau hoặc báº±ng ngày bắt Äầu.\n" +
            "Ngày bắt Äầu: " + ngayBatDau.format(formatter) + "\n" +
            "Ngày kết thúc: " + ngayKetThuc.format(formatter));
            datePickerEnd.requestFocus();
            return false;
        }
    }
    return true;
}
// ==================== TIá»N ÍCH ====================
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
// ==================== INNER CLASS ====================
public static class Promotion {
    private final SimpleStringProperty name;
    private final SimpleStringProperty code;
    private final SimpleStringProperty duration;
    private final SimpleStringProperty status;
    private final SimpleStringProperty value;
    public Promotion(String name, String code, LocalDate startDate, LocalDate endDate, String status, String value) {
        this.name = new SimpleStringProperty(name);
        this.code = new SimpleStringProperty(code);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        this.duration = new SimpleStringProperty(startDate.format(formatter) + " - " + endDate.format(formatter));
        this.status = new SimpleStringProperty(status);
        this.value = new SimpleStringProperty(value);
    }
    public String getName() { return name.get(); }
    public String getCode() { return code.get(); }
    public String getDuration() { return duration.get(); }
    public String getStatus() { return status.get(); }
    public String getValue() { return value.get(); }
}
}