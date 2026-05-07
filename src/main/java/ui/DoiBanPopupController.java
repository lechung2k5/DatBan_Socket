package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.*;
import entity.TrangThaiBan;
import entity.TrangThaiHoaDon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
public class DoiBanPopupController {
    @FXML private Label lblMaHDGoc;
    @FXML private Label lblSoBanHienTai;
    @FXML private Label lblDanhSachBanCu;
    @FXML private DatePicker datePickerThoiGianMoi;
    @FXML private TextField txtThoiGianMoi;
    @FXML private Button btnTimBanTrong;
    @FXML private Label lblThongTinChonBan;
    @FXML private ListView<Ban> listViewBanTrong;
    @FXML private Button btnHuy;
    @FXML private Button btnXacNhanDoi;
    private List<HoaDon> hoaDonGocVaPhu;
    private DatBan mainController;
    private ObservableList<Ban> banTrongList = FXCollections.observableArrayList();
    private Map<Ban, BooleanProperty> selectionMap = new HashMap<>(); // Map lưu trạng thái chá»n
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private int soBanCanChon = 0;
    // === HELPER Cáº¦N THIáº¾T Tá»ª DATBAN (ÄÆ¯á»¢C Gá»I QUA mainController) ===
    // Äá» Controller nÃ y có thá» gá»i các hÃ m phức tạp trong DatBan
    // (Giả Äá»nh mainController có các hÃ m sau, nếu không, ta phải tạo interface hoáº·c dùng Reflection)
    // CÃCH Tá»T NHáº¤T: Bá» sung thêm các phương thức public cáº§n thiết vÃ o DatBan.java
    // Cáº§n có má»t Set các mã bÃ n cũ Äá» kiá»m tra
    private Set<String> maBanCuSet;
    @FXML
    private void initialize() {
        listViewBanTrong.setItems(banTrongList);
        listViewBanTrong.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // === KHÃI PHá»¤C LIST CELL Sá»¬ Dá»¤NG CHECKBOX VÃ BINDING Vá»I selectionMap ===
        listViewBanTrong.setCellFactory(new Callback<ListView<Ban>, ListCell<Ban>>() {
            @Override
            public ListCell<Ban> call(ListView<Ban> lv) {
                ListCell<Ban> cell = new ListCell<Ban>() {
                    private final CheckBox checkBox = new CheckBox();
                    private final Label label = new Label();
                    private final HBox hbox = new HBox(5, checkBox, label);
                    {
                        hbox.setAlignment(Pos.CENTER_LEFT);
                        // Khi Checkbox Äưá»£c click, cáº­p nháº­t trạng thái trong Map
                        checkBox.setOnAction(event -> {
                            if (getItem() != null) {
                                // Cáº­p nháº­t BooleanProperty trong Map
                                selectionMap.get(getItem()).set(checkBox.isSelected());
                                updateXacNhanButtonState(); // Cáº­p nháº­t trạng thái nút
                            }
                        });
                    }
                    @Override
                    protected void updateItem(Ban item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                        // Láº¥y/Tạo BooleanProperty vÃ  cáº­p nháº­t CheckBox
                        BooleanProperty selected = selectionMap.computeIfAbsent(item, k -> new SimpleBooleanProperty(false));
                        // Sá»¬A Lá»I CÃ PHÃP: Dùng getDisplayName()
                        label.setText(item.getMaBan() + " (" + item.getSucChua() + " chá») - " + item.getTrangThai().getDisplayName());
                        // Set trạng thái của Checkbox từ Map
                        checkBox.setSelected(selected.get());
                        // === LOGIC VÃ HIá»U HÃA CHECKBOX VÃ BÃN Báº¬N (FIX Lá»I B010) ===
                        boolean laBanCu = maBanCuSet != null && maBanCuSet.contains(item.getMaBan());
                        // BÃ n bá» coi lÃ  Báº¬N nếu trạng thái logic (logic 4/8 tiếng) không phải lÃ  TRONG
                        boolean isCurrentlyBusy = item.getTrangThai() != TrangThaiBan.TRONG;
                        // Chá» cho phÃ©p chá»n nếu (Nó lÃ  BÃ n cũ) HOáº¶C (Nó hoÃ n toÃ n TRá»NG)
                        // Nếu isCurrentlyBusy=true, vÃ  nó KHÃNG phải bÃ n cũ -> DISABLE
                        checkBox.setDisable(isCurrentlyBusy && !laBanCu);
                        // Set mÃ u ná»n (tùy chá»n)
                        if (isCurrentlyBusy) {
                            setStyle("-fx-background-color: #fce4e4; -fx-opacity: 0.8;"); // MÃ u nhạt cho bÃ n báº­n
                        } else {
                        setStyle("");
                    }
                    setGraphic(hbox);
                }
            }
        };
        return cell;
    }
});
// ===============================================
// Gán sự kiá»n cho các nút
btnTimBanTrong.setOnAction(e -> handleTimBanTrong());
btnXacNhanDoi.setOnAction(e -> handleXacNhanDoi());
btnHuy.setOnAction(e -> closePopup());
btnXacNhanDoi.setDisable(true);
}
/**
* Nháº­n dữ liá»u ban Äáº§u từ mÃ n hình DatBan
*/
public void setInitialData(List<HoaDon> hoaDonGocVaPhu, DatBan mainController) {
    this.hoaDonGocVaPhu = hoaDonGocVaPhu;
    this.mainController = mainController;
    // Láº¥y mã bÃ n cũ má»t láº§n duy nháº¥t
    this.maBanCuSet = hoaDonGocVaPhu.stream()
    .filter(hd -> hd.getBan() != null)
    .map(hd -> hd.getBan().getMaBan())
    .collect(Collectors.toSet());
    // Hiá»n thá» thông tin HÄ hiá»n tại
    HoaDon hdGoc = hoaDonGocVaPhu.stream().filter(h -> h.getMaHDGoc() == null).findFirst().orElse(null);
    if (hdGoc != null) {
        lblMaHDGoc.setText(hdGoc.getMaHD());
        soBanCanChon = hoaDonGocVaPhu.size(); // Sá» bÃ n cáº§n chá»n = sá» bÃ n hiá»n tại
        lblSoBanHienTai.setText(String.valueOf(soBanCanChon));
        String dsBanCu = hoaDonGocVaPhu.stream()
        .map(h -> h.getBan() != null ? h.getBan().getMaBan() : "?")
        .collect(Collectors.joining(", "));
        lblDanhSachBanCu.setText(dsBanCu);
        lblThongTinChonBan.setText(String.format("Chá»n Äúng %d bÃ n trá»ng dưá»i Äây:", soBanCanChon));
        // Äáº·t thá»i gian máº·c Äá»nh lÃ  giá» vÃ o hiá»n tại của HÄ Gá»c
        if (hdGoc.getGioVao() != null) {
            datePickerThoiGianMoi.setValue(hdGoc.getGioVao().toLocalDate());
            txtThoiGianMoi.setText(hdGoc.getGioVao().toLocalTime().format(timeFormatter));
        } else {
        // Hoáº·c giá» hiá»n tại nếu HÄ không có giá» vÃ o
        datePickerThoiGianMoi.setValue(LocalDate.now());
        txtThoiGianMoi.setText(LocalTime.now().format(timeFormatter));
    }
    // Tự Äá»ng tìm bÃ n trá»ng láº§n Äáº§u
    handleTimBanTrong();
} else {
// Xá»­ lý lá»i nếu không tìm tháº¥y HÄ Gá»c
showAlert(AlertType.ERROR, "Lá»i Dữ liá»u", "Không tìm tháº¥y Hóa Äơn Gá»c trong danh sách truyá»n vÃ o.");
closePopup();
}
}
// DoiBanPopupController.java (Chá» pháº§n hÃ m handleTimBanTrong Äưá»£c sá»­a)
/**
* Xá»­ lý khi nháº¥n nút "Tìm bÃ n trá»ng"
*/
private void handleTimBanTrong() {
    LocalDate ngayMoi = datePickerThoiGianMoi.getValue();
    String gioMoiStr = txtThoiGianMoi.getText();
    LocalTime gioMoi;
    try {
        if (ngayMoi == null || gioMoiStr == null || gioMoiStr.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Thiếu thá»i gian", "Vui lòng nháº­p NgÃ y vÃ  Giá» má»i.");
            return;
        }
        gioMoi = LocalTime.parse(gioMoiStr, timeFormatter);
    } catch (Exception e) {
    showAlert(AlertType.ERROR, "Lá»i Äá»nh dạng", "Giá» nháº­p không há»£p lá» (cáº§n HH:mm).");
    return;
}
// Cáº§n tải lại ds HÄ Äang chá» cho logic hiá»n thá» trạng thái chÃ­nh xác
mainController.loadDsHoaDonDatTrongNgay(ngayMoi);
banTrongList.clear();
selectionMap.clear();
// --- Láº¥y mã bÃ n cũ ---
Set<String> maBanCuSet = hoaDonGocVaPhu.stream()
.filter(hd -> hd.getBan() != null)
.map(hd -> hd.getBan().getMaBan())
.collect(Collectors.toSet());
// -----------------------
// ð¥ FIX: Láº¥y Táº¤T Cáº¢ các bÃ n vÃ  kiá»m tra trạng thái Báº¬N Cá»¨NG (DAO Äã xá»­ lý loại trừ HÄ hiá»n tại)
// GIáº¢ Äá»NH: DAO.getAllBanWithAvailability ÄÃ ÄÆ¯á»¢C FIX Äá» LOáº I TRá»ª Cá»¤M HÄ ÄANG XEM.
// DO KHÃNG CÃ CODE DAO Äáº¦Y Äá»¦ á» ÄÃY, CHÃNG TA PHáº¢I Gá»I DAO Äá» Láº¤Y Táº¤T Cáº¢ BÃN
// VÃ DÃNG LOGIC MÃU Cá»¦A CONTROLLER CHÃNH
Response res = Client.send(CommandType.GET_TABLES, null);
List<Ban> tatCaBan;
if (res.getStatusCode() == 200) {
    tatCaBan = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), Ban.class);
} else {
tatCaBan = FXCollections.observableArrayList();
}
for (Ban ban : tatCaBan) {
    // 1. Láº¥y trạng thái Báº¬N (logic 4/8 tiếng) tại thá»i Äiá»m má»i
    TrangThaiBan trangThaiLogic = mainController.getTrangThaiHienThi(ban, gioMoi);
    // 2. Tạo Äá»i tưá»£ng Ban má»i Äá» hiá»n thá» trong ListView
    Ban banHienThi = new Ban(ban.getMaBan(), ban.getViTri(), ban.getSucChua(), ban.getLoaiBan(), ban.getTrangThai());
    // ð¥ LOGIC Má»I: Chá» loại trừ bÃ n cũ khá»i danh sách Báº¬N.
    if (maBanCuSet.contains(ban.getMaBan())) {
        // Nếu lÃ  bÃ n cũ -> luôn set lÃ  TRá»NG Äá» cho phÃ©p chá»n (dù logic 4/8 tiếng báo Äá»/CAM)
        banHienThi.setTrangThai(TrangThaiBan.TRONG);
    } else if (trangThaiLogic != TrangThaiBan.TRONG) {
    // Nếu lÃ  bÃ n má»i vÃ  Báº­n theo Logic 4/8 tiếng -> giữ trạng thái Báº¬N
    banHienThi.setTrangThai(trangThaiLogic);
} else {
// Ngưá»£c lại -> TRá»NG
banHienThi.setTrangThai(TrangThaiBan.TRONG);
}
banTrongList.add(banHienThi);
// Khá»i tạo trạng thái chá»n: Tá»° Äá»NG TICK BÃN CÅ¨
boolean isSelected = maBanCuSet.contains(banHienThi.getMaBan());
BooleanProperty prop = new SimpleBooleanProperty(isSelected);
selectionMap.put(banHienThi, prop);
}
// Cáº§n refresh ListView Äá» ListCellFactory cáº­p nháº­t Checkbox
listViewBanTrong.refresh();
updateXacNhanButtonState();
lblThongTinChonBan.setText(String.format("Chá»n Äúng %d bÃ n trá»ng muá»n Äá»i Äến:", soBanCanChon));
}
/**
* ð¥ HÃM ÄÃ Sá»¬A: Xá»­ lý xác nháº­n Äá»i bÃ n (Bao gá»m các trưá»ng há»£p: 1-1, N-N, N-M, N-1)
* ÄÃ Tá»I Æ¯U: Loại bá» vòng láº·p cáº­p nháº­t HÄ cũ vÃ  thay báº±ng các bưá»c Cáº­p nháº­t/Xóa rõ rÃ ng.
*/
/**
* ð¥ HÃM ÄÃ Sá»¬A: Xá»­ lý xác nháº­n Äá»i bÃ n (Bao gá»m các trưá»ng há»£p: 1-1, N-N, N-M, N-1)
* ÄÃ Sá»¬A Lá»I: Gá»i hÃ m DAO má»i Äá» cáº­p nháº­t trạng thái NHIá»U bÃ n cùng lúc.
*/
private void handleXacNhanDoi() {
    // ... (pháº§n code láº¥y selectedBanMoi, hoaDonGoc, maBanCuList, hoaDonPhuCuList, maBanMoiSet, maBanMoiGoc) ...
    List<Ban> selectedBanMoi = selectionMap.entrySet().stream()
    .filter(entry -> entry.getValue().get())
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
    if (selectedBanMoi.isEmpty()) {
        showAlert(AlertType.ERROR, "Chưa chá»n bÃ n", "Vui lòng chá»n Ã­t nháº¥t má»t bÃ n má»i.");
        return;
    }
    // Kiá»m tra bÃ n không Äưá»£c báº­n (chá» kiá»m tra các bÃ n má»i không phải lÃ  bÃ n cũ)
    for (Ban ban : selectedBanMoi) {
        if (ban.getTrangThai() != TrangThaiBan.TRONG && !maBanCuSet.contains(ban.getMaBan())) {
            showAlert(AlertType.ERROR, "Lá»i", "BÃ n " + ban.getMaBan() + " Äang báº­n. Vui lòng bá» chá»n bÃ n nÃ y.");
            return;
        }
    }
    HoaDon hoaDonGoc = hoaDonGocVaPhu.stream().filter(h -> h.getMaHDGoc() == null).findFirst().orElse(null);
    if (hoaDonGoc == null) return;
    // Láº¥y mã bÃ n cũ vÃ  các HÄ phụ
    List<String> maBanCuList = hoaDonGocVaPhu.stream().map(h -> h.getBan() != null ? h.getBan().getMaBan() : null)
    .filter(Objects::nonNull).collect(Collectors.toList());
    List<HoaDon> hoaDonPhuCuList = hoaDonGocVaPhu.stream().filter(h -> h.getMaHDGoc() != null).collect(Collectors.toList());
    Set<String> maBanMoiSet = selectedBanMoi.stream().map(Ban::getMaBan).collect(Collectors.toSet());
    // BÃ n má»i gá»c lÃ  bÃ n Äáº§u tiên Äưá»£c chá»n
    String maBanMoiGoc = selectedBanMoi.get(0).getMaBan();
    String maHDGoc = hoaDonGoc.getMaHD();
    // --- Kiá»m tra trùng láº·p (Giữ nguyên) ---
    if (new HashSet<>(maBanCuList).equals(maBanMoiSet) && maBanCuList.size() == maBanMoiSet.size()) {
        showAlert(AlertType.WARNING, "Không Äá»i", "BÃ n cũ vÃ  bÃ n má»i giá»ng nhau.");
        return;
    }
    // ----------------------------------------
    Optional<ButtonType> result = showAlertConfirm("Xác nháº­n Äá»i bÃ n",
    String.format("Bạn có cháº¯c cháº¯n muá»n Äá»i các bÃ n %s sang các bÃ n %s không?\n\n"
    + "Lưu ý: Các hóa Äơn phụ/bÃ n cũ không còn Äưá»£c liên kết sáº½ bá» hủy/giải phóng.",
    String.join(", ", maBanCuList), String.join(", ", maBanMoiSet)));
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            Timestamp thoiGianDoiMoi = Timestamp.valueOf(datePickerThoiGianMoi.getValue().atTime(LocalTime.parse(txtThoiGianMoi.getText(), timeFormatter)));
            // 1. XÃA Táº¤T Cáº¢ HÃA ÄÆ N PHá»¤ CÅ¨ QUA API
            for (HoaDon hdPhu : hoaDonPhuCuList) {
                Client.sendWithParams(CommandType.CANCEL_INVOICE, Map.of(
                "maHD", hdPhu.getMaHD(),
                "trangThai", "DaHuy"
                ));
            }
            // 2. Cáº¬P NHáº¬T HÃA ÄÆ N Gá»C QUA API
            Client.sendWithParams(CommandType.UPDATE_INVOICE, Map.of(
            "maHD", maHDGoc,
            "maBan", maBanMoiGoc,
            "gioVao", thoiGianDoiMoi.toString()
            ));
            // 3. Táº O HÃA ÄÆ N PHá»¤ Má»I (Nếu có nhiá»u hơn 1 bÃ n má»i)
            if (selectedBanMoi.size() > 1) {
                for (int i = 1; i < selectedBanMoi.size(); i++) {
                    Ban banMoi = selectedBanMoi.get(i);
                    HoaDon hoaDonPhuMoi = new HoaDon();
                    hoaDonPhuMoi.setNgayLap(java.time.LocalDateTime.now());
                    hoaDonPhuMoi.setGioVao(thoiGianDoiMoi.toLocalDateTime());
                    hoaDonPhuMoi.setKhachHang(hoaDonGoc.getKhachHang());
                    hoaDonPhuMoi.setBan(banMoi);
                    hoaDonPhuMoi.setTienCoc(0);
                    hoaDonPhuMoi.setMaHDGoc(maHDGoc);
                    hoaDonPhuMoi.setTrangThai(TrangThaiHoaDon.HOA_DON_TAM.getDbValue());
                    Client.sendWithParams(CommandType.CREATE_ORDER, Map.of(
                    "hoaDon", hoaDonPhuMoi,
                    "chiTiet", new ArrayList<>()
                    ));
                }
                System.out.println("LOG: Äã tạo " + (selectedBanMoi.size() - 1) + " Hóa Äơn Phụ má»i.");
            }
            // 4. GIáº¢I PHÃNG VÃ KHÃA BÃN
            // 4.1. Giải phóng các bÃ n cũ KHÃNG còn Äưá»£c sá»­ dụng
            Set<String> maBanCuKhongDuocChonLai = new HashSet<>(maBanCuList);
            maBanCuKhongDuocChonLai.removeAll(maBanMoiSet);
            if (!maBanCuKhongDuocChonLai.isEmpty()) {
                Client.sendWithParams(CommandType.UPDATE_MANY_TABLES, Map.of(
                "maBan", new ArrayList<>(maBanCuKhongDuocChonLai),
                "newStatus", TrangThaiBan.TRONG.getDbValue()
                ));
                System.out.println("LOG: Äã giải phóng các bÃ n cũ: " + maBanCuKhongDuocChonLai);
            }
            // 4.2. Cáº­p nháº­t trạng thái Báº¬N cho Táº¤T Cáº¢ các bÃ n má»i Äưá»£c chá»n
            String trangThaiBanMoi = (hoaDonGoc.getTrangThai() == TrangThaiHoaDon.DAT) ?
            TrangThaiHoaDon.DAT.getDbValue() : TrangThaiHoaDon.DANG_SU_DUNG.getDbValue();
            // ð¥ Gá»I API Äá» Cáº¬P NHáº¬T NHIá»U BÃN
            Client.sendWithParams(CommandType.UPDATE_MANY_TABLES, Map.of(
            "maBan", new ArrayList<>(maBanMoiSet),
            "newStatus", trangThaiBanMoi
            ));
            System.out.println("LOG: Äã khóa " + maBanMoiSet.size() + " bÃ n má»i vá»i trạng thái: " + trangThaiBanMoi);
            // 5. Káº¾T THÃC
            showAlert(AlertType.INFORMATION, "ThÃ nh công",
            String.format("Äã Äá»i/gá»p bÃ n thÃ nh công!\nTừ: %s\nSang: %s",
            String.join(", ", maBanCuList), String.join(", ", maBanMoiSet)));
            if (mainController != null) {
                mainController.loadTableGrids();
                mainController.loadBookingCards();
                Response resHd = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", maHDGoc));
                if (resHd.getStatusCode() == 200) {
                    HoaDon hdMoi = JsonUtil.convertValue(resHd.getData(), HoaDon.class);
                    mainController.loadHoaDonToMainInterface(hdMoi);
                }
            }
            closePopup();
        } catch (Exception e) {
        e.printStackTrace();
        showAlert(AlertType.ERROR, "Lá»i CSDL", "Không thá» Äá»i/gá»p bÃ n: " + e.getMessage());
    }
}
}
/**
* Cáº­p nháº­t trạng thái enable/disable của nút Xác nháº­n
* ð¥ ÄÃ Sá»¬A: CHá» Cáº¦N CHá»N ÃT NHáº¤T 1 BÃN (cho phÃ©p gá»p bÃ n)
*/
private void updateXacNhanButtonState() {
    // Láº¥y danh sách bÃ n Äang Äưá»£c chá»n từ Checkbox Map
    long countSelected = selectionMap.values().stream()
    .filter(BooleanProperty::get)
    .count();
    // Chá» enable khi sá» lưá»£ng chá»n Lá»N HÆ N 0
    btnXacNhanDoi.setDisable(countSelected == 0);
}
/**
* Äóng cá»­a sá» Popup
*/
private void closePopup() {
    Stage stage = (Stage) btnHuy.getScene().getWindow();
    stage.close();
}
// --- HÃ m tiá»n ích ---
private void showAlert(AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
private Optional<ButtonType> showAlertConfirm(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    return alert.showAndWait();
}
}