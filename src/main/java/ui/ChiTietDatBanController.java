package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import entity.HoaDon;
import entity.TrangThaiBan;
import entity.TrangThaiHoaDon;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ui.DatBan.MonOrder;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
/**
* Controller Chi Tiết Äáº·t BÃ n: Xá»­ lý hiá»n thá» thông tin vÃ  Cáº¬P NHáº¬T TRáº NG THÃI.
* Äã Äưá»£c Äơn giản hóa: áº¨n các nút chức nÄng phức tạp (Äá»i/Tách/Gá»i món).
* =========================================================
* FIX: Äã thêm lại hÃ m setHoaDonData vÃ  sá»­ dụng các @FXML fields Äúng.
* =========================================================
*/
public class ChiTietDatBanController implements Initializable {
    // =========================================================
    // FXML FIELDS (LÆ¯á»¢C Bá» CÃC NÃT KHÃNG Cáº¦N THIáº¾T)
    // =========================================================
    @FXML private Label lblMaHD;
    @FXML private TextField txtTenKhachHang;
    @FXML private TextField txtSoDienThoai;
    @FXML private TextField txtSoLuongKhach;
    @FXML private TextField txtYeuCau;
    @FXML private TextField txtMaBan;
    @FXML private ComboBox<String> comboTrangThai; // Chứa tên hiá»n thá» trạng thái
    @FXML private TextField txtThoiGian;
    @FXML private DatePicker datePickerThoiGianDen;
    // NÃT CHá»¨C NÄNG Cáº¦N GIá»® Láº I (Dựa trên ChiTietDatBan_Popup.fxml Äã sá»­a)
    @FXML private Button btnCapNhat;
    @FXML private Button btnBack;
    // FXML fields từ Panel_ChiTietOrder.fxml (Cáº§n Äưá»£c gán lại qua lookup nếu không dùng Controller con)
    @FXML private StackPane contentContainer;
    private Label lblTienCoc;
    private Label lblTongTienMonAn;
    private Label lblTongThanhToan;
    private TableView<MonOrder> tblChiTietOrder;
    // =========================================================
    // LOGIC & DATA
    // =========================================================
    private HoaDon hoaDon;
    private ObservableList<MonOrder> monOrderList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private Node orderPanel; // Cache panel con
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // === BÆ¯á»C 1: TRáº¢ Láº I DANH SÃCH Äáº¦Y Äá»¦ ===
        comboTrangThai.setItems(FXCollections.observableArrayList(
        TrangThaiHoaDon.DAT.getDisplayName(),          // "Äã Äáº·t"
        TrangThaiHoaDon.DANG_SU_DUNG.getDisplayName(), // "Äang phục vụ"
        TrangThaiHoaDon.DA_THANH_TOAN.getDisplayName(),// "Äã thanh toán" (Giữ lại)
        TrangThaiHoaDon.DA_HUY.getDisplayName(),       // "Äã hủy"
        TrangThaiHoaDon.HOA_DON_TAM.getDisplayName(),  // "Hóa Äơn tạm"
        TrangThaiHoaDon.CHO_XAC_NHAN.getDisplayName()  // "Chá» xác nháº­n"
        ));
        // === BÆ¯á»C 2: THÃM CELL FACTORY Äá» VÃ HIá»U HÃA Lá»°A CHá»N ===
        final String disabledItem = TrangThaiHoaDon.DA_THANH_TOAN.getDisplayName();
        comboTrangThai.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setDisable(false);
                            setStyle(null);
                        } else {
                        setText(item);
                        // Vô hiá»u hóa cell nếu item lÃ  "Äã thanh toán"
                        boolean isDisabled = item.equals(disabledItem);
                        setDisable(isDisabled);
                        // Thêm style Äá» lÃ m nó má» Äi (giá»ng như bá» vô hiá»u hóa)
                        getStyleClass().remove("disabled-list-cell"); // Xóa class cũ (nếu có)
                        if (isDisabled) {
                            getStyleClass().add("disabled-list-cell"); // Thêm class má»i
                        } else {
                        setStyle(null); // Reset style cho các cell khác
                    }
                }
            }
        };
    }
});
// === Káº¾T THÃC THÃM Má»I ===
// Gán sự kiá»n (giữ nguyên)
btnBack.setOnAction(e -> handleClosePopup());
if (btnCapNhat != null) {
    btnCapNhat.setOnAction(e -> handleCapNhatDatBan());
}
}
/**
* ð¥ Phương thức khá»i tạo dữ liá»u vÃ  nháº­n tham chiếu DAO.
* Äây lÃ  hÃ m kÃ­ch hoạt viá»c tải dữ liá»u vÃ  giao diá»n.
*/
public void setHoaDonData(HoaDon hd) {
    this.hoaDon = hd;
    // Tải Panel Chi tiết Order
    loadOrderPanel();
    // Tải dữ liá»u vÃ o UI (Header + Panel Order)
    loadDataToUI();
    // Cáº§n tải lại Panel Order Äá» hiá»n thá» món
    loadOrderDetail();
}
/**
* Tải dữ liá»u chÃ­nh (Header) lên UI.
*/
private void loadDataToUI() {
    if (hoaDon == null) return;
    lblMaHD.setText("Chi tiết Äáº·t bÃ n " + hoaDon.getMaHD());
    // Láº¥y giá trá» trạng thái DB
    String trangThaiHdDb = hoaDon.getTrangThai().getDbValue();
    if (hoaDon.getKhachHang() != null) {
        txtTenKhachHang.setText(hoaDon.getKhachHang().getTenKH());
        txtSoDienThoai.setText(hoaDon.getKhachHang().getSoDT());
    }
    if (hoaDon.getBan() != null) {
        txtMaBan.setText(hoaDon.getBan().getMaBan());
        txtSoLuongKhach.setText(String.valueOf(hoaDon.getBan().getSucChua()));
    }
    if (hoaDon.getGioVao() != null) {
        txtThoiGian.setText(hoaDon.getGioVao().toLocalTime().format(timeFormatter));
        datePickerThoiGianDen.setValue(hoaDon.getGioVao().toLocalDate());
    }
    // === Cáº¬P NHáº¬T COMBO BOX TRáº NG THÃI (Láº¤Y TÃN HIá»N THá») ===
    String trangThaiHienThi = TrangThaiHoaDon.fromDbValue(trangThaiHdDb).getDisplayName();
    comboTrangThai.getSelectionModel().select(trangThaiHienThi);
    // ===================================
}
/**
* Tải Panel Chi Tiết Order (Món Än) vÃ  gán các fields.
*/
private void loadOrderPanel() {
    if (contentContainer == null) return;
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Panel_ChiTietOrder.fxml"));
        orderPanel = loader.load();
        // === GÃN CÃC FIELDS Bá» THIáº¾U Tá»ª PANEL PHá»¤ QUA LOOKUP ===
        @SuppressWarnings("unchecked")
        TableView<MonOrder> tblLookup = (TableView<MonOrder>) orderPanel.lookup("#tblChiTietOrder");
        tblChiTietOrder = tblLookup;
        lblTongTienMonAn = (Label) orderPanel.lookup("#lblTongTienMonAn");
        lblTienCoc = (Label) orderPanel.lookup("#lblTienCoc");
        lblTongThanhToan = (Label) orderPanel.lookup("#lblTongThanhToan");
        Button btnThanhToan = (Button) orderPanel.lookup("#btnThanhToan"); // Nút thanh toán trong panel
        // Gán sự kiá»n thanh toán (tùy chá»n)
        if (btnThanhToan != null) {
            // btnThanhToan.setOnAction(e -> handleThanhToan());
            btnThanhToan.setVisible(false); // áº¨n nút Thanh Toán theo yêu cáº§u
        }
        // ========================================================
        setupOrderTable(); // CÃ i Äáº·t TableView
        contentContainer.getChildren().clear();
        contentContainer.getChildren().add(orderPanel);
    } catch (IOException e) {
    showAlert(AlertType.ERROR, "Lá»i UI", "Không thá» tải file /fxml/Panel_ChiTietOrder.fxml.");
}
}
/**
* Tải dữ liá»u món Än vÃ o TableView.
*/
public void loadOrderDetail() {
    if (hoaDon != null && hoaDon.getMaHD() != null && tblChiTietOrder != null) {
        monOrderList.clear();
        Response res = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, Map.of("maHD", hoaDon.getMaHD()));
        if (res.getStatusCode() == 200) {
            List<MonOrder> chiTiet = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), MonOrder.class);
            monOrderList.addAll(chiTiet);
        }
        tblChiTietOrder.setItems(monOrderList);
        calculateTotal();
    }
}
/**
* CÃ i Äáº·t cáº¥u trúc cá»t cho TableView (Dùng các fields Äã lookup).
*/
private void setupOrderTable() {
    if (tblChiTietOrder == null) return;
    // Giả sá»­ các cá»t Äã Äưá»£c Äá»nh nghÄ©a Äúng thứ tự vÃ  kiá»u trong FXML
    // Dùng Index Äá» láº¥y cá»t (không an toÃ n, nhưng cáº§n thiết nếu không dùng fx:id)
    if (tblChiTietOrder.getColumns().size() >= 6) {
        @SuppressWarnings("unchecked")
        TableColumn<MonOrder, String> colTenMon = (TableColumn<MonOrder, String>) tblChiTietOrder.getColumns().get(0);
        @SuppressWarnings("unchecked")
        TableColumn<MonOrder, Number> colDonGia = (TableColumn<MonOrder, Number>) tblChiTietOrder.getColumns().get(1);
        @SuppressWarnings("unchecked")
        TableColumn<MonOrder, Integer> colSoLuong = (TableColumn<MonOrder, Integer>) tblChiTietOrder.getColumns().get(2);
        @SuppressWarnings("unchecked")
        TableColumn<MonOrder, Number> colThanhTien = (TableColumn<MonOrder, Number>) tblChiTietOrder.getColumns().get(3);
        @SuppressWarnings("unused")
        TableColumn<MonOrder, Void> colTangGiam = (TableColumn<MonOrder, Void>) tblChiTietOrder.getColumns().get(4);
        @SuppressWarnings("unused")
        TableColumn<MonOrder, Void> colHuy = (TableColumn<MonOrder, Void>) tblChiTietOrder.getColumns().get(5);
        colTenMon.setCellValueFactory(cellData -> cellData.getValue().tenMonProperty());
        colDonGia.setCellValueFactory(cellData -> cellData.getValue().donGiaProperty());
        colSoLuong.setCellValueFactory(cellData -> cellData.getValue().soLuongProperty().asObject());
        colThanhTien.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getDonGia() * cellData.getValue().getSoLuong()));
        // Logic tÄng giảm / hủy (Giữ nguyên logic phức tạp)
        // ... (cáº§n thêm logic CellFactory cho TangGiam vÃ  Huy)
    }
}
/**
* TÃ­nh tá»ng tiá»n vÃ  cáº­p nháº­t labels.
*/
public void calculateTotal() {
    double tongTienMonAn = monOrderList.stream()
    .mapToDouble(order -> order.getDonGia() * order.getSoLuong())
    .sum();
    final double VAT_RATE = 0.08;
    double thueVAT = tongTienMonAn * VAT_RATE;
    // Giả sá»­ getTienCoc() trả vá» double (kiá»u nguyên thủy)
    double tienCocDaThanhToan = (hoaDon != null) ? hoaDon.getTienCoc() : 0.0;
    double tongTienThanhToan = tongTienMonAn + thueVAT - tienCocDaThanhToan;
    // Luôn kiá»m tra null cho các UI component Äưá»£c gán Äá»ng
    if (lblTongTienMonAn != null) lblTongTienMonAn.setText(String.format("%,.0f Ä", tongTienMonAn));
    if (lblTienCoc != null) lblTienCoc.setText(String.format("%,.0f Ä", tienCocDaThanhToan));
    if (lblTongThanhToan != null) lblTongThanhToan.setText(String.format("%,.0f Ä", Math.max(0, tongTienThanhToan)));
}
/**
* ð¥ Xá»­ lý Cáº­p nháº­t thông tin Äáº·t bÃ n vÃ  TRáº NG THÃI (FINAL FIX)
*/
@FXML
private void handleCapNhatDatBan() {
    if (hoaDon == null) return;
    String trangThaiMoiDisplay = comboTrangThai.getSelectionModel().getSelectedItem();
    // --- FIX Lá»I: KIá»M TRA NULL Tá»ª fromDisplayName ---
    // Convert display name to Enum (Äảm bảo không bá» NullPointerException khi gá»i .getDbValue())
    TrangThaiHoaDon newStatusEnum = TrangThaiHoaDon.fromDisplayName(trangThaiMoiDisplay);
    if (newStatusEnum == null) {
        showAlert(AlertType.ERROR, "Lá»i Chuyá»n Äá»i", "Trạng thái Äưá»£c chá»n không há»£p lá». Vui lòng kiá»m tra lại.");
        return;
    }
    String trangThaiMoiDb = newStatusEnum.getDbValue();
    // ----------------------------------------------------
    try {
        // Xác Äá»nh xem có cáº§n set gioRa (Chá» khi DaThanhToan hoáº·c DaHuy)
        boolean setGioRa = (trangThaiMoiDb.equals(TrangThaiHoaDon.DA_THANH_TOAN.getDbValue()) || trangThaiMoiDb.equals(TrangThaiHoaDon.DA_HUY.getDbValue()));
        // 1. Cáº­p nháº­t trạng thái Hóa Äơn qua API
        Client.sendWithParams(CommandType.UPDATE_INVOICE, Map.of(
        "maHD", hoaDon.getMaHD(),
        "trangThai", trangThaiMoiDb,
        "setGioRa", setGioRa
        ));
        // 2. Cáº­p nháº­t trạng thái BÃ n qua API
        if(hoaDon.getBan() != null) {
            String banStatusUpdate = (setGioRa) ? TrangThaiBan.TRONG.getDbValue() : trangThaiMoiDb;
            Client.sendWithParams(CommandType.UPDATE_TABLE_STATUS, Map.of(
            "maBan", hoaDon.getBan().getMaBan(),
            "newStatus", banStatusUpdate
            ));
        }
        // 3. Tải lại dữ liá»u chÃ­nh vÃ  Äóng popup
        Stage stage = (Stage) btnCapNhat.getScene().getWindow();
        if (stage.getScene().getRoot().getUserData() instanceof DatBan) {
            DatBan parentCtrl = (DatBan) stage.getScene().getRoot().getUserData();
            parentCtrl.loadBookingCards();
            parentCtrl.loadTableGrids();
        }
        showAlert(AlertType.INFORMATION, "ThÃ nh công", "Äã cáº­p nháº­t trạng thái Hóa Äơn thÃ nh: " + trangThaiMoiDisplay);
        handleClosePopup();
    } catch (Exception e) {
    e.printStackTrace();
    showAlert(AlertType.ERROR, "Lá»i Cáº­p Nháº­t", "Không thá» cáº­p nháº­t trạng thái: " + e.getMessage());
}
}
// =========================================================
// HÃM UTILITY
// =========================================================
/**
* Äóng cửa sổ popup
*/
@FXML
private void handleClosePopup() {
    Stage stage = (Stage) btnBack.getScene().getWindow();
    if (stage != null) {
        stage.close();
    }
}
private void showAlert(AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
}