package ui;
import entity.*;
import network.CommandType;
import network.Response;
import network.Client;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import utils.ClientSessionManager;
import java.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Callback; // CáÂºÂ§n thiáÂºÂ¿t cho CellValueFactory
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.stream.Collectors;






public class ThanhToanPreviewController {
    // === KHAI BÁO CŨ (Giữ nguyên) ===
    @FXML private Label lblMaHD;
    @FXML private Label lblBan;
    @FXML private Label lblTongMonAn;
    @FXML private Label lblPhiDV;
    @FXML private Label lblThueVAT;
    @FXML private Label lblTienCoc;
    @FXML private Label lblTongThanhToan;
    @FXML private ToggleButton btnTienMat;
    @FXML private ToggleButton btnNganHang;
    @FXML private ToggleButton btnMoMo;
    @FXML private Button btnHuy;
    @FXML private Button btnXacNhan;
    @FXML private Button btnInHoaDon;
    @FXML private ToggleGroup paymentGroup;
    // === KHAI BÁO MỚI (Từ FXML Preview) ===
    @FXML private Label lblTongTruocKM;
    @FXML private Label lblThanhVien;
    @FXML private Label lblUuDaiApDung;
    @FXML private Label lblSoTienKhachTra;
    // Thông tin chung
    @FXML private Label lblNgay;
    @FXML private Label lblGioVao;
    @FXML private Label lblGioRa;
    @FXML private Label lblThuNgan;
    @FXML private Label lblKhachHang;
    // BẢNG MÓN ĂN (Sẽ dùng MyReceiptItem)
    @FXML private TableView<MyReceiptItem> tblMonAnThanhToan;
    @FXML private TableColumn<MyReceiptItem, Integer> colStt;
    @FXML private TableColumn<MyReceiptItem, String> colTenMon;
    @FXML private TableColumn<MyReceiptItem, Integer> colSL;
    @FXML private TableColumn<MyReceiptItem, Double> colDonGia;
    @FXML private TableColumn<MyReceiptItem, Double> colThanhTien;
    // ===============================================
    @FXML private ComboBox<String> promoComboBoxPreview;
    // 🔥 THÊM CÁC BIẾN ĐỂ QUẢN LÝ KHUYẾN MÃI VÀ TÍNH TOÁN LẠI
    private List<UuDai> dsUuDaiDangApDung = new ArrayList<>();
    private UuDai selectedUuDai = null;
    // 🔥 THÊM BIẾN LƯU TRỮ GIÁ TRỊ TÍNH TOÁN
    private double currentTongMonAn;
    private double currentTienCoc;
    private HoaDon hoaDonToPay;
    private DatBan mainController;
    private PTTThanhToan selectedPTTT = PTTThanhToan.TIEN_MAT;
    private final DecimalFormat currencyFormatter = new DecimalFormat("###,### VNĐ");
    private boolean isNewInvoice;
    // Dữ LIỆU TÁCH MÓN
    private ObservableList<TachBanPopupController.MonTach> monTachListSnapshot;
    private String maHDGocSnapshot;
    @FXML
    private void initialize() {
        // 🔥 THÊM SETONACTION CHO CÁC NÚT THANH TOÁN
        if (btnTienMat != null) {
            btnTienMat.setOnAction(e -> moPopupThanhToanTienMat());
        }
        if (btnNganHang != null) {
            btnNganHang.setOnAction(e -> openNganHangQrPopup());
        }
        if (btnMoMo != null) {
            btnMoMo.setOnAction(e -> openMoMoQrPopup());
        }
        btnHuy.setOnAction(e -> closePopup());
        if (btnXacNhan != null) {
            btnXacNhan.setOnAction(e -> handleFinalThanhToan());
        }
        if (btnInHoaDon != null) {
            btnInHoaDon.setOnAction(e -> handleInHoaDon());
            btnInHoaDon.setDisable(false); // Disable ban đầu
            btnInHoaDon.setStyle(
            "-fx-background-color: #007bff; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 1.1em; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 10px 25px; " +
            "-fx-background-radius: 5px; " +
            "-fx-border-radius: 5px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 5, 0, 1, 2);"
            );
        }
        // 🔥 FIX BINDING LỖI: Cấu hình cột TableView sử dụng Getter truyền thống
        if (tblMonAnThanhToan != null) {
            // STT (dùng index)
            colStt.setCellValueFactory(data -> new SimpleIntegerProperty(tblMonAnThanhToan.getItems().indexOf(data.getValue()) + 1).asObject());
            // Tên món
            colTenMon.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTenMon()));
            // SL
            colSL.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getSoLuong()).asObject());
            // Đơn giá
            colDonGia.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getDonGia()).asObject());
            // Thành tiền (Tính toán)
            colThanhTien.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getThanhTien()).asObject());
        }
        // 🔥 THÊM MỚI: Tải và lắng nghe ComboBox khuyến mãi
        loadPromoComboBox();
        if (promoComboBoxPreview != null) {
            promoComboBoxPreview.valueProperty().addListener((obs, oldVal, newVal) -> {
                handlePromoSelection(newVal);
            });
        }
    }
    /**
    * 🔥 HÀM MỚI: Xử lý khi người dùng chọn Khuyến mãi
    */
    private void handlePromoSelection(String selectedName) {
        if (selectedName == null || selectedName.equals("Không áp dụng")) {
            selectedUuDai = null;
        } else {
        String simpleName = selectedName.substring(0, selectedName.indexOf(" (Giảm")).trim();
        selectedUuDai = dsUuDaiDangApDung.stream()
        .filter(ud -> ud.getTenUuDai().equals(simpleName))
        .findFirst()
        .orElse(null);
    }
    // Tính toán và hiển thị lại tổng tiền ngay lập tức
    calculateAndDisplayTotals();
}
/**
* 🔥 HÀM MỚI: Tính toán và cập nhật tất cả Label tổng tiền
*/
private void calculateAndDisplayTotals() {
    // Lấy giá trị đã lưu
    double tongMonAn = this.currentTongMonAn;
    double tienCocApDung = this.currentTienCoc;
    // Tính toán
    double phiDV = tongMonAn * 0.05;
    double vat = (tongMonAn + phiDV) * 0.08;
    double tienKhuyenMai = 0.0;
    if (selectedUuDai != null) {
        // Giả định giảm trên tổng tiền món ăn
        tienKhuyenMai = tongMonAn * (selectedUuDai.getGiaTri() / 100.0);
    }
    double tongTruocKM = tongMonAn + phiDV + vat - tienCocApDung;
    double tongThanhToan = tongTruocKM - tienKhuyenMai;
    double soTienKhachTra = tongThanhToan;
    // Cập nhật giao diện
    if (lblTongMonAn != null) lblTongMonAn.setText(currencyFormatter.format(tongMonAn));
    if (lblPhiDV != null) lblPhiDV.setText(currencyFormatter.format(phiDV));
    if (lblThueVAT != null) lblThueVAT.setText(currencyFormatter.format(vat));
    if (lblTienCoc != null) lblTienCoc.setText("-" + currencyFormatter.format(tienCocApDung));
    if (lblTongTruocKM != null) lblTongTruocKM.setText(currencyFormatter.format(tongTruocKM));
    if (lblUuDaiApDung != null) lblUuDaiApDung.setText("-" + currencyFormatter.format(tienKhuyenMai));
    if (lblTongThanhToan != null) lblTongThanhToan.setText(currencyFormatter.format(Math.max(0, tongThanhToan)));
    if (lblSoTienKhachTra != null) lblSoTienKhachTra.setText(currencyFormatter.format(Math.max(0, soTienKhachTra)));
}
/**
* 🔥 HÀM MỚI: Tải danh sách Khuyến mãi 'Đang áp dụng'
*/
private void loadPromoComboBox() {
    Response res = Client.send(CommandType.GET_PROMOS, null);
    List<UuDai> allUuDai;
    if (res.getStatusCode() == 200) {
        allUuDai = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), UuDai.class);
    } else {
    allUuDai = new ArrayList<>();
}
dsUuDaiDangApDung = allUuDai.stream()
.filter(ud -> ud.getTrangThai().equals("Đang áp dụng"))
.collect(Collectors.toList());
ObservableList<String> promoNames = FXCollections.observableArrayList();
promoNames.add("Không áp dụng");
for (UuDai ud : dsUuDaiDangApDung) {
    promoNames.add(String.format("%s (Giảm %.0f%%)", ud.getTenUuDai(), ud.getGiaTri()));
}
if (promoComboBoxPreview != null) {
    promoComboBoxPreview.setItems(promoNames);
    promoComboBoxPreview.getSelectionModel().selectFirst();
}
}
/**
* 🔥 SỬA: Cập nhật hàm để tính toán, đổ dữ liệu vào các Labels mới và BẢNG
* ĐàFIX: Đảm bảo TableView được cấu hình và đổ dữ liệu MonOrder từ MonTachSnapshot.
*/
// [Trong file ThanhToanPreviewController.java]
public void setInitialData(HoaDon hd, DatBan controller, double tongMonAn, double tienCoc, boolean isNewInvoice) {
    this.hoaDonToPay = hd;
    this.mainController = controller;
    this.isNewInvoice = isNewInvoice;
    // --- 1. LƯU GIÁ TRỊ TÍNH TOÁN (Đã đúng) ---
    this.currentTongMonAn = tongMonAn;
    this.currentTienCoc = isNewInvoice ? 0.0 : tienCoc;
    // --- 🔥 ĐàXÓA KHỎI TÍNH TOÁN CŨ (từ phiDV đến soTienKhachTra) ---
    // (Logic này đã được chuyển vào hàm calculateAndDisplayTotals())
    // --- 2. ĐỔ DỮ LIỆU CHUNG (HEADER) (Giữ nguyên) ---
    HoaDon hdGocDeLayThongTin = hd;
    if (isNewInvoice && maHDGocSnapshot != null) {
        try {
            Response res = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("invoiceId", maHDGocSnapshot));
            if (res.getStatusCode() == 200) {
                HoaDon fetchedHdGoc = utils.JsonUtil.convertValue(res.getData(), HoaDon.class);
                if (fetchedHdGoc != null) hdGocDeLayThongTin = fetchedHdGoc;
            }
        } catch (Exception e) {
        System.err.println("Lỗi khi fetch HĐ gốc cho preview: " + e.getMessage());
    }
}
String tenThuNgan = "N/A";
String khachHangSdt = hdGocDeLayThongTin.getKhachHang() != null ? hdGocDeLayThongTin.getKhachHang().getSoDT() : "N/A";
try {
    TaiKhoan tk = MainApp.getLoggedInUser();
    if (tk != null && tk.getNhanVien() != null) {
        tenThuNgan = tk.getNhanVien().getHoTen();
    }
} catch (Exception ignored) {}
// Phần Header (GridPane)
if (lblMaHD != null) lblMaHD.setText("Số HĐ : " + hd.getMaHD());
// ... (Giữ nguyên code set text cho lblNgay, lblGioVao, lblGioRa, lblThuNgan, lblKhachHang, lblBan) ...
if (lblNgay != null) lblNgay.setText("Ngày: " + (hdGocDeLayThongTin.getNgayLap() != null ? hdGocDeLayThongTin.getNgayLap().toLocalDate().toString() : "N/A"));
if (lblGioVao != null) lblGioVao.setText("Giờ vào: " + (hdGocDeLayThongTin.getGioVao() != null ? hdGocDeLayThongTin.getGioVao().toLocalTime().toString().substring(0, 5) : "N/A"));
if (lblGioRa != null) lblGioRa.setText("Giờ ra: " + java.time.LocalTime.now().toString().substring(0, 5));
if (lblThuNgan != null) lblThuNgan.setText("Thu ngân: " + tenThuNgan);
if (lblKhachHang != null) lblKhachHang.setText("Khách hàng: " + khachHangSdt);
if (lblBan != null) lblBan.setText("Bàn: " + (hd.getBan() != null ? hd.getBan().getMaBan() : "Chưa gán"));
    // --- 🔥 ĐàXÓA CÁC LỆNH .setText CŨ CHO PHẦN SUMMARY ---
