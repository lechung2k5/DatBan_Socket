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
import java.util.stream.Collectors;

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

        // 🔥 LẮNG NGHE REAL-TIME
        network.RealTimeClient.getInstance().addListener(event -> {
            if (event.getType() == CommandType.UPDATE_PROMO) {
                javafx.application.Platform.runLater(this::loadDataFromDatabase);
            }
        });
    }

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
                    if (promo != null) xoaKhuyenMai(promo);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
                if (deleteButton != null) deleteButton.setDisable(isEditMode);
            }
        });
    }

    private void setupFilterComboBox() {
        filterComboBox.setItems(FXCollections.observableArrayList("Tất cả", "Đang áp dụng", "Sắp diễn ra", "Đã hết hạn"));
        filterComboBox.setValue("Tất cả");
        filterComboBox.setOnAction(e -> filterPromotions(filterComboBox.getValue()));
    }

    private void setupButtonEvents() {
        btnThem.setOnAction(e -> themKhuyenMai());
        btnSua.setOnAction(e -> chinhSuaKhuyenMai());
        btnLuu.setOnAction(e -> luuKhuyenMai());
    }

    private void setupInputValidation() {
        txtTenKM.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 100) txtTenKM.setText(oldVal);
        });
        txtGiaTri.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 10) txtGiaTri.setText(oldVal);
        });
    }

    private void setupTableSelectionEvent() {
        tblKhuyenMai.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isEditMode) {
                selectedPromotion = newVal;
                loadPromotionToForm(newVal);
            }
        });
    }

    private void loadDataFromDatabase() {
        try {
            Response res = Client.send(CommandType.GET_PROMOS, null);
            if (res.getStatusCode() == 200) {
                List<UuDai> uuDaiList = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), UuDai.class);
                promotionList.clear();
                for (UuDai ud : uuDaiList) {
                    promotionList.add(new Promotion(
                        ud.getTenUuDai(),
                        ud.getMaUuDai(),
                        ud.getNgayBatDau(),
                        ud.getNgayKetThuc(),
                        ud.getTrangThai(),
                        String.format("%.0f%%", ud.getGiaTri())
                    ));
                }
                tblKhuyenMai.setItems(promotionList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void themKhuyenMai() {
        if (isEditMode) return;
        clearForm();
        setFormEditable(true);
        isEditMode = true;
        selectedPromotion = null;
        
        int maxNum = 0;
        for (Promotion p : promotionList) {
            if (p.getCode() != null && p.getCode().matches("KM\\d+")) {
                int num = Integer.parseInt(p.getCode().substring(2));
                if (num > maxNum) maxNum = num;
            }
        }
        currentMaKM = String.format("KM%03d", maxNum + 1);
        updateButtonStates(true, true, false);
        updateTableState(true);
        showAlert(Alert.AlertType.INFORMATION, "Thêm mới", "Mã tự động: " + currentMaKM);
    }

    private void chinhSuaKhuyenMai() {
        if (selectedPromotion == null) return;
        isEditMode = true;
        currentMaKM = selectedPromotion.getCode();
        setFormEditable(true);
        updateButtonStates(true, true, false);
        updateTableState(true);
    }

    private void luuKhuyenMai() {
        if (!validateAllInput()) return;
        
        UuDai ud = new UuDai();
        ud.setMaUuDai(currentMaKM);
        ud.setTenUuDai(txtTenKM.getText().trim());
        ud.setGiaTri(Double.parseDouble(txtGiaTri.getText().trim()));
        ud.setNgayBatDau(datePickerStart.getValue());
        ud.setNgayKetThuc(datePickerEnd.getValue());
        
        Response res = Client.sendWithParams(CommandType.UPDATE_PROMO, Map.of("promo", ud));
        if (res.getStatusCode() == 200) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu khuyến mãi.");
            loadDataFromDatabase();
            resetFormState();
        }
    }

    private void xoaKhuyenMai(Promotion promo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa " + promo.getName() + "?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Response res = Client.sendWithParams(CommandType.DELETE_PROMO, Map.of("id", promo.getCode()));
            if (res.getStatusCode() == 200) {
                loadDataFromDatabase();
                resetFormState();
            }
        }
    }

    private void filterPromotions(String filter) {
        if (filter.equals("Tất cả")) {
            tblKhuyenMai.setItems(promotionList);
        } else {
            tblKhuyenMai.setItems(FXCollections.observableArrayList(
                promotionList.stream().filter(p -> p.getStatus().equals(filter)).collect(Collectors.toList())
            ));
        }
    }

    private void loadPromotionToForm(Promotion promo) {
        Response res = Client.sendWithParams(CommandType.GET_PROMO_BY_ID, Map.of("id", promo.getCode()));
        if (res.getStatusCode() == 200) {
            UuDai ud = JsonUtil.fromJson(JsonUtil.toJson(res.getData()), UuDai.class);
            txtTenKM.setText(ud.getTenUuDai());
            txtGiaTri.setText(String.valueOf(ud.getGiaTri()));
            datePickerStart.setValue(ud.getNgayBatDau());
            datePickerEnd.setValue(ud.getNgayKetThuc());
        }
    }

    private void clearForm() {
        txtTenKM.clear(); txtGiaTri.clear();
        datePickerStart.setValue(null); datePickerEnd.setValue(null);
    }

    private void resetFormState() {
        clearForm(); setFormEditable(false);
        isEditMode = false; updateButtonStates(false, false, true);
        updateTableState(false);
    }

    private void setFormEditable(boolean editable) {
        txtTenKM.setEditable(editable); txtGiaTri.setEditable(editable);
        datePickerStart.setDisable(!editable); datePickerEnd.setDisable(!editable);
    }

    private void updateButtonStates(boolean dThem, boolean dSua, boolean dLuu) {
        btnThem.setDisable(dThem); btnSua.setDisable(dSua); btnLuu.setDisable(dLuu);
    }

    private void updateTableState(boolean disable) {
        tblKhuyenMai.setDisable(disable); filterComboBox.setDisable(disable);
    }

    private boolean validateAllInput() {
        try {
            if (txtTenKM.getText().isEmpty()) return false;
            double v = Double.parseDouble(txtGiaTri.getText());
            if (v < 0 || v > 100) return false;
            if (datePickerStart.getValue() == null || datePickerEnd.getValue() == null) return false;
            if (datePickerEnd.getValue().isBefore(datePickerStart.getValue())) return false;
            return true;
        } catch (Exception e) { return false; }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type); a.setTitle(title); a.setContentText(content); a.showAndWait();
    }

    public static class Promotion {
        private final SimpleStringProperty name, code, duration, status, value;
        private final LocalDate startDate, endDate;

        public Promotion(String name, String code, LocalDate start, LocalDate end, String status, String value) {
            this.name = new SimpleStringProperty(name);
            this.code = new SimpleStringProperty(code);
            this.startDate = start;
            this.endDate = end;
            this.duration = new SimpleStringProperty(start + " - " + end);
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