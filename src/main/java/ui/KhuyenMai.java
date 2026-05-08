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

    // ==================== SETUP CÁC THÀNH PHẦN ====================
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
        // RÀNG BUỘC: Tên khuyến mãi giới hạn độ dài
        txtTenKM.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 100) {
                txtTenKM.setText(oldVal);
            }
        });
        // RÀNG BUỘC: Giá trị giới hạn độ dài (cho phép nhập tự do, validate khi Lưu)
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

    // ==================== XỬ LÝ DATABASE ====================
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
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu: " + res.getMessage());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== XỬ LÝ CÁC NÚT ====================
    private void themKhuyenMai() {
        if (isEditMode) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận");
            confirm.setHeaderText("Bạn đang trong chế độ chỉnh sửa");
            confirm.setContentText("Dữ liệu chưa lưu sẽ bị mất. Bạn có muốn tiếp tục?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }
        clearForm();
        setFormEditable(true);
        isEditMode = true;
        selectedPromotion = null;
        // Tìm số thứ tự LỚN NHẤT đang có trong danh sách, rồi +1 để tránh trùng mã
        int maxNum = 0;
        for (Promotion p : promotionList) {
            String code = p.getCode();
            if (code != null && code.matches("KM\\d+")) {
                try {
                    int num = Integer.parseInt(code.substring(2));
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        currentMaKM = String.format("KM%03d", maxNum + 1);
        txtTenKM.requestFocus();
        updateButtonStates(true, true, false);
        updateTableState(true);
        showAlert(Alert.AlertType.INFORMATION, "Thêm mới khúyến mãi",
        "Mã khúyến mãi đã được tự động phát sinh: " + currentMaKM + ".\nVui lòng nhập đầy đủ thông tin.");
    }

    private void chinhSuaKhuyenMai() {
        // RÀNG BUỘC: Phải chọn voucher trước khi sửa
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn khuyến mãi cần sửa từ bảng.");
            return;
        }
        isEditMode = true;
        currentMaKM = selectedPromotion.getCode();
        setFormEditable(true);
        // Khóa ngày bắt đầu nếu khuyến mãi đã diễn ra
        if (datePickerStart.getValue() != null && !datePickerStart.getValue().isAfter(LocalDate.now())) {
            datePickerStart.setDisable(true);
        }
        updateButtonStates(true, true, false);
        updateTableState(true);
        txtTenKM.requestFocus();
        txtTenKM.selectAll();
    }

    private void luuKhuyenMai() {
        // RÀNG BUỘC: Kiểm tra tất cả ràng buộc trước khi lưu
        if (!validateAllInput()) {
            return;
        }
        String tenKM = txtTenKM.getText().trim();
        double giaTri = Double.parseDouble(txtGiaTri.getText().trim());
        LocalDate ngayBatDau = datePickerStart.getValue();
        LocalDate ngayKetThuc = datePickerEnd.getValue();
        // RÀNG BUỘC: Kiểm tra trùng mã voucher (đã xử lý trong DAO)
        UuDai uuDai = new UuDai();
        uuDai.setMaUuDai(currentMaKM);
        uuDai.setTenUuDai(tenKM);
        uuDai.setGiaTri(giaTri);
        uuDai.setNgayBatDau(ngayBatDau);
        uuDai.setNgayKetThuc(ngayKetThuc);
        Response res = Client.sendWithParams(CommandType.UPDATE_PROMO, Map.of("promo", uuDai));
        if (res.getStatusCode() == 200) {
            if (selectedPromotion == null) {
                showAlert(Alert.AlertType.INFORMATION, "Thêm thành công", "Đã thêm khuyến mãi mới: " + tenKM);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Cập nhật thành công", "Đã cập nhật khuyến mãi: " + tenKM);
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi: " + res.getMessage());
            return;
        }
        // Fix DynamoDB Eventual Consistency: Đợi 500ms trước khi tải lại để đảm bảo data đã lên DB
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                loadDataFromDatabase();
                filterComboBox.setValue("Tất cả"); // Reset filter
                resetFormState();
            });
        }).start();
    }

    private void xoaKhuyenMai(Promotion promo) {
        // RÀNG BUỘC: Không cho xóa nếu đang diễn ra và chưa kết thúc hôm nay
        if ("Đang áp dụng".equals(promo.getStatus())) {
            LocalDate endDate = promo.getEndDate();
            // Chỉ chặn xóa nếu ngày kết thúc còn trong tương lai (sau hôm nay)
            if (endDate != null && endDate.isAfter(LocalDate.now())) {
                showAlert(Alert.AlertType.WARNING, "Không thể xóa",
                "Khuyến mãi này đang có hiệu lực đến " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                ".\nBạn không thể xóa để tránh lỗi hóa đơn.\nHãy sửa 'Ngày kết thúc' về hôm nay để vô hiệu hóa nó.");
                return;
            }
        }

        // RÀNG BUỘC: Chỉ quản lý mới có quyền xóa
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText("Bạn chắc chắn muốn xóa khuyến mãi này?");
        confirmDialog.setContentText("Khuyến mãi: " + promo.getName() + "\nMã: " + promo.getCode() +
        "\n\nHành động này không thể hoàn tác.");
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                java.util.Map<String, Object> delParams = new java.util.HashMap<>();
                delParams.put("id", promo.getCode());
                Response res = Client.sendWithParams(CommandType.DELETE_PROMO, delParams);
                if (res.getStatusCode() == 200) {
                    showAlert(Alert.AlertType.INFORMATION, "Xóa thành công",
                    "Đã xóa khuyến mãi \"" + promo.getName() + "\"");
                    if (selectedPromotion != null &&
                    selectedPromotion.getCode().equals(promo.getCode())) {
                        selectedPromotion = null;
                    }
                    new Thread(() -> {
                        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                        javafx.application.Platform.runLater(() -> {
                            loadDataFromDatabase();
                            resetFormState();
                        });
                    }).start();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi xóa", "Không thể xóa: " + res.getMessage());
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi",
                "Không thể xóa khuyến mãi: " + e.getMessage() +
                "\n\nKhuyến mãi có thể đang được sử dụng trong hóa đơn hoặc bị ràng buộc dữ liệu.");
                e.printStackTrace();
            }
        }
    }

    // ==================== XỬ LÝ LỌC ====================
    private void filterPromotions(String filter) {
        if (filter == null || filter.equals("Tất cả")) {
            tblKhuyenMai.setItems(promotionList);
        } else {
            ObservableList<Promotion> filtered = FXCollections.observableArrayList();
            for (Promotion p : promotionList) {
                if (filter.equals(p.getStatus())) {
                    filtered.add(p);
                }
            }
            tblKhuyenMai.setItems(filtered);
        }
    }

    // ==================== XỬ LÝ FORM ====================
    private void loadPromotionToForm(Promotion promo) {
        if (promo == null) return;
        // Guard null: data cũ trong DB có thể không có mã do lỗi nhập liệu trước
        if (promo.getCode() == null || promo.getCode().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Không thể sửa",
            "Không tìm thấy mã khúyến mãi. Dữ liệu này có thể bị lỗi, vui lòng xóa và tạo lại.");
            return;
        }
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("id", promo.getCode());
            Response res = Client.sendWithParams(CommandType.GET_PROMO_BY_ID, params);
            if (res.getStatusCode() == 200) {
                UuDai uuDai = JsonUtil.fromJson(JsonUtil.toJson(res.getData()), UuDai.class);
                if (uuDai != null) {
                    txtTenKM.setText(uuDai.getTenUuDai() != null ? uuDai.getTenUuDai() : "");
                    txtGiaTri.setText(String.valueOf(uuDai.getGiaTri()));
                    datePickerStart.setValue(uuDai.getNgayBatDau());
                    datePickerEnd.setValue(uuDai.getNgayKetThuc());
                }
            }
        } catch (Exception e) {
            System.err.println("[KhuyenMai] Lỗi loadPromotionToForm: " + e.getMessage());
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

    // ==================== VALIDATION - RÀNG BUỘC ====================
    private boolean validateAllInput() {
        return validateMaUuDai() &&
        validateTenKhuyenMai() &&
        validateGiaTri() &&
        validateNgayBatDau() &&
        validateNgayKetThuc() &&
        validateDateRange();
    }

    // RÀNG BUỘC 1: Mã ưu đãi (maUuDai)
    private boolean validateMaUuDai() {
        if (currentMaKM == null || currentMaKM.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Mã ưu đãi không được tạo. Vui lòng thử lại.");
            return false;
        }
        return true;
    }

    // RÀNG BUỘC 2: Tên ưu đãi (tenUuDai)
    private boolean validateTenKhuyenMai() {
        String tenKM = txtTenKM.getText().trim();
        if (tenKM.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin",
            "Tên khuyến mãi không được để trống.\nVui lòng nhập tên khuyến mãi.");
            txtTenKM.requestFocus();
            return false;
        }
        if (tenKM.length() < 5) {
            showAlert(Alert.AlertType.WARNING, "Tên quá ngắn",
            "Tên khuyến mãi phải có ít nhất 5 ký tự để đảm bảo đầy đủ thông tin.");
            txtTenKM.requestFocus();
            return false;
        }
        // Kiểm tra ký tự đặc biệt
        if (tenKM.matches(".*[<>{}\\[\\]].*")) {
            showAlert(Alert.AlertType.WARNING, "Ký tự không hợp lệ",
            "Tên khuyến mãi không được chứa các ký tự đặc biệt như < > { } [ ].");
            txtTenKM.requestFocus();
            return false;
        }
        // Kiểm tra trùng lặp tên
        for (Promotion p : promotionList) {
            if (p.getName().equalsIgnoreCase(tenKM) && !p.getCode().equals(currentMaKM)) {
                showAlert(Alert.AlertType.WARNING, "Trùng lặp dữ liệu",
                "Tên khuyến mãi này đã tồn tại trong hệ thống.\nVui lòng đặt một tên khác để tránh nhầm lẫn.");
                txtTenKM.requestFocus();
                return false;
            }
        }
        return true;
    }

    // RÀNG BUỘC 3: Giá trị ưu đãi (giaTri)
    private boolean validateGiaTri() {
        String giaTriStr = txtGiaTri.getText().trim();
        if (giaTriStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin",
            "Giá trị khuyến mãi không được để trống.\nVui lòng nhập giá trị khuyến mãi (%).");
            txtGiaTri.requestFocus();
            return false;
        }
        try {
            double giaTri = Double.parseDouble(giaTriStr);
            if (giaTri <= 0) {
                showAlert(Alert.AlertType.WARNING, "Giá trị không hợp lệ",
                "Giá trị khuyến mãi phải lớn hơn 0.\nVui lòng nhập giá trị hợp lệ.");
                txtGiaTri.requestFocus();
                return false;
            }
            if (giaTri > 100) {
                showAlert(Alert.AlertType.WARNING, "Giá trị không hợp lệ",
                "Giá trị khuyến mãi không được vượt quá 100%.\nVui lòng nhập giá trị từ 0 đến 100.");
                txtGiaTri.requestFocus();
                return false;
            }
            // Làm tròn đến 2 chữ số thập phân
            txtGiaTri.setText(String.format(java.util.Locale.US, "%.2f", giaTri).replace(".00", ""));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Giá trị không hợp lệ",
            "Giá trị khuyến mãi phải là số.\nVui lòng nhập số hợp lệ (ví dụ: 10, 15.5).");
            txtGiaTri.requestFocus();
            return false;
        }
        return true;
    }

    // RÀNG BUỘC 4: Ngày bắt đầu (ngayBatDau)
    private boolean validateNgayBatDau() {
        if (datePickerStart.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu ngày bắt đầu",
            "Ngày bắt đầu không được để trống.\nVui lòng chọn ngày bắt đầu áp dụng khuyến mãi.");
            datePickerStart.requestFocus();
            return false;
        }
        // Thêm mới: Không được tạo mới khuyến mãi có ngày bắt đầu trong quá khứ
        if (selectedPromotion == null && datePickerStart.getValue().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Ngày không hợp lệ",
            "Không thể tạo khuyến mãi mới có ngày bắt đầu trong quá khứ.\nVui lòng chọn ngày từ hôm nay trở đi.");
            datePickerStart.requestFocus();
            return false;
        }
        return true;
    }

    // RÀNG BUỘC 5: Ngày kết thúc (ngayKetThuc)
    private boolean validateNgayKetThuc() {
        if (datePickerEnd.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu ngày kết thúc",
            "Ngày kết thúc không được để trống.\nVui lòng chọn ngày kết thúc áp dụng khuyến mãi.");
            datePickerEnd.requestFocus();
            return false;
        }
        return true;
    }

    // RÀNG BUỘC 6: Khoảng thời gian
    private boolean validateDateRange() {
        LocalDate ngayBatDau = datePickerStart.getValue();
        LocalDate ngayKetThuc = datePickerEnd.getValue();
        if (ngayBatDau != null && ngayKetThuc != null) {
            if (ngayKetThuc.isBefore(ngayBatDau)) {
                showAlert(Alert.AlertType.WARNING, "Ngày không hợp lệ",
                "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.\n" +
                "Ngày bắt đầu: " + ngayBatDau.format(formatter) + "\n" +
                "Ngày kết thúc: " + ngayKetThuc.format(formatter));
                datePickerEnd.requestFocus();
                return false;
            }
            // Không được lưu khuyến mãi có ngày kết thúc trong quá khứ
            if (ngayKetThuc.isBefore(LocalDate.now())) {
                showAlert(Alert.AlertType.WARNING, "Ngày không hợp lệ",
                "Không thể tạo hoặc lưu khuyến mãi đã hết hạn (ngày kết thúc trong quá khứ).\nVui lòng chọn ngày kết thúc từ hôm nay trở đi.");
                datePickerEnd.requestFocus();
                return false;
            }
            // Giới hạn 1 năm
            if (java.time.temporal.ChronoUnit.DAYS.between(ngayBatDau, ngayKetThuc) > 365) {
                showAlert(Alert.AlertType.WARNING, "Thời gian quá dài",
                "Một khuyến mãi không được kéo dài quá 1 năm (365 ngày) để tránh sai sót.\nVui lòng kiểm tra lại năm kết thúc.");
                datePickerEnd.requestFocus();
                return false;
            }
        }
        return true;
    }

    // ==================== TIỆN ÍCH ====================
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
        private final LocalDate endDate; // Lưu để kiểm tra khi xóa

        public Promotion(String name, String code, LocalDate startDate, LocalDate endDate, String status, String value) {
            this.name = new SimpleStringProperty(name);
            this.code = new SimpleStringProperty(code);
            this.endDate = endDate; // giữ lại raw date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String startStr = (startDate != null) ? startDate.format(formatter) : "Chưa có";
            String endStr = (endDate != null) ? endDate.format(formatter) : "Chưa có";
            this.duration = new SimpleStringProperty(startStr + " - " + endStr);
            this.status = new SimpleStringProperty(status);
            this.value = new SimpleStringProperty(value);
        }

        public String getName() { return name.get(); }
        public String getCode() { return code.get(); }
        public String getDuration() { return duration.get(); }
        public String getStatus() { return status.get(); }
        public String getValue() { return value.get(); }
        public LocalDate getEndDate() { return endDate; }
    }
}