package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
public class TachBanPopupController {
    // ==========================================================
    // FXML FIELDS (ĐàDỌN DẸP)
    // ==========================================================
    @FXML private Label lblMaHDGoc;
    @FXML private TableView<MonTach> tblMonAnGoc;
    @FXML private TableColumn<MonTach, String> colTenMonGoc;
    @FXML private TableColumn<MonTach, Number> colSLHienTai;
    @FXML private TableColumn<MonTach, Number> colSLTach;
    @FXML private TableView<MonTach> tblMonAnMoi;
    @FXML private TableColumn<MonTach, String> colTenMonMoi;
    @FXML private TableColumn<MonTach, Number> colSLMoi;
    // NÚT CHUYỂN MÓN (TRUNG TÂM)
    @FXML private Button btnChuyenChon; // Nút chuyển 1 món (>)
    @FXML private Button btnHuyChuyen; // Nút hủy chuyển 1 món (<)
    // NÚT THANH TOÁN
    @FXML private Button btnThanhToanGoc;
    @FXML private Button btnThanhToanMoi;
    @FXML private Button btnHuy;
    @FXML private Button btnXacNhanTach; // Nút mới: Tách mà không thanh toán
    @FXML private Label lblTongTienMoi;
    // ==========================================================
    // DATA & CONTROLLERS
    // ==========================================================
    private HoaDon hoaDonGoc;
    private DatBan mainController;
    private String maHDGoc;
    private ObservableList<MonTach> allMonTach = FXCollections.observableArrayList();
    private ObservableList<MonTach> monTachList = FXCollections.observableArrayList();
    private ObservableList<MonTach> monMoiList = FXCollections.observableArrayList();
    // ==========================================================
    // INITIALIZATION & SETUP
    // ==========================================================
    @FXML
    private void initialize() {
        setupTableViews();
        setupNutActions();
        if (btnHuy != null) btnHuy.setOnAction(e -> closePopup());
        if (btnXacNhanTach != null) {
            btnXacNhanTach.setOnAction(e -> handleXacNhanTach());
            btnXacNhanTach.setDisable(true); // Disable ban đầu
        }
    }
    /**
    * Nhận dữ liệu ban đầu từ màn hình DatBan.
    */
    public void setInitialData(HoaDon hdGoc, DatBan mainCtrl) {
        this.hoaDonGoc = hdGoc;
        this.mainController = mainCtrl;
        this.maHDGoc = hdGoc.getMaHD();
        lblMaHDGoc.setText(hdGoc.getMaHD() + " (Bàn " + hdGoc.getBan().getMaBan() + ")");
        loadMonTachList(hdGoc.getMaHD());
        updateButtonState();
    }
    /**
    * Tải danh sách món ăn từ HĐ gốc và chuyển thành ViewModel MonTach.
    */
    private void loadMonTachList(String maHD) {
        Response res = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, Map.of("maHD", maHD));
        if (res.getStatusCode() == 200) {
            // Sửa: Load trực tiếp từ ChiTietHoaDon để tránh lệch field
            List<entity.ChiTietHoaDon> initialDetails = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), entity.ChiTietHoaDon.class);
            allMonTach.clear();
            for (entity.ChiTietHoaDon ct : initialDetails) {
                // Tạo MonTach từ ChiTietHoaDon
                MonTach mt = new MonTach(null, ct.getTenMon(), ct.getDonGia(), ct.getSoLuong());
                allMonTach.add(mt);
            }
        }
        updateLists();
        tblMonAnGoc.refresh();
    }
    /**
    * Thiết lập cấu trúc cho TableView.
    */
    private void setupTableViews() {
        // Bảng Gốc
        colTenMonGoc.setCellValueFactory(cell -> cell.getValue().tenMonProperty());
        colSLHienTai.setCellValueFactory(cell -> (ObservableValue<Number>) cell.getValue().soLuongConLaiProperty());
        colSLTach.setCellValueFactory(cell -> (ObservableValue<Number>) cell.getValue().soLuongTachProperty());
        tblMonAnGoc.setItems(monTachList);
        // Bảng Mới
        colTenMonMoi.setCellValueFactory(cell -> cell.getValue().tenMonProperty());
        colSLMoi.setCellValueFactory(cell -> (ObservableValue<Number>) cell.getValue().soLuongTachProperty());
        tblMonAnMoi.setItems(monMoiList);
    }
    // ==========================================================
    // CommandType HANDLERS (TÁCH MÓN)
    // ==========================================================
    /**
    * Xử lý tăng giảm số lượng món muốn tách.
    */
    private void handleTangGiamSL(MonTach mon, int delta) {
        int currentTach = mon.getSoLuongTach();
        int maxGoc = mon.getSoLuongGoc();
        int newTach = currentTach + delta;
        if (newTach >= 0 && newTach <= maxGoc) {
            mon.setSoLuongTach(newTach);
            updateLists();
            calculateTotalMoi();
            tblMonAnGoc.refresh();
            tblMonAnMoi.refresh();
        }
        updateButtonState();
    }
    /**
    * Xử lý hành động chuyển món giữa hai bảng.
    */
    private void setupNutActions() {
        // Nút Chuyển Chọn (>) - Tăng 1 đơn vị
        btnChuyenChon.setOnAction(e -> {
            MonTach selectedMon = tblMonAnGoc.getSelectionModel().getSelectedItem();
            if (selectedMon != null) {
                int slConLaiTrongGoc = selectedMon.getSoLuongConLai();
                if (slConLaiTrongGoc > 0) {
                    // Chuyển 1 món sang (delta = 1)
                    handleTangGiamSL(selectedMon, 1);
                } else {
                showAlert(AlertType.WARNING, "Lỗi", "Món này đã được chuyển hết.");
            }
        } else {
        showAlert(AlertType.WARNING, "Chọn món", "Vui lòng chọn một món trong bảng gốc có số lượng còn lại.");
    }
});
// Nút Hủy Chuyển (<) - Giảm 1 đơn vị
btnHuyChuyen.setOnAction(e -> {
    MonTach selectedMon = tblMonAnMoi.getSelectionModel().getSelectedItem();
    if (selectedMon != null) {
        // Giảm 1 món (delta = -1)
        handleTangGiamSL(selectedMon, -1);
    } else {
    showAlert(AlertType.WARNING, "Chọn món", "Vui lòng chọn một món trong bảng mới để hủy chuyển.");
}
});
// NÚT THANH TOÁN (Logic giữ nguyên, giả định FXML fields tồn tại)
if (btnThanhToanGoc != null) {
    btnThanhToanGoc.setOnAction(e -> handleThanhToanGopTach(false));
}
if (btnThanhToanMoi != null) {
    btnThanhToanMoi.setOnAction(e -> handleThanhToanGopTach(true));
}
}
/**
* Cập nhật danh sách hiển thị cho hai bảng từ allMonTach.
*/
private void updateLists() {
    // Lọc cho bảng gốc (chỉ hiển thị món có SL còn lại > 0)
    monTachList.setAll(allMonTach.stream()
    .filter(m -> m.getSoLuongConLai() > 0)
    .sorted(Comparator.comparing(MonTach::getTenMon))
    .collect(Collectors.toList()));
    // Lọc cho bảng mới (chỉ hiển thị món có SL tách > 0)
    monMoiList.setAll(allMonTach.stream()
    .filter(m -> m.getSoLuongTach() > 0)
    .sorted(Comparator.comparing(MonTach::getTenMon))
    .collect(Collectors.toList()));
}
/**
* Tính toán tổng tiền của các món đã tách.
*/
private void calculateTotalMoi() {
    double total = monMoiList.stream()
    .mapToDouble(m -> m.getDonGia() * m.getSoLuongTach())
    .sum();
    lblTongTienMoi.setText(String.format("Tổng tiền món mới: %,.0f Đ", total));
}
/**
* Cập nhật trạng thái nút Xác nhận Tách.
*/
private void updateButtonState() {
    boolean monDuocChon = monMoiList.stream().anyMatch(m -> m.getSoLuongTach() > 0);
    // Nút Thanh toán chỉ được enable sau khi có món được tách
        if (btnThanhToanMoi != null) btnThanhToanMoi.setDisable(!monDuocChon);
        if (btnXacNhanTach != null) btnXacNhanTach.setDisable(!monDuocChon);
    // Nút Thanh toán gốc luôn được enable nếu HĐ gốc có món
    if (btnThanhToanGoc != null) {
        boolean monConLai = monTachList.stream().anyMatch(m -> m.getSoLuongConLai() > 0);
        btnThanhToanGoc.setDisable(!monConLai);
    }
}
    /**
     * 🔥 HÀM MỚI: Xử lý tách bàn mà KHÔNG thanh toán ngay.
     * Sẽ tạo một hóa đơn mới cho các món được tách và giữ nguyên hóa đơn gốc.
     */
    private void handleXacNhanTach() {
        if (monMoiList.isEmpty()) {
            showAlert(AlertType.ERROR, "Lỗi", "Vui lòng chọn ít nhất một món để tách.");
            return;
        }
        
        Optional<ButtonType> result = showAlertConfirm("Xác nhận Tách Bàn", 
            "Bạn có chắc muốn tách các món này sang một bàn mới? Hóa đơn mới sẽ được tạo ở trạng thái 'Hóa đơn tạm'.");
            
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Chuyển đổi MonTach sang ChiTietHoaDon
                List<entity.ChiTietHoaDon> itemsToMove = monMoiList.stream().map(m -> {
                    entity.ChiTietHoaDon ct = new entity.ChiTietHoaDon();
                    ct.setTenMon(m.getTenMon());
                    ct.setSoLuong(m.getSoLuongTach());
                    ct.setDonGia(m.getDonGia());
                    ct.setThanhTien(m.getDonGia() * m.getSoLuongTach());
                    return ct;
                }).collect(Collectors.toList());

                // Gửi lệnh SPLIT_INVOICE lên Server
                // Dùng đúng sourceId và itemsToMove như Server mong đợi
                Response res = Client.sendWithParams(CommandType.SPLIT_INVOICE, Map.of(
                    "sourceId", maHDGoc,
                    "itemsToMove", itemsToMove
                ));

                if (res.getStatusCode() == 200) {
                    showAlert(AlertType.INFORMATION, "Thành công", "Đã tách bàn thành công. Hóa đơn mới: " + res.getData());
                    if (mainController != null) {
                        mainController.getTxtSearch().clear(); // Xóa trắng ô tìm kiếm
                        mainController.loadTableGrids();
                        mainController.loadBookingCards();
                    }
                    closePopup();
                } else {
                    showAlert(AlertType.ERROR, "Lỗi Server", "Không thể tách bàn: " + res.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Lỗi", "Lỗi khi tách bàn: " + e.getMessage());
            }
        }
    }

