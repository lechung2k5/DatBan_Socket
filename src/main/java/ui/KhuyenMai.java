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
    // ==================== SETUP CÃC THÃNH PHáº¦N ====================
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
        // RÃNG BUá»C: Tên khuyến mãi giá»i hạn Äá» dÃ i
        txtTenKM.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 100) {
                txtTenKM.setText(oldVal);
            }
        });
        // RÃNG BUá»C: Giá trá» giá»i hạn Äá» dÃ i (cho phÃ©p nháº­p tự do, validate khi Lưu)
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
    // ==================== Xá»¬ LÃ DATABASE ====================
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
// ==================== Xá»¬ LÃ CÃC NÃT ====================
private void themKhuyenMai() {
    if (isEditMode) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nháº­n");
        confirm.setHeaderText("Bạn Äang trong chế Äá» chá»nh sá»­a");
        confirm.setContentText("Dữ liá»u chưa lưu sáº½ bá» máº¥t. Bạn có muá»n tiếp tục?");
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
    "Mã khuyến mãi sáº½ Äưá»£c tự Äá»ng phát sinh khi Lưu.\nVui lòng nháº­p Äáº§y Äủ thông tin.");
}
private void chinhSuaKhuyenMai() {
    // RÃNG BUá»C: Phải chá»n voucher trưá»c khi sá»­a
    if (selectedPromotion == null) {
        showAlert(Alert.AlertType.WARNING, "Chưa chá»n", "Vui lòng chá»n khuyến mãi cáº§n sá»­a từ bảng.");
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
    // RÃNG BUá»C: Kiá»m tra táº¥t cả rÃ ng buá»c trưá»c khi lưu
    if (!validateAllInput()) {
        return;
    }
    String tenKM = txtTenKM.getText().trim();
    double giaTri = Double.parseDouble(txtGiaTri.getText().trim());
    LocalDate ngayBatDau = datePickerStart.getValue();
    LocalDate ngayKetThuc = datePickerEnd.getValue();
    // RÃNG BUá»C: Kiá»m tra trùng mã voucher (Äã xá»­ lý trong DAO)
    UuDai uuDai = new UuDai();
    uuDai.setMaUuDai(currentMaKM);
    uuDai.setTenUuDai(tenKM);
    uuDai.setGiaTri(giaTri);
    uuDai.setNgayBatDau(ngayBatDau);
    uuDai.setNgayKetThuc(ngayKetThuc);
    Response res = Client.sendWithParams(CommandType.UPDATE_PROMO, Map.of("promo", uuDai));
    if (res.getStatusCode() == 200) {
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.INFORMATION, "Thêm thÃ nh công", "Äã thêm khuyến mãi má»i: " + tenKM);
        } else {
        showAlert(Alert.AlertType.INFORMATION, "Cáº­p nháº­t thÃ nh công", "Äã cáº­p nháº­t khuyến mãi: " + tenKM);
    }
} else {
showAlert(Alert.AlertType.ERROR, "Lá»i", "Lá»i: " + res.getMessage());
return;
}
// RÃNG BUá»C: Ghi log lá»ch sá»­ thao tác (Äã xá»­ lý trong DAO)
loadDataFromDatabase();
resetFormState();
}
private void xoaKhuyenMai(Promotion promo) {
    // RÃNG BUá»C: Chá» quản lý má»i có quyá»n xóa
    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
    confirmDialog.setTitle("Xác nháº­n xóa");
    confirmDialog.setHeaderText("Bạn cháº¯c cháº¯n muá»n xóa khuyến mãi nÃ y?");
    confirmDialog.setContentText("Khuyến mãi: " + promo.getName() + "\nMã: " + promo.getCode() +
    "\n\nHÃ nh Äá»ng nÃ y không thá» hoÃ n tác.");
    Optional<ButtonType> result = confirmDialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            Response res = Client.sendWithParams(CommandType.DELETE_PROMO, Map.of("id", promo.getCode()));
            if (res.getStatusCode() == 200) {
                showAlert(Alert.AlertType.INFORMATION, "Xóa thÃ nh công",
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
    "\n\nKhuyến mãi có thá» Äang Äưá»£c sá»­ dụng trong hóa Äơn hoáº·c bá» rÃ ng buá»c dữ liá»u.");
    e.printStackTrace();
}
}
}
// ==================== Xá»¬ LÃ Lá»C ====================
private void filterPromotions(String filter) {
    if (filter.equals("Táº¥t cả")) {
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
// ==================== Xá»¬ LÃ FORM ====================
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
// ==================== VALIDATION - RÃNG BUá»C ====================
private boolean validateAllInput() {
    return validateMaUuDai() &&
    validateTenKhuyenMai() &&
    validateGiaTri() &&
    validateNgayBatDau() &&
    validateNgayKetThuc() &&
    validateDateRange();
}
// RÃNG BUá»C 1: Mã ưu Äãi (maUuDai)
// - Không Äưá»£c null
// - Không Äưá»£c rá»ng ("")
// - Äá»nh dạng: {id}, NOT NULL
private boolean validateMaUuDai() {
    if (currentMaKM == null || currentMaKM.trim().isEmpty()) {
        showAlert(Alert.AlertType.ERROR, "Lá»i há» thá»ng", "Mã ưu Äãi không Äưá»£c tạo. Vui lòng thá»­ lại.");
        return false;
    }
    return true;
}
// RÃNG BUá»C 2: Tên ưu Äãi (tenUuDai)
// - Không Äưá»£c null
// - Không Äưá»£c rá»ng ("")
// - Báº¯t buá»c nháº­p
private boolean validateTenKhuyenMai() {
    String tenKM = txtTenKM.getText().trim();
    if (tenKM.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin",
        "Tên khuyến mãi không Äưá»£c Äá» trá»ng.\nVui lòng nháº­p tên khuyến mãi.");
        txtTenKM.requestFocus();
        return false;
    }
    if (tenKM.length() < 5) {
        showAlert(Alert.AlertType.WARNING, "Tên quá ngáº¯n",
        "Tên khuyến mãi phải có Ã­t nháº¥t 5 ký tự Äá» Äảm bảo Äáº§y Äủ thông tin.");
        txtTenKM.requestFocus();
        return false;
    }
    return true;
}
// RÃNG BUá»C 3: Giá trá» ưu Äãi (giaTri)
// - Phải >= 0
// - Kiá»u dữ liá»u: double
// - Có thá» lÃ  % hoáº·c sá» tiá»n cụ thá»
// - Giá trá» từ 0 Äến 100 (%)
private boolean validateGiaTri() {
    String giaTriStr = txtGiaTri.getText().trim();
    if (giaTriStr.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Thiếu thông tin",
        "Giá trá» khuyến mãi không Äưá»£c Äá» trá»ng.\nVui lòng nháº­p giá trá» khuyến mãi (%).");
        txtGiaTri.requestFocus();
        return false;
    }
    try {
        double giaTri = Double.parseDouble(giaTriStr);
        // RÃNG BUá»C: giaTri >= 0
        if (giaTri < 0) {
            showAlert(Alert.AlertType.WARNING, "Giá trá» không há»£p lá»",
            "Giá trá» khuyến mãi không Äưá»£c âm.\nVui lòng nháº­p giá trá» từ 0 Äến 100.");
            txtGiaTri.requestFocus();
            return false;
        }
        // RÃNG BUá»C: giaTri <= 100 (%)
        if (giaTri > 100) {
            showAlert(Alert.AlertType.WARNING, "Giá trá» không há»£p lá»",
            "Giá trá» khuyến mãi không Äưá»£c vưá»£t quá 100%.\nVui lòng nháº­p giá trá» từ 0 Äến 100.");
            txtGiaTri.requestFocus();
            return false;
        }
    } catch (NumberFormatException e) {
    showAlert(Alert.AlertType.WARNING, "Giá trá» không há»£p lá»",
    "Giá trá» khuyến mãi phải lÃ  sá».\nVui lòng nháº­p sá» há»£p lá» (vÃ­ dụ: 10, 15.5).");
    txtGiaTri.requestFocus();
    return false;
}
return true;
}
// RÃNG BUá»C 4: NgÃ y báº¯t Äáº§u (ngayBatDau)
// - NOT NULL
// - Kiá»u dữ liá»u: Date
private boolean validateNgayBatDau() {
    if (datePickerStart.getValue() == null) {
        showAlert(Alert.AlertType.WARNING, "Thiếu ngÃ y báº¯t Äáº§u",
        "NgÃ y báº¯t Äáº§u không Äưá»£c Äá» trá»ng.\nVui lòng chá»n ngÃ y báº¯t Äáº§u áp dụng khuyến mãi.");
        datePickerStart.requestFocus();
        return false;
    }
    return true;
}
// RÃNG BUá»C 5: NgÃ y kết thúc (ngayKetThuc)
// - NOT NULL
// - Phải >= ngayBatDau
// - Kiá»u dữ liá»u: Date
private boolean validateNgayKetThuc() {
    if (datePickerEnd.getValue() == null) {
        showAlert(Alert.AlertType.WARNING, "Thiếu ngÃ y kết thúc",
        "NgÃ y kết thúc không Äưá»£c Äá» trá»ng.\nVui lòng chá»n ngÃ y kết thúc áp dụng khuyến mãi.");
        datePickerEnd.requestFocus();
        return false;
    }
    return true;
}
// RÃNG BUá»C 6: Khoảng thá»i gian
// - ngayKetThuc >= ngayBatDau
private boolean validateDateRange() {
    LocalDate ngayBatDau = datePickerStart.getValue();
    LocalDate ngayKetThuc = datePickerEnd.getValue();
    if (ngayBatDau != null && ngayKetThuc != null) {
        if (ngayKetThuc.isBefore(ngayBatDau)) {
            showAlert(Alert.AlertType.WARNING, "NgÃ y không há»£p lá»",
            "NgÃ y kết thúc phải sau hoáº·c báº±ng ngÃ y báº¯t Äáº§u.\n" +
            "NgÃ y báº¯t Äáº§u: " + ngayBatDau.format(formatter) + "\n" +
            "NgÃ y kết thúc: " + ngayKetThuc.format(formatter));
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