if (lblThanhVien != null) {
    String memberStatus = (hdGocDeLayThongTin.getKhachHang() != null && hdGocDeLayThongTin.getKhachHang().getThanhVien().equals("VIP")) ? "Gold (giảm 10%)" : "N/A";
    lblThanhVien.setText(memberStatus);
}
// --- 3. ĐỔ DỮ LIỆU BẢNG MÓN ĂN (Giữ nguyên) ---
if (tblMonAnThanhToan != null && monTachListSnapshot != null) {
    ObservableList<MyReceiptItem> receiptList = FXCollections.observableArrayList();
    for (TachBanPopupController.MonTach mon : monTachListSnapshot) {
        int sl;
        if (isNewInvoice) {
            sl = mon.getSoLuongTach(); // SL ÃâÃÂ£ tÃÂ¡ch
        } else {
        sl = mon.getSoLuongGoc() - mon.getSoLuongTach(); // SL cÃÂ²n láÂºÂ¡i
    }
    if (sl > 0) {
        double tt = sl * mon.getDonGia();
        receiptList.add(new MyReceiptItem(mon.getTenMon(), sl, mon.getDonGia(), tt));
    }
}
tblMonAnThanhToan.setItems(receiptList);
}
// --- 4. GáÂ»ÅI HÃâ¬M TÃÂNH TOÃÂN TáÂ»âNG THáÂ»â (ÃÂÃÂ£ ÃâÃÂºng) ---
calculateAndDisplayTotals();
}
public void setMonTachList(ObservableList<TachBanPopupController.MonTach> monTachListSnapshot, String maHDGoc) {
    this.monTachListSnapshot = monTachListSnapshot;
    this.maHDGocSnapshot = maHDGoc;
}
/**
* XáÂ»Â­ lÃÂ½ xÃÂ¡c nháÂºÂ­n thanh toÃÂ¡n cuáÂ»âi cÃÂ¹ng (LÃÂ°u vÃÂ o CSDL).
* HÃÂ m nÃÂ y sáÂºÂ½ ÃâÃÂ°áÂ»Â£c gáÂ»Âi TáÂ»Âª BÃÅ N TRONG cÃÂ¡c popup thanh toÃÂ¡n (TiáÂ»Ân máÂºÂ·t, QR...).
*/
private void handleFinalThanhToan() {
    String maNV = ClientSessionManager.getInstance().getCurrentEmployee().getMaNV();
    try {
        String maUuDaiDaChon = (selectedUuDai != null) ? selectedUuDai.getMaUuDai() : null;
        // 1. GáÂ»ÅI API THANH TOÃÂN (HáÂ»â tráÂ»Â£ cáÂºÂ£ TÃÂ¡ch bÃÂ n náÂºÂ¿u cÃÂ³ maHDGocSnapshot)
        Map<String, Object> params = new HashMap<>();
        params.put("maHD", hoaDonToPay.getMaHD());
        params.put("pttt", selectedPTTT.name());
        params.put("maNhanVien", maNV);
        params.put("maUuDai", maUuDaiDaChon);
        params.put("totalAmount", Double.parseDouble(lblSoTienKhachTra.getText().replaceAll("[^0-9]", "")));
        params.put("totalFood", Double.parseDouble(lblTongMonAn.getText().replaceAll("[^0-9]", "")));
        params.put("serviceFee", Double.parseDouble(lblPhiDV.getText().replaceAll("[^0-9]", "")));
        params.put("vat", Double.parseDouble(lblThueVAT.getText().replaceAll("[^0-9]", "")));
        params.put("discount", Double.parseDouble(lblUuDaiApDung.getText().replaceAll("[^0-9]", "")));
        params.put("maBan", hoaDonToPay.getBan() != null ? hoaDonToPay.getBan().getMaBan() : "");
        if (maHDGocSnapshot != null) {
            params.put("maHDGoc", maHDGocSnapshot);
            // Convert MonTach to ChiTietHoaDon list
            List<entity.ChiTietHoaDon> itemsToMove = monTachListSnapshot.stream().map(m -> {
                entity.ChiTietHoaDon ct = new entity.ChiTietHoaDon();
                ct.setTenMon(m.getTenMon());
                ct.setSoLuong(m.getSoLuongTach());
                ct.setDonGia(m.getDonGia());
                ct.setThanhTien(m.getDonGia() * m.getSoLuongTach());
                return ct;
            }).collect(Collectors.toList());
            params.put("monTach", itemsToMove);
        }
        Response res = Client.sendWithParams(CommandType.CHECK_OUT, params);
        if (res.getStatusCode() == 200) {
            showAlert(AlertType.INFORMATION, "Thành công", "Hóa đơn đã được thanh toán thành công.");
            if (mainController != null) {
                mainController.loadTableGrids();
                mainController.loadBookingCards();
            }
            disablePopupPaymentButtons();
            if (btnInHoaDon != null) {
                btnInHoaDon.setDisable(false);
                if (isNewInvoice) {
                    btnInHoaDon.setText("Đóng (HĐ Mới)");
                    btnInHoaDon.setOnAction(e -> closePopup());
                } else {
                this.hoaDonToPay.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
                btnInHoaDon.setText("In Hóa Đơn (Final)");
                btnInHoaDon.setOnAction(e -> handleInHoaDonDaThanhToan());
            }
        }
    } else {
    showAlert(AlertType.ERROR, "Lỗi", "Không thể hoàn tất thanh toán: " + res.getMessage());
}
} catch (Exception e) {
showAlert(AlertType.ERROR, "Lỗi hệ thống", "Lỗi: " + e.getMessage());
}
}
/**
* 🔥 HÀM MỚI: Chỉ dùng để in sau khi đã bấm "Thanh toán" thành công.
* Tải lại HĐ từ CSDL để đảm bảo in bản final.
*/
    private void handleInHoaDonDaThanhToan() {
        String maHDCanIn = this.hoaDonToPay.getMaHD();
        if (maHDCanIn == null) return;
        
        try {
            Response resHD = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("invoiceId", maHDCanIn));
            if (resHD.getStatusCode() != 200) {
                showAlert(AlertType.ERROR, "Lỗi", "Không tìm thấy hóa đơn đã thanh toán để in.");
                return;
            }
            HoaDon hoaDonDeIn = utils.JsonUtil.convertValue(resHD.getData(), HoaDon.class);
            
            Response resDetails = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, Map.of("maHD", maHDCanIn));
            if (resDetails.getStatusCode() == 200) {
                List<ChiTietHoaDon> chiTiet = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resDetails.getData()), ChiTietHoaDon.class);
                hoaDonDeIn.setChiTietHoaDon(chiTiet);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HoaDonPreview_Popup.fxml"));
            Parent root = loader.load();
            HoaDonPreviewController controller = loader.getController();
            controller.setHoaDon(hoaDonDeIn);
            
            Stage stage = new Stage();
            stage.setTitle("In Hóa Đơn - " + maHDCanIn);
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
            
            closePopup(); // Đóng màn hình thanh toán sau khi mở preview in final
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Lỗi", "Không thể in hóa đơn: " + e.getMessage());
        }
    }

    // === HELPER METHODS ===
    private void showAlert(AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void closePopup() {
        if (btnHuy != null && btnHuy.getScene() != null) {
            Stage stage = (Stage) btnHuy.getScene().getWindow();
            stage.close();
        }
    }

    private void disablePopupPaymentButtons() {
        if (btnTienMat != null) btnTienMat.setDisable(true);
        if (btnNganHang != null) btnNganHang.setDisable(true);
        if (btnMoMo != null) btnMoMo.setDisable(true);
        if (btnXacNhan != null) btnXacNhan.setDisable(true);
    }

    private void moPopupThanhToanTienMat() {
        selectedPTTT = PTTThanhToan.TIEN_MAT;
        // Có thể thêm hiệu ứng highlight ở đây
        if (lblSoTienKhachTra != null) {
            System.out.println("Đã chọn: Tiền mặt. Số tiền: " + lblSoTienKhachTra.getText());
        }
    }

    private void openNganHangQrPopup() {
        selectedPTTT = PTTThanhToan.NGAN_HANG;
        // TODO: Mở popup hiển thị mã QR Ngân hàng
    }

    private void openMoMoQrPopup() {
        selectedPTTT = PTTThanhToan.VI_DIEN_TU;
        // TODO: Mở popup hiển thị mã QR MoMo
    }

    private void handleInHoaDon() {
        // Có thể in bản nháp trước khi thanh toán
        handleInHoaDonDaThanhToan();
    }

    // === INNER CLASS DTO ===
    public static class MyReceiptItem {
        private final StringProperty tenMon;
        private final SimpleIntegerProperty soLuong;
        private final SimpleDoubleProperty donGia;
        private final SimpleDoubleProperty thanhTien;

        public MyReceiptItem(String tenMon, int soLuong, double donGia, double thanhTien) {
            this.tenMon = new SimpleStringProperty(tenMon);
            this.soLuong = new SimpleIntegerProperty(soLuong);
            this.donGia = new SimpleDoubleProperty(donGia);
            this.thanhTien = new SimpleDoubleProperty(thanhTien);
        }

        public String getTenMon() { return tenMon.get(); }
        public int getSoLuong() { return soLuong.get(); }
        public double getDonGia() { return donGia.get(); }
        public double getThanhTien() { return thanhTien.get(); }
    }
}
