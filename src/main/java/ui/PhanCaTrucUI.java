package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.CaTruc;
import entity.NhanVien;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList; // Import FilteredList
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Import Map
import java.util.Optional;
import java.util.stream.Collectors;
public class PhanCaTrucUI {
    @FXML private GridPane gridPhanCa;
    @FXML private DatePicker datePicker;
    private LocalDate startOfWeek;
    // DAOs removed
    private ObservableList<NhanVien> activeNhanVienList;
    @FXML
    private void initialize() {
        startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        datePicker.setValue(startOfWeek);
        activeNhanVienList = FXCollections.observableArrayList();
        loadActiveEmployees();
        updateSchedule();
    }
    private void loadActiveEmployees() {
        Response res = Client.send(CommandType.GET_EMPLOYEES, null);
        if (res.getStatusCode() == 200) {
            List<NhanVien> list = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), NhanVien.class);
            activeNhanVienList.setAll(list.stream().filter(nv -> "Đang làm".equals(nv.getTrangThai())).collect(Collectors.toList()));
        }
    }
    // ... (Các hàm updateSchedule, createShiftGroupNode, createEmployeeListBox không đổi)
    private void updateSchedule() {
        gridPhanCa.getChildren().clear();
        List<CaTruc> caTrucTrongTuan = new ArrayList<>();
        Response res = Client.sendWithParams(CommandType.GET_WEEKLY_SHIFTS_ALL, Map.of("start", startOfWeek.toString()));
        if (res.getStatusCode() == 200) {
            caTrucTrongTuan = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), CaTruc.class);
        }
        String[] daysOfWeek = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
        String[] timeSlots = {"Sáng", "Chiều", "Tối"};
        // --- Tạo Header ---
        Label caLamHeader = new Label("Ca làm");
        caLamHeader.getStyleClass().add("time-slot-label");
        caLamHeader.setAlignment(Pos.CENTER);
        caLamHeader.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        gridPhanCa.add(caLamHeader, 0, 0);
        LocalDate currentDayHeader = startOfWeek;
        for (int i = 0; i < daysOfWeek.length; i++) {
            Label dayLabel = new Label(daysOfWeek[i] + "\n" + currentDayHeader.format(DateTimeFormatter.ofPattern("dd/MM")));
            dayLabel.getStyleClass().add("day-of-week-label");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            dayLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            gridPhanCa.add(dayLabel, i + 1, 0);
            currentDayHeader = currentDayHeader.plusDays(1);
        }
        for (int i = 0; i < timeSlots.length; i++) {
            Label timeLabel = new Label(timeSlots[i]);
            timeLabel.getStyleClass().add("time-slot-label");
            timeLabel.setAlignment(Pos.CENTER);
            timeLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            gridPhanCa.add(timeLabel, 0, i + 1);
        }
        // --- Kết thúc Header ---
        // --- Điền dữ liệu vào các ô ---
        for (int col = 0; col < daysOfWeek.length; col++) {
            LocalDate dateOfCell = startOfWeek.plusDays(col);
            for (int row = 0; row < timeSlots.length; row++) {
                String timeSlotOfCell = timeSlots[row];
                LocalTime startTimeSlot = getTimeSlotStartTime(timeSlotOfCell);
                LocalTime endTimeSlot = getTimeSlotEndTime(timeSlotOfCell);
                // Lọc ca trực cho ô hiện tại (khớp ngày và nằm trong khung giờ)
                // Lọc theo khung giờ rộng hơn để lấy các ca có thể bắt đầu/kết thúc lệch 1 chút
                List<CaTruc> shiftsInCell = caTrucTrongTuan.stream()
                .filter(ca -> ca.getNgay().equals(dateOfCell) &&
                ca.getGioBatDau().equals(startTimeSlot)) // Chỉ cần khớp giờ bắt đầu
                .collect(Collectors.toList());
                // VBox chứa toàn bộ nội dung của 1 ô
                VBox cellContainer = new VBox(5); // Khoảng cách giữa các ca khác giờ (nếu có)
                cellContainer.setAlignment(Pos.TOP_LEFT);
                cellContainer.getStyleClass().add("day-cell");
                cellContainer.setPadding(new Insets(8));
                if (!shiftsInCell.isEmpty()) {
                    // 🔥 ĐàSỬA: Nhóm các ca theo giờ làm việc CHÍNH XÁC
                    Map<String, List<CaTruc>> shiftsGroupedByTime = shiftsInCell.stream()
                    .collect(Collectors.groupingBy(ca ->
                    ca.getGioBatDau().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                    ca.getGioKetThuc().format(DateTimeFormatter.ofPattern("HH:mm"))
                    ));
                    // Tạo giao diện con cho từng nhóm giờ trong ô
                    shiftsGroupedByTime.forEach((timeString, shiftsGroup) -> {
                        // Tạo Label giờ làm
                        Label timeLabel = new Label(timeString);
                        timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a1a;");
                        // Lấy danh sách tên nhân viên
                        List<String> employeeNames = shiftsGroup.stream()
                        .map(ca -> ca.getNhanVien().getHoTen())
                        .distinct()
                        .collect(Collectors.toList());
                        String nameText = String.join(", ", employeeNames);
                        Label namesLabel = new Label(nameText);
                        namesLabel.getStyleClass().add("shift-employee-names");
                        namesLabel.setWrapText(false); // Không cho xuống dòng
                        namesLabel.setTextOverrun(OverrunStyle.ELLIPSIS); // Hiển thị "..." nếu tràn
                        namesLabel.setMaxWidth(Double.MAX_VALUE);
                        // Gom giờ và tên vào một VBox nhỏ cho mỗi ca
                        VBox shiftDisplayBox = new VBox(2); // Khoảng cách nhỏ giữa giờ và tên
                        shiftDisplayBox.getChildren().addAll(timeLabel, namesLabel);
                        shiftDisplayBox.setCursor(javafx.scene.Cursor.HAND);
                        shiftDisplayBox.setOnMouseClicked(e -> showEditShiftModal(shiftsGroup)); // Gắn sự kiện click
                        cellContainer.getChildren().add(shiftDisplayBox); // Thêm vào ô
                    });
                } else {
                // Ô trống -> nút Đăng ký
                Button registerButton = new Button("Đăng ký");
                registerButton.getStyleClass().add("register-button");
                final LocalDate finalDate = dateOfCell;
                final String finalTimeSlot = timeSlotOfCell; // "Sáng", "Chiều" hoặc "Tối"
                registerButton.setOnAction(e -> showRegisterModal(finalDate, finalTimeSlot));
                cellContainer.getChildren().add(registerButton);
            }
            gridPhanCa.add(cellContainer, col + 1, row + 1);
        }
    }
}
private VBox createShiftGroupNode(List<CaTruc> shiftsGroup) {
    if (shiftsGroup == null || shiftsGroup.isEmpty()) {
        return new VBox(); // Trả về VBox trống nếu không có ca
    }
    // Lấy thông tin chung từ ca đầu tiên
    CaTruc firstShift = shiftsGroup.get(0);
    LocalTime startTime = firstShift.getGioBatDau();
    LocalTime endTime = firstShift.getGioKetThuc();
    // Tạo Label giờ làm
    Label timeLabel = new Label(String.format("%s - %s",
    startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
    endTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
    timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a1a;"); // Tùy chỉnh style nếu muốn
    // Gọi hàm tạo danh sách tên
    VBox employeeListBox = createEmployeeListBox(shiftsGroup);
    // Gom lại trong VBox
    VBox groupNode = new VBox(4); // Giảm khoảng cách 1 chút
    groupNode.getChildren().addAll(timeLabel, employeeListBox); // Chỉ thêm giờ và danh sách tên
    groupNode.getStyleClass().add("shift-group-node");
    groupNode.setPadding(new Insets(5)); // Giữ lại padding nhỏ
    groupNode.setCursor(javafx.scene.Cursor.HAND);
    return groupNode;
}
private VBox createEmployeeListBox(List<CaTruc> shiftsGroup) {
    VBox vbox = new VBox(2); // Khoảng cách nhỏ giữa các tên
    vbox.setAlignment(Pos.CENTER_LEFT);
    for (CaTruc ca : shiftsGroup) {
        NhanVien nv = ca.getNhanVien();
        if (nv != null && nv.getHoTen() != null) {
            Label nameLabel = new Label("  " + nv.getHoTen()); // Thêm dấu  cho đẹp
            nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
            nameLabel.setWrapText(true); // 🔥 THÊM DÒNG NÀY ĐỂ CHO PHÉP XUỐNG DÒNG
            vbox.getChildren().add(nameLabel);
        }
    }
    return vbox;
}
// --- Hiển thị Modal Đăng ký ---
private void showRegisterModal(LocalDate date, String timeSlot) { // timeSlot là "Sáng", "Chiều", "Tối"
    Stage modalStage = new Stage();
    modalStage.initModality(Modality.APPLICATION_MODAL);
    modalStage.setTitle("Đăng ký ca trực mới");
    DatePicker dpStartDate = new DatePicker(date);
    ComboBox<Integer> cbStartHour = createHourComboBox();
    ComboBox<Integer> cbStartMinute = createMinuteComboBox();
    DatePicker dpEndDate = new DatePicker(date);
    ComboBox<Integer> cbEndHour = createHourComboBox();
    ComboBox<Integer> cbEndMinute = createMinuteComboBox();
    CheckBox cbSameDay = new CheckBox("Cùng ngày");
    cbSameDay.setSelected(true);
    TextArea taNotes = new TextArea();
    taNotes.setPromptText("(Optional)");
    taNotes.setPrefHeight(60);
    TextField tfSearchNV = new TextField();
    tfSearchNV.setPromptText("Gõ để tìm kiếm...");
    // 🔥 THÊM MỚI: Checkbox để lọc theo ca yêu thích
    CheckBox cbLocTheoYeuThich = new CheckBox("Chỉ hiển thị NV yêu thích ca này");
    cbLocTheoYeuThich.setSelected(false); // Mặc định tắt (hiển thị tất cả)
    ListView<CheckBox> lvNhanVien = new ListView<>();
    lvNhanVien.setPrefHeight(150);
    ObservableList<CheckBox> checkBoxes = FXCollections.observableArrayList();
    for (NhanVien nv : activeNhanVienList) {
        CheckBox cb = new CheckBox(nv.getHoTen() + " (" + nv.getMaNV() + ")");
        cb.setUserData(nv);
        checkBoxes.add(cb);
    }
    lvNhanVien.setItems(checkBoxes);
    FilteredList<CheckBox> filteredCheckBoxes = new FilteredList<>(checkBoxes, p -> true);
    // 🔥 CẬP NHẬT LISTENER: Kết hợp cả tìm kiếm và lọc
    Runnable updateFilter = () -> {
        String searchText = tfSearchNV.getText();
        boolean loc = cbLocTheoYeuThich.isSelected();
        filteredCheckBoxes.setPredicate(checkBox -> {
            NhanVien nv = (NhanVien) checkBox.getUserData();
            String caYeuThich = nv.getCaLamYeuThich(); // "Sáng", "Chiều", "Tối", "Nguyên ngày", null
            // 1. Lọc theo checkbox yêu thích
            boolean matchSlot = true; // Mặc định là khớp (nếu không lọc)
            if (loc) {
                if (caYeuThich == null) {
                    matchSlot = false; // Không có ca yêu thích -> ẩn
                } else {
                // Khớp nếu ca yêu thích là "Nguyên ngày" HOẶC khớp chính xác (Sáng/Chiều/Tối)
                matchSlot = caYeuThich.equals("Nguyên ngày") || caYeuThich.equals(timeSlot);
            }
        }
        // 2. Lọc theo ô tìm kiếm
        boolean matchSearch = true;
        if (searchText != null && !searchText.isEmpty() && !searchText.isBlank()) {
            matchSearch = checkBox.getText().toLowerCase().contains(searchText.toLowerCase().trim());
        }
        return matchSlot && matchSearch;
    });
};
// Gắn listener
tfSearchNV.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
cbLocTheoYeuThich.selectedProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
lvNhanVien.setItems(filteredCheckBoxes);
LocalTime defaultStartTime = getTimeSlotStartTime(timeSlot);
LocalTime defaultEndTime = getTimeSlotEndTime(timeSlot);
cbStartHour.setValue(defaultStartTime.getHour());
cbStartMinute.setValue(defaultStartTime.getMinute());
cbEndHour.setValue(defaultEndTime.getHour());
cbEndMinute.setValue(defaultEndTime.getMinute());
dpEndDate.disableProperty().bind(cbSameDay.selectedProperty());
cbSameDay.selectedProperty().addListener((obs, oldVal, newVal) -> {
    if (newVal) {
        dpEndDate.setValue(dpStartDate.getValue());
    }
});
dpStartDate.valueProperty().addListener((obs, oldVal, newVal) -> {
    if (cbSameDay.isSelected()) {
        dpEndDate.setValue(newVal);
    }
});
Button btnConfirm = new Button("Xác nhận");
btnConfirm.setDefaultButton(true);
btnConfirm.setOnAction(e -> {
    LocalDate startDate = dpStartDate.getValue();
    LocalDate endDate = dpEndDate.getValue();
    Integer startHour = cbStartHour.getValue();
    Integer startMinute = cbStartMinute.getValue();
    Integer endHour = cbEndHour.getValue();
    Integer endMinute = cbEndMinute.getValue();
    String notes = taNotes.getText();
    List<NhanVien> selectedNhanVien = new ArrayList<>();
    for (CheckBox cb : checkBoxes) { // Lấy từ 'checkBoxes' (danh sách gốc)
        if (cb.isSelected()) {
            selectedNhanVien.add((NhanVien) cb.getUserData());
        }
    }
    if (startDate == null || startHour == null || startMinute == null || endHour == null || endMinute == null || selectedNhanVien.isEmpty()) {
        showErrorAlert("Thiếu thông tin", "Vui lòng chọn ngày, giờ và ít nhất một nhân viên.");
        return;
    }
    if (endDate == null) endDate = startDate;
    LocalTime startTime = LocalTime.of(startHour, startMinute);
    LocalTime endTime = LocalTime.of(endHour, endMinute);
    boolean allSuccess = true;
    List<String> failedEmployees = new ArrayList<>();
    for (NhanVien nv : selectedNhanVien) {
        CaTruc newCaTruc = new CaTruc();
        newCaTruc.setMaCa(""); // Server will generate
        newCaTruc.setNgay(startDate);
        newCaTruc.setGioBatDau(startTime);
        newCaTruc.setGioKetThuc(endTime);
        newCaTruc.setNhanVien(nv);
        Response res = Client.sendWithParams(CommandType.UPDATE_SHIFT, Map.of("shift", newCaTruc));
        if (res.getStatusCode() != 200) {
            allSuccess = false;
            failedEmployees.add(nv.getHoTen());
        }
    }
    if (allSuccess) {
        showInfoAlert("Thành công", "Đã thêm ca trực cho các nhân viên được chọn.");
        modalStage.close();
        updateSchedule();
    } else {
    showErrorAlert("Lỗi", "Không thể thêm ca trực cho các nhân viên: " + String.join(", ", failedEmployees));
    updateSchedule();
}
});
Button btnCancel = new Button("Hủy");
btnCancel.setCancelButton(true);
btnCancel.setOnAction(e -> modalStage.close());
GridPane timePane = new GridPane();
timePane.setHgap(10); timePane.setVgap(10);
timePane.add(new Label("Bắt đầu:"), 0, 0); timePane.add(cbStartHour, 1, 0); timePane.add(new Label(":"), 2, 0); timePane.add(cbStartMinute, 3, 0); timePane.add(dpStartDate, 4, 0);
timePane.add(new Label("Kết thúc:"), 0, 1); timePane.add(cbEndHour, 1, 1); timePane.add(new Label(":"), 2, 1); timePane.add(cbEndMinute, 3, 1); timePane.add(dpEndDate, 4, 1); timePane.add(cbSameDay, 5, 1);
VBox modalRoot = new VBox(15);
modalRoot.setPadding(new Insets(20));
// 🔥 Gộp ô tìm kiếm và checkbox lọc
HBox searchPane = new HBox(10, tfSearchNV, cbLocTheoYeuThich);
HBox.setHgrow(tfSearchNV, Priority.ALWAYS);
searchPane.setAlignment(Pos.CENTER_LEFT);
modalRoot.getChildren().addAll(
new Label("Ngày: " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - Khung giờ: " + timeSlot), new Separator(),
timePane, new Label("Ghi chú:"), taNotes, new Label("Nhân viên:"),
searchPane, // Thêm HBox tìm kiếm
lvNhanVien,
new HBox(10, btnConfirm, btnCancel) {{ setAlignment(Pos.CENTER_RIGHT); }}
);
modalStage.setScene(new Scene(modalRoot, 500, 600));
modalStage.showAndWait();
}
// ... (Các hàm showEditShiftModal, createHour/MinuteComboBox, getTimeSlotStart/EndTime, ... không đổi)
private void showEditShiftModal(List<CaTruc> existingShifts) {
    if (existingShifts == null || existingShifts.isEmpty()) return;
    Stage modalStage = new Stage();
    modalStage.initModality(Modality.APPLICATION_MODAL);
    modalStage.setTitle("Thông tin ca trực");
    CaTruc firstShift = existingShifts.get(0);
    LocalDate date = firstShift.getNgay();
    LocalTime startTime = firstShift.getGioBatDau();
    LocalTime endTime = firstShift.getGioKetThuc();
    Label lblDate = new Label(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    Label lblTime = new Label(String.format("%s - %s",
    startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
    endTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
    TextArea taNotes = new TextArea();
    taNotes.setPromptText("Ghi chú (chưa hoạt động)");
    taNotes.setPrefHeight(60);
    ListView<CheckBox> lvCurrentNV = new ListView<>();
    lvCurrentNV.setPrefHeight(150);
    ObservableList<CheckBox> currentCheckBoxes = FXCollections.observableArrayList();
    for (CaTruc ca : existingShifts) {
        NhanVien nv = ca.getNhanVien();
        CheckBox cb = new CheckBox(nv.getHoTen() + " (" + nv.getMaNV() + ")");
        cb.setUserData(ca);
        cb.setSelected(true);
        currentCheckBoxes.add(cb);
    }
    lvCurrentNV.setItems(currentCheckBoxes);
    lvCurrentNV.setCellFactory(param -> new ListCell<CheckBox>() {
        @Override
        protected void updateItem(CheckBox item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
            setGraphic(item);
            item.setOnAction(event -> {
                if (!item.isSelected()) {
                    CaTruc caToRemove = (CaTruc) item.getUserData();
                    if (caToRemove != null) {
                        Alert confirmRemove = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmRemove.setTitle("Xác nhận");
                        confirmRemove.setContentText("Xóa nhân viên " + caToRemove.getNhanVien().getHoTen() + " khỏi ca này?");
                        Optional<ButtonType> result = confirmRemove.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            if (Client.sendWithParams(CommandType.DELETE_SHIFT, Map.of("maCa", caToRemove.getMaCa())).getStatusCode() == 200) {
                                currentCheckBoxes.remove(item);
                            } else {
                            showErrorAlert("Lỗi", "Không thể xóa nhân viên khỏi ca.");
                            item.setSelected(true);
                        }
                    } else {
                    item.setSelected(true);
                }
            }
        }
    });
}
}
});
TextField tfSearchAddNV = new TextField();
tfSearchAddNV.setPromptText("Tìm kiếm để thêm...");
ListView<CheckBox> lvAvailableNV = new ListView<>();
lvAvailableNV.setPrefHeight(150);
ObservableList<CheckBox> availableCheckBoxes = FXCollections.observableArrayList();
FilteredList<CheckBox> filteredAvailableCB = new FilteredList<>(availableCheckBoxes, p -> true);
lvAvailableNV.setItems(filteredAvailableCB);
Runnable updateAvailableList = () -> {
    List<String> currentMaNVs = currentCheckBoxes.stream()
    .map(cb -> ((CaTruc) cb.getUserData()).getNhanVien().getMaNV())
    .collect(Collectors.toList());
    availableCheckBoxes.clear();
    for (NhanVien nv : activeNhanVienList) {
        if (!currentMaNVs.contains(nv.getMaNV())) {
            CheckBox cb = new CheckBox(nv.getHoTen() + " (" + nv.getMaNV() + ")");
            cb.setUserData(nv);
            availableCheckBoxes.add(cb);
        }
    }
};
updateAvailableList.run();
currentCheckBoxes.addListener((javafx.collections.ListChangeListener<CheckBox>) c -> updateAvailableList.run());
tfSearchAddNV.textProperty().addListener((obs, oldVal, newVal) -> {
    filteredAvailableCB.setPredicate(checkBox -> {
        if (newVal == null || newVal.isEmpty() || newVal.isBlank()) return true;
        String lower = newVal.toLowerCase().trim();
        return checkBox.getText().toLowerCase().contains(lower);
    });
});
Button btnConfirm = new Button("Xác nhận");
btnConfirm.setDefaultButton(true);
btnConfirm.setOnAction(e -> {
    List<NhanVien> employeesToAdd = new ArrayList<>();
    for(CheckBox cb : availableCheckBoxes) {
        if(cb.isSelected()){
            employeesToAdd.add((NhanVien) cb.getUserData());
        }
    }
    boolean allAddSuccess = true;
    List<String> failedAdd = new ArrayList<>();
    for (NhanVien nvToAdd : employeesToAdd) {
        CaTruc newCaTruc = new CaTruc("", date, startTime, endTime, nvToAdd);
        Response res = Client.sendWithParams(CommandType.UPDATE_SHIFT, Map.of("shift", newCaTruc));
        if (res.getStatusCode() != 200) {
            allAddSuccess = false;
            failedAdd.add(nvToAdd.getHoTen());
        }
    }
    if(failedAdd.isEmpty()) {
        if(!employeesToAdd.isEmpty()) {
            showInfoAlert("Thành công", "Đã cập nhật danh sách nhân viên cho ca.");
        }
        modalStage.close();
        updateSchedule();
    } else {
    showErrorAlert("Lỗi", "Không thể thêm các NV: " + String.join(", ", failedAdd));
    updateSchedule();
}
});
Button btnDeleteShift = new Button("Xóa Ca");
btnDeleteShift.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
btnDeleteShift.setOnAction(e -> {
    Alert confirmDelete = new Alert(Alert.AlertType.CONFIRMATION);
    confirmDelete.setTitle("Xác nhận xóa ca");
    confirmDelete.setContentText("Bạn có chắc muốn xóa toàn bộ ca trực này ("+ currentCheckBoxes.size() +" nhân viên)?");
    Optional<ButtonType> result = confirmDelete.showAndWait();
    if(result.isPresent() && result.get() == ButtonType.OK) {
        boolean allDeleteSuccess = true;
        for(CheckBox cb : currentCheckBoxes) {
            CaTruc ca = (CaTruc) cb.getUserData();
            if(Client.sendWithParams(CommandType.DELETE_SHIFT, Map.of("maCa", ca.getMaCa())).getStatusCode() != 200) {
                allDeleteSuccess = false;
            }
        }
        if(allDeleteSuccess) {
            showInfoAlert("Thành công", "Đã xóa ca trực.");
            modalStage.close();
            updateSchedule();
        } else {
        showErrorAlert("Lỗi", "Xảy ra lỗi khi xóa ca trực.");
    }
}
});
Button btnCancel = new Button("Đóng");
btnCancel.setCancelButton(true);
btnCancel.setOnAction(e -> modalStage.close());
VBox editModalRoot = new VBox(15);
editModalRoot.setPadding(new Insets(20));
editModalRoot.getChildren().addAll(
new HBox(10, new Label("Ngày:"), lblDate, new Label("Giờ:"), lblTime), new Separator(),
new Label("Nhân viên hiện tại trong ca (Bỏ tick để xóa):"), lvCurrentNV,
new Label("Thêm nhân viên khác vào ca (Tick để thêm):"), tfSearchAddNV, lvAvailableNV,
new HBox(10, btnConfirm, btnDeleteShift, btnCancel) {{ setAlignment(Pos.CENTER_RIGHT); }}
);
modalStage.setScene(new Scene(editModalRoot, 500, 650));
modalStage.showAndWait();
}
private ComboBox<Integer> createHourComboBox() { ComboBox<Integer> cb = new ComboBox<>(); for (int i = 0; i < 24; i++) cb.getItems().add(i); return cb; }
private ComboBox<Integer> createMinuteComboBox() { ComboBox<Integer> cb = new ComboBox<>(); cb.getItems().addAll(0, 15, 30, 45); return cb; }
private LocalTime getTimeSlotStartTime(String timeSlot) { switch (timeSlot.toLowerCase()) { case "sáng": return LocalTime.of(8, 0); case "chiều": return LocalTime.of(14, 0); case "tối": return LocalTime.of(18, 0); default: return LocalTime.MIDNIGHT; } }
private LocalTime getTimeSlotEndTime(String timeSlot) { switch (timeSlot.toLowerCase()) { case "sáng": return LocalTime.of(14, 0); case "chiều": return LocalTime.of(22, 0); case "tối": return LocalTime.of(2, 0); default: return LocalTime.MAX; } }
@FXML private void previousWeek() { startOfWeek = startOfWeek.minusWeeks(1); datePicker.setValue(startOfWeek); updateSchedule(); }
@FXML private void nextWeek() { startOfWeek = startOfWeek.plusWeeks(1); datePicker.setValue(startOfWeek); updateSchedule(); }
@FXML private void handleCurrentWeek() { startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY); datePicker.setValue(startOfWeek); updateSchedule(); }
@FXML private void updateCalendarFromDatePicker() { LocalDate selectedDate = datePicker.getValue(); if (selectedDate != null) { startOfWeek = selectedDate.with(DayOfWeek.MONDAY); updateSchedule(); } }
private void showErrorAlert(String title, String content) { Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait(); }); }
private void showInfoAlert(String title, String content) { Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait(); }); }
}