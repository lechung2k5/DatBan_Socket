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
    // Äá» Controller này có thá» gá»i các hàm phức tạp trong DatBan
    // (Giả Äá»nh mainController có các hàm sau, nếu không, ta phải tạo interface hoặc dùng Reflection)
    // CíCH Tá»T NHáº¤T: Bá» sung thêm các phương thức public cần thiết vào DatBan.java
    // Cần có má»t Set các mã bàn cũ Äá» kiá»m tra
    private Set<String> maBanCuSet;
    @FXML
    private void initialize() {
        listViewBanTrong.setItems(banTrongList);
        listViewBanTrong.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // === KHíI PHá»¤C LIST CELL Sá»¬ Dá»¤NG CHECKBOX Ví BINDING Vá»I selectionMap ===
        listViewBanTrong.setCellFactory(new Callback<ListView<Ban>, ListCell<Ban>>() {
            @Override
            public ListCell<Ban> call(ListView<Ban> lv) {
                ListCell<Ban> cell = new ListCell<Ban>() {
                    private final CheckBox checkBox = new CheckBox();
                    private final Label label = new Label();
                    private final HBox hbox = new HBox(5, checkBox, label);
                    {
                        hbox.setAlignment(Pos.CENTER_LEFT);
                        // Khi Checkbox Äược click, cập nhật trạng thái trong Map
                        checkBox.setOnAction(event -> {
                            if (getItem() != null) {
                                // Cập nhật BooleanProperty trong Map
                                selectionMap.get(getItem()).set(checkBox.isSelected());
                                updateXacNhanButtonState(); // Cập nhật trạng thái nút
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
                        // Lấy/Tạo BooleanProperty và cập nhật CheckBox
                        BooleanProperty selected = selectionMap.computeIfAbsent(item, k -> new SimpleBooleanProperty(false));
                        // Sá»¬A Lá»I Cí PHíP: Dùng getDisplayName()
                        label.setText(item.getMaBan() + " (" + item.getSucChua() + " chá») - " + item.getTrangThai().getDisplayName());
                        // Set trạng thái của Checkbox từ Map
                        checkBox.setSelected(selected.get());
                        // === LOGIC Ví HIá»U HíA CHECKBOX Ví BíN Báº¬N (FIX Lá»I B010) ===
                        boolean laBanCu = maBanCuSet != null && maBanCuSet.contains(item.getMaBan());
                        // Bàn bá» coi là Báº¬N nếu trạng thái logic (logic 4/8 tiếng) không phải là TRONG
                        boolean isCurrentlyBusy = item.getTrangThai() != TrangThaiBan.TRONG;
                        // Chá» cho phép chá»n nếu (Nó là Bàn cũ) HOáº¶C (Nó hoàn toàn TRá»NG)
                        // Nếu isCurrentlyBusy=true, và nó KHíNG phải bàn cũ -> DISABLE
                        checkBox.setDisable(isCurrentlyBusy && !laBanCu);
                        // Set màu ná»n (tùy chá»n)
                        if (isCurrentlyBusy) {
                            setStyle("-fx-background-color: #fce4e4; -fx-opacity: 0.8;"); // Màu nhạt cho bàn bận
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
* Nhận dữ liá»u ban Äầu từ màn hình DatBan
*/
public void setInitialData(List<HoaDon> hoaDonGocVaPhu, DatBan mainController) {
    this.hoaDonGocVaPhu = hoaDonGocVaPhu;
    this.mainController = mainController;
    // Lấy mã bàn cũ má»t lần duy nhất
    this.maBanCuSet = hoaDonGocVaPhu.stream()
    .filter(hd -> hd.getBan() != null)
    .map(hd -> hd.getBan().getMaBan())
    .collect(Collectors.toSet());
    // Hiá»n thá» thông tin HÄ hiá»n tại
    HoaDon hdGoc = hoaDonGocVaPhu.stream().filter(h -> h.getMaHDGoc() == null).findFirst().orElse(null);
    if (hdGoc != null) {
        lblMaHDGoc.setText(hdGoc.getMaHD());
        soBanCanChon = hoaDonGocVaPhu.size(); // Sá» bàn cần chá»n = sá» bàn hiá»n tại
        lblSoBanHienTai.setText(String.valueOf(soBanCanChon));
        String dsBanCu = hoaDonGocVaPhu.stream()
        .map(h -> h.getBan() != null ? h.getBan().getMaBan() : "?")
        .collect(Collectors.joining(", "));
        lblDanhSachBanCu.setText(dsBanCu);
        lblThongTinChonBan.setText(String.format("Chá»n Äúng %d bàn trá»ng dưá»i Äây:", soBanCanChon));
        // Äặt thá»i gian mặc Äá»nh là giá» vào hiá»n tại của HÄ Gá»c
        if (hdGoc.getGioVao() != null) {
            datePickerThoiGianMoi.setValue(hdGoc.getGioVao().toLocalDate());
            txtThoiGianMoi.setText(hdGoc.getGioVao().toLocalTime().format(timeFormatter));
        } else {
        // Hoặc giá» hiá»n tại nếu HÄ không có giá» vào
        datePickerThoiGianMoi.setValue(LocalDate.now());
        txtThoiGianMoi.setText(LocalTime.now().format(timeFormatter));
    }
    // Tự Äá»ng tìm bàn trá»ng lần Äầu
    handleTimBanTrong();
} else {
// Xử lý lá»i nếu không tìm thấy HÄ Gá»c
showAlert(AlertType.ERROR, "Lá»i Dữ liá»u", "Không tìm thấy Hóa Äơn Gá»c trong danh sách truyá»n vào.");
closePopup();
}
}
// DoiBanPopupController.java (Chá» phần hàm handleTimBanTrong Äược sửa)
/**
* Xử lý khi nhấn nút "Tìm bàn trá»ng"
*/
private void handleTimBanTrong() {
    LocalDate ngayMoi = datePickerThoiGianMoi.getValue();
    String gioMoiStr = txtThoiGianMoi.getText();
    LocalTime gioMoi;
    try {
        if (ngayMoi == null || gioMoiStr == null || gioMoiStr.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Thiếu thá»i gian", "Vui lòng nhập Ngày và Giá» má»i.");
            return;
        }
        gioMoi = LocalTime.parse(gioMoiStr, timeFormatter);
    } catch (Exception e) {
    showAlert(AlertType.ERROR, "Lá»i Äá»nh dạng", "Giá» nhập không hợp lá» (cần HH:mm).");
    return;
}
// Cần tải lại ds HÄ Äang chá» cho logic hiá»n thá» trạng thái chính xác
mainController.loadDsHoaDonDatTrongNgay(ngayMoi);
banTrongList.clear();
selectionMap.clear();
// --- Lấy mã bàn cũ ---
Set<String> maBanCuSet = hoaDonGocVaPhu.stream()
.filter(hd -> hd.getBan() != null)
.map(hd -> hd.getBan().getMaBan())
.collect(Collectors.toSet());
// -----------------------
// ð¥ FIX: Lấy Táº¤T Cáº¢ các bàn và kiá»m tra trạng thái Báº¬N Cá»¨NG (DAO Äã xử lý loại trừ HÄ hiá»n tại)
// GIáº¢ Äá»NH: DAO.getAllBanWithAvailability Äí ÄÆ¯á»¢C FIX Äá» LOáº I TRá»ª Cá»¤M HÄ ÄANG XEM.
// DO KHíNG Cí CODE DAO Äáº¦Y Äá»¦ á» ÄíY, CHíNG TA PHáº¢I Gá»I DAO Äá» Láº¤Y Táº¤T Cáº¢ BíN
// Ví DíNG LOGIC MíU Cá»¦A CONTROLLER CHíNH
Response res = Client.send(CommandType.GET_TABLES, null);
List<Ban> tatCaBan;
if (res.getStatusCode() == 200) {
    tatCaBan = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), Ban.class);
} else {
tatCaBan = FXCollections.observableArrayList();
}
for (Ban ban : tatCaBan) {
    // 1. Lấy trạng thái Báº¬N (logic 4/8 tiếng) tại thá»i Äiá»m má»i
    TrangThaiBan trangThaiLogic = mainController.getTrangThaiHienThi(ban, gioMoi);
    // 2. Tạo Äá»i tượng Ban má»i Äá» hiá»n thá» trong ListView
    Ban banHienThi = new Ban(ban.getMaBan(), ban.getViTri(), ban.getSucChua(), ban.getLoaiBan(), ban.getTrangThai());
    // ð¥ LOGIC Má»I: Chá» loại trừ bàn cũ khá»i danh sách Báº¬N.
    if (maBanCuSet.contains(ban.getMaBan())) {
        // Nếu là bàn cũ -> luôn set là TRá»NG Äá» cho phép chá»n (dù logic 4/8 tiếng báo Äá»/CAM)
        banHienThi.setTrangThai(TrangThaiBan.TRONG);
    } else if (trangThaiLogic != TrangThaiBan.TRONG) {
    // Nếu là bàn má»i và Bận theo Logic 4/8 tiếng -> giữ trạng thái Báº¬N
    banHienThi.setTrangThai(trangThaiLogic);
} else {
// Ngược lại -> TRá»NG
banHienThi.setTrangThai(TrangThaiBan.TRONG);
}
banTrongList.add(banHienThi);
// Khá»i tạo trạng thái chá»n: Tá»° Äá»NG TICK BíN CÅ¨
boolean isSelected = maBanCuSet.contains(banHienThi.getMaBan());
BooleanProperty prop = new SimpleBooleanProperty(isSelected);
selectionMap.put(banHienThi, prop);
}
// Cần refresh ListView Äá» ListCellFactory cập nhật Checkbox
listViewBanTrong.refresh();
updateXacNhanButtonState();
lblThongTinChonBan.setText(String.format("Chá»n Äúng %d bàn trá»ng muá»n Äá»i Äến:", soBanCanChon));
}
/**
* ð¥ HíM Äí Sá»¬A: Xử lý xác nhận Äá»i bàn (Bao gá»m các trưá»ng hợp: 1-1, N-N, N-M, N-1)
* Äí Tá»I Æ¯U: Loại bá» vòng lặp cập nhật HÄ cũ và thay báº±ng các bưá»c Cập nhật/Xóa rõ ràng.
*/
/**
* ð¥ HíM Äí Sá»¬A: Xử lý xác nhận Äá»i bàn (Bao gá»m các trưá»ng hợp: 1-1, N-N, N-M, N-1)
* Äí Sá»¬A Lá»I: Gá»i hàm DAO má»i Äá» cập nhật trạng thái NHIá»U bàn cùng lúc.
*/
private void handleXacNhanDoi() {
    // ... (phần code lấy selectedBanMoi, hoaDonGoc, maBanCuList, hoaDonPhuCuList, maBanMoiSet, maBanMoiGoc) ...
    List<Ban> selectedBanMoi = selectionMap.entrySet().stream()
    .filter(entry -> entry.getValue().get())
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
    if (selectedBanMoi.isEmpty()) {
        showAlert(AlertType.ERROR, "Chưa chá»n bàn", "Vui lòng chá»n ít nhất má»t bàn má»i.");
        return;
    }
    // Kiá»m tra bàn không Äược bận (chá» kiá»m tra các bàn má»i không phải là bàn cũ)
    for (Ban ban : selectedBanMoi) {
        if (ban.getTrangThai() != TrangThaiBan.TRONG && !maBanCuSet.contains(ban.getMaBan())) {
            showAlert(AlertType.ERROR, "Lá»i", "Bàn " + ban.getMaBan() + " Äang bận. Vui lòng bá» chá»n bàn này.");
            return;
        }
    }
    HoaDon hoaDonGoc = hoaDonGocVaPhu.stream().filter(h -> h.getMaHDGoc() == null).findFirst().orElse(null);
    if (hoaDonGoc == null) return;
    // Lấy mã bàn cũ và các HÄ phụ
    List<String> maBanCuList = hoaDonGocVaPhu.stream().map(h -> h.getBan() != null ? h.getBan().getMaBan() : null)
    .filter(Objects::nonNull).collect(Collectors.toList());
    List<HoaDon> hoaDonPhuCuList = hoaDonGocVaPhu.stream().filter(h -> h.getMaHDGoc() != null).collect(Collectors.toList());
    Set<String> maBanMoiSet = selectedBanMoi.stream().map(Ban::getMaBan).collect(Collectors.toSet());
    // Bàn má»i gá»c là bàn Äầu tiên Äược chá»n
    String maBanMoiGoc = selectedBanMoi.get(0).getMaBan();
    String maHDGoc = hoaDonGoc.getMaHD();
    // --- Kiá»m tra trùng lặp (Giữ nguyên) ---
    if (new HashSet<>(maBanCuList).equals(maBanMoiSet) && maBanCuList.size() == maBanMoiSet.size()) {
        showAlert(AlertType.WARNING, "Không Äá»i", "Bàn cũ và bàn má»i giá»ng nhau.");
        return;
    }
    // ----------------------------------------
    Optional<ButtonType> result = showAlertConfirm("Xác nhận Äá»i bàn",
    String.format("Bạn có chắc chắn muá»n Äá»i các bàn %s sang các bàn %s không?\n\n"
    + "Lưu ý: Các hóa Äơn phụ/bàn cũ không còn Äược liên kết sáº½ bá» hủy/giải phóng.",
    String.join(", ", maBanCuList), String.join(", ", maBanMoiSet)));
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            Timestamp thoiGianDoiMoi = Timestamp.valueOf(datePickerThoiGianMoi.getValue().atTime(LocalTime.parse(txtThoiGianMoi.getText(), timeFormatter)));
            // 1. XíA Táº¤T Cáº¢ HíA ÄÆ N PHá»¤ CÅ¨ QUA API
            for (HoaDon hdPhu : hoaDonPhuCuList) {
                Client.sendWithParams(CommandType.CANCEL_INVOICE, Map.of(
                "maHD", hdPhu.getMaHD(),
                "trangThai", "DaHuy"
                ));
            }
            // 2. Cáº¬P NHáº¬T HíA ÄÆ N Gá»C QUA API
            Client.sendWithParams(CommandType.UPDATE_INVOICE, Map.of(
            "maHD", maHDGoc,
            "maBan", maBanMoiGoc,
            "gioVao", thoiGianDoiMoi.toString()
            ));
            // 3. Táº O HíA ÄÆ N PHá»¤ Má»I (Nếu có nhiá»u hơn 1 bàn má»i)
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
            // 4. GIáº¢I PHíNG Ví KHíA BíN
            // 4.1. Giải phóng các bàn cũ KHíNG còn Äược sử dụng
            Set<String> maBanCuKhongDuocChonLai = new HashSet<>(maBanCuList);
            maBanCuKhongDuocChonLai.removeAll(maBanMoiSet);
            if (!maBanCuKhongDuocChonLai.isEmpty()) {
                Client.sendWithParams(CommandType.UPDATE_MANY_TABLES, Map.of(
                "maBan", new ArrayList<>(maBanCuKhongDuocChonLai),
                "newStatus", TrangThaiBan.TRONG.getDbValue()
                ));
                System.out.println("LOG: Äã giải phóng các bàn cũ: " + maBanCuKhongDuocChonLai);
            }
            // 4.2. Cập nhật trạng thái Báº¬N cho Táº¤T Cáº¢ các bàn má»i Äược chá»n
            String trangThaiBanMoi = (hoaDonGoc.getTrangThai() == TrangThaiHoaDon.DAT) ?
            TrangThaiHoaDon.DAT.getDbValue() : TrangThaiHoaDon.DANG_SU_DUNG.getDbValue();
            // ð¥ Gá»I API Äá» Cáº¬P NHáº¬T NHIá»U BíN
            Client.sendWithParams(CommandType.UPDATE_MANY_TABLES, Map.of(
            "maBan", new ArrayList<>(maBanMoiSet),
            "newStatus", trangThaiBanMoi
            ));
            System.out.println("LOG: Äã khóa " + maBanMoiSet.size() + " bàn má»i vá»i trạng thái: " + trangThaiBanMoi);
            // 5. Káº¾T THíC
            showAlert(AlertType.INFORMATION, "Thành công",
            String.format("Äã Äá»i/gá»p bàn thành công!\nTừ: %s\nSang: %s",
            String.join(", ", maBanCuList), String.join(", ", maBanMoiSet)));
            if (mainController != null) {
                mainController.loadTableGrids();
                mainController.loadBookingCards();
                Response resHd = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", maHDGoc));
                if (resHd.getStatusCode() == 200) {
                    HoaDon hdMoi = JsonUtil.convertValue(resHd.getData(), HoaDon.class);
                    mainController.loadHoaDonToMainInterface(hdMoi, false);
                }
            }
            closePopup();
        } catch (Exception e) {
        e.printStackTrace();
        showAlert(AlertType.ERROR, "Lá»i CSDL", "Không thá» Äá»i/gá»p bàn: " + e.getMessage());
    }
}
}
/**
* Cập nhật trạng thái enable/disable của nút Xác nhận
* ð¥ Äí Sá»¬A: CHá» Cáº¦N CHá»N íT NHáº¤T 1 BíN (cho phép gá»p bàn)
*/
private void updateXacNhanButtonState() {
    // Lấy danh sách bàn Äang Äược chá»n từ Checkbox Map
    long countSelected = selectionMap.values().stream()
    .filter(BooleanProperty::get)
    .count();
    // Chá» enable khi sá» lượng chá»n Lá»N HÆ N 0
    btnXacNhanDoi.setDisable(countSelected == 0);
}
/**
* Äóng cửa sá» Popup
*/
private void closePopup() {
    Stage stage = (Stage) btnHuy.getScene().getWindow();
    stage.close();
}
// --- Hàm tiá»n ích ---
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