// ==========================================================
// XỬ LÝ THANH TOÁN GỘP TÁCH (GOM LOGIC TÁCH VÀO THANH TOÁN)
// ==========================================================
/**
* 🔥 HÀM ĐàSỬA: Xử lý TÁCH MÓN VÀ MỞ POPUP PREVIEW THANH TOÁN
* LƯU Ý: KHÔNG GỌI CSDL CHO ĐẾN KHI NÚT XÁC NHẬN CUỐI CÙNG TRONG PREVIEW ĐƯỢC NHẤN.
*/
private void handleThanhToanGopTach(boolean thanhToanMoi) {
    if (monMoiList.isEmpty() && thanhToanMoi) {
        showAlert(AlertType.ERROR, "Lỗi", "Không có món nào được tách để thanh toán Hóa đơn mới.");
        return;
    }
    if (monTachList.isEmpty() && !thanhToanMoi) {
        showAlert(AlertType.ERROR, "Lỗi", "Không còn món nào trong Hóa đơn gốc để thanh toán.");
        return;
    }
    Optional<ButtonType> result = showAlertConfirm("Xác nhận Tách Món",
    "Bạn có chắc muốn thực hiện TÁCH MÓN VÀ thanh toán? Thao tác này sẽ chuẩn bị giao dịch.");
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            // 1. TÍNH TOÁN DỮ LIỆU TÁCH MỚI (chỉ trong bộ nhớ)
            // Hóa đơn Mới (tạm thời) sẽ được tạo mã HD ngẫu nhiên để Preview
            HoaDon hdMoiTam = new HoaDon();
            // 🔥 Sửa: Không cần gọi getNextMaHD() ở đây, chỉ cần tạo mã tạm.
            hdMoiTam.setMaHD("TMP" + "001"); // Mã tạm
            // 2. Lựa chọn HĐ cần thanh toán và thông tin tiền cọc
            HoaDon hdToPreview = thanhToanMoi ? hdMoiTam : hoaDonGoc;
            // 3. Tính toán tổng tiền món ăn (cho Preview)
            double tongMonAn;
            if (thanhToanMoi) {
                tongMonAn = monMoiList.stream().mapToDouble(m -> m.getDonGia() * m.getSoLuongTach()).sum();
            } else {
            // Nếu thanh toán HĐ gốc: tính tổng số lượng món còn lại
            tongMonAn = monTachList.stream().mapToDouble(m -> m.getDonGia() * m.getSoLuongConLai()).sum();
        }
        // 4. MỞ POPUP XÁC NHẬN THANH TOÁN CUỐI CÙNG
        // Tiền cọc gốc chỉ được áp dụng nếu HĐ gốc được thanh toán (thanhToanMoi = false)
        openThanhToanPreviewPopup(hdToPreview, tongMonAn, hoaDonGoc.getTienCoc(), thanhToanMoi);
        // Sau khi Popup Thanh toán đóng, cần đóng Popup Tách Bàn (vì giao dịch đã hoàn tất)
        closePopup();
    } catch (Exception e) {
    e.printStackTrace();
    showAlert(AlertType.ERROR, "Lỗi CSDL", "Không thể chuẩn bị giao dịch Tách: " + e.getMessage());
}
}
}
/**
* 🔥 HÀM MỚI: Mở Popup Thanh toán Preview.
*/
private void openThanhToanPreviewPopup(HoaDon hdToPay, double tongMonAn, double tienCocGoc, boolean isNewInvoice) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ThanhToanPreview_Popup.fxml"));
    VBox root = loader.load();
    ThanhToanPreviewController controller = loader.getController();
    // 🔥 BƯỚC 1: TRUYỀN DỮ LIỆU TÁCH (SNAPSHOT) TRƯỚC
    ObservableList<MonTach> monTachListSnapshot = FXCollections.observableArrayList(allMonTach);
    controller.setMonTachList(monTachListSnapshot, hoaDonGoc.getMaHD()); //
    // 🔥 BƯỚC 2: GỌI SETINITIALDATA SAU (Để nó có thể sử dụng dữ liệu vừa truyền)
    controller.setInitialData(hdToPay, mainController, tongMonAn, tienCocGoc, isNewInvoice);
    Stage popupStage = new Stage();
    popupStage.setTitle("Xác nhận Thanh toán: " + hdToPay.getMaHD());
    popupStage.setScene(new Scene(root));
    popupStage.showAndWait();
}
/**
* Thực hiện giao dịch Tách Bàn (Tạo HĐ mới, cập nhật HĐ cũ).
* 🔥 HÀM NÀY ĐàBỊ XÓA KHỎI CONTROLLER, CẦN CHUYỂN LOGIC NÀY VÀO DAO!
*/
/*
private HoaDon thucHienTransactionTachBan() throws SQLException {
    // ... (Logic cũ bị loại bỏ)
}
*/
// ==========================================================
// UTILITIES
// ==========================================================
private void closePopup() {
    Stage stage = (Stage) btnHuy.getScene().getWindow();
    stage.close();
}
private Optional<ButtonType> showAlert(AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    return alert.showAndWait();
}
private Optional<ButtonType> showAlertConfirm(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    return alert.showAndWait();
}
// ==========================================================
// INNER STATIC CLASS: VIEW MODEL MON TÁCH
// ==========================================================
public static class MonTach implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String maMon_entity;
    private String tenMon_entity;
    private double donGia_entity;
    private int soLuongGoc_entity;
    private int soLuongTach_entity;
    private transient SimpleStringProperty maMon;
    private transient SimpleStringProperty tenMon;
    private transient SimpleDoubleProperty donGia;
    private transient SimpleIntegerProperty soLuongGoc;      // SL ban đầu trong HĐ cũ (Tổng SL)
    private transient SimpleIntegerProperty soLuongTach;     // SL muốn tách sang HĐ mới
    private transient SimpleIntegerProperty soLuongConLai;   // SL còn lại (soLuongGoc - soLuongTach)
    public MonTach(String maMon, String tenMon, double donGia, int soLuongGoc) {
        this.maMon_entity = maMon;
        this.tenMon_entity = tenMon;
        this.donGia_entity = donGia;
        this.soLuongGoc_entity = soLuongGoc;
        this.soLuongTach_entity = 0;
        initProperties();
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initProperties();
    }
    private void initProperties() {
        if (maMon == null) maMon = new SimpleStringProperty(maMon_entity);
        if (tenMon == null) tenMon = new SimpleStringProperty(tenMon_entity);
        if (donGia == null) donGia = new SimpleDoubleProperty(donGia_entity);
        if (soLuongGoc == null) soLuongGoc = new SimpleIntegerProperty(soLuongGoc_entity);
        if (soLuongTach == null) {
            soLuongTach = new SimpleIntegerProperty(soLuongTach_entity);
            soLuongTach.addListener((obs, oldVal, newVal) -> {
                this.soLuongTach_entity = newVal.intValue();
                if (this.soLuongConLai != null) {
                    this.soLuongConLai.set(this.soLuongGoc_entity - this.soLuongTach_entity);
                }
            });
        }
        if (soLuongConLai == null) soLuongConLai = new SimpleIntegerProperty(soLuongGoc_entity - soLuongTach_entity);
    }
    // --- Getters and Setters for Properties ---
    public String getMaMon() { return maMon.get(); }
    public String getTenMon() { return tenMon.get(); }
    public SimpleStringProperty tenMonProperty() { return tenMon; }
    public double getDonGia() { return donGia.get(); }
    public SimpleDoubleProperty donGiaProperty() { return donGia; }
    public int getSoLuongGoc() { return soLuongGoc.get(); }
    public SimpleIntegerProperty soLuongGocProperty() { return soLuongGoc; }
    public int getSoLuongTach() { return soLuongTach.get(); }
    public void setSoLuongTach(int soLuongTach) { this.soLuongTach.set(soLuongTach); }
    public SimpleIntegerProperty soLuongTachProperty() { return soLuongTach; }
    // <<< THUỘC TÍNH MỚI CHO HIỂN THỊ SỐ LƯỢNG CÒN LẠI >>>
    public int getSoLuongConLai() { return soLuongConLai.get(); }
    public SimpleIntegerProperty soLuongConLaiProperty() { return soLuongConLai; }
    // ---------------------------------------------------
    public static MonTach fromMonOrder(DatBan.MonOrder order) {
        return new MonTach(order.getMaMon(), order.getTenMon(), order.getDonGia(), order.getSoLuong());
    }
}
}