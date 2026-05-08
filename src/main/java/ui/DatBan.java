package ui;

// === IMPORTS CẦN THIẾT CHO LOGIC ĐẶT BÀN VÀ DAO ===
import entity.Ban;
import entity.HoaDon;
import entity.KhachHang;
import entity.LoaiBan;
import entity.DanhMucMon;
import entity.TrangThaiBan;
import entity.TrangThaiHoaDon;
import entity.PTTThanhToan;
import entity.TaiKhoan;
import entity.UuDai;
import network
        .CommandType;
import network.Response;
import network.Client;
import network.RealTimeClient;
import network.RealTimeEvent;
import utils.ClientSessionManager;
// Import JavaFX
import java.io.IOException;
import java.text.DecimalFormat;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.io.ByteArrayInputStream;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DatBan implements Initializable, network.RealTimeSubscriber {
    private final java.util.function.Consumer<network.RealTimeEvent> rtListener = this::handleRealTimeEvent;

    @Override
    public java.util.function.Consumer<network.RealTimeEvent> getRealTimeListener() {
        return rtListener;
    }

    private void handleRealTimeEvent(network.RealTimeEvent event) {
        String affectedId = event.getAffectedId();
        Platform.runLater(() -> {
            // Đợi một chút để DB ổn định
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
            pause.setOnFinished(e -> {
                loadTableGrids();
                loadBookingCards();

                // Nếu hóa đơn đang mở bị ảnh hưởng, nạp lại
                if (currentHoaDon != null && affectedId != null) {
                    if (affectedId.equals(currentHoaDon.getMaHD()) || "ALL".equals(affectedId)) {
                        try {
                            Response res = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", currentHoaDon.getMaHD()));
                            if (res.getStatusCode() == 200) {
                                HoaDon updated = utils.JsonUtil.convertValue(res.getData(), HoaDon.class);
                                if (updated != null) {
                                    loadHoaDonToMainInterface(updated, true);
                                }
                            }
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
            });
            pause.play();
        });
    }

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    // Data
    private ObservableList<MonOrder> monOrderList = FXCollections.observableArrayList();
    private List<DanhMucMon> dsDanhMuc;
    private ObservableList<Ban> selectedBanList = FXCollections.observableArrayList();
    private ObservableList<Button> selectedButtonList = FXCollections.observableArrayList();
    private List<HoaDon> dsHoaDonDatTrongNgay = new ArrayList<>();
    private boolean isBookingConfirmed = false;
    private HoaDon currentHoaDon = null;
    private List<HoaDon> currentHoaDonGocVaPhu = new ArrayList<>();
    private Map<String, Button> tableButtonMap = new HashMap<>();// <<< THÊM DÒNG NÀY
    private ToggleGroup menuGroup;
    private ToggleGroup paymentGroup;
    private List<UuDai> dsUuDaiDangApDung = new ArrayList<>(); // 🔥 Danh sách ưu đãi đang áp dụng
    private UuDai selectedUuDai = null; // 🔥 Ưu đãi đang được chọn
    private boolean daThanhToanCoc = false;
    // === CỘT 1 (Left Panel) ===
    @FXML
    private ComboBox<String> comboFilter;
    @FXML
    private TextField txtSearch;

    public TextField getTxtSearch() {
        return txtSearch;
    }

    @FXML
    private VBox vboxBookingCards;
    @FXML
    private ScrollPane middleScrollPane;
    @FXML
    private ScrollPane vboxReceipt;
    // === CỘT 2 (Middle Panel) ===
    @FXML
    private ComboBox<String> comboThoiGian;
    @FXML
    private DatePicker datePickerThoiGianDen;
    @FXML
    private ComboBox<String> comboKhuVuc;
    @FXML
    private Button btnTim;
    @FXML
    private GridPane gridTangTret;
    @FXML
    private GridPane gridTang1;
    @FXML
    private GridPane gridPhongRieng;
    @FXML
    private TextField txtTenKhachHang;
    @FXML
    private TextField txtSoDienThoai;
    @FXML
    private TextField txtSoLuongKhach;
    @FXML
    private TextField txtYeuCau;
    @FXML
    private TextField txtTienCoc;
    @FXML
    private Button btnThanhToanCoc;
    @FXML
    private Label lblTrangThaiBan;
    @FXML
    private Label lblBanDangChon;
    // @FXML private Button btnTimKhach; // Không có trong FXML
    @FXML
    private Button btnXacNhanBan;
    @FXML
    private ComboBox<String> comboTrangThaiHienTai; // 🔥 MỚI
    @FXML
    private Button btnCapNhatTrangThai; // 🔥 MỚI
    // === CỘT 3 (Right Panel) - GỌI MÓN & HÓA ĐƠN ===
    @FXML
    private ToggleButton toggleKhaiVi, toggleNuong, toggleLau, toggleXaoHap, toggleChien, toggleDacSan, toggleDoUong;
    @FXML
    private Button btnLuuDatHang;
    @FXML
    private Button btnThanhToan;
    @FXML
    private TextField txtTimMon;
    @FXML
    private Button btnSuaMon;
    @FXML
    private Button btnDoiBan;
    @FXML
    private Button btnTachBan;
    @FXML
    private Button btnGopBan;
    @FXML
    private Button btnHuyBan;
    @FXML
    private Button btnLuuMon; // 🔥 MỚI
    @FXML
    private Button btnLamMoi; // 🔥 MỚI
    // Table Món Ăn (Menu)
    @FXML
    private TableView<MonAn> tblMonAn;
    @FXML
    private TableColumn<MonAn, String> colTenMon;
    @FXML
    private TableColumn<MonAn, String> colHinhAnh;
    @FXML
    private TableColumn<MonAn, Number> colGia;
    @FXML
    private TableColumn<MonAn, Void> colChon;
    // Table Món Đã chọn (Order)
    @FXML
    private TableView<MonOrder> tblMonDaChon;
    @FXML
    private TableColumn<MonOrder, String> colOrderTenMon;
    @FXML
    private TableColumn<MonOrder, Number> colOrderDonGia;
    @FXML
    private TableColumn<MonOrder, Integer> colOrderSoLuong;
    @FXML
    private TableColumn<MonOrder, Void> colOrderTangGiam;
    @FXML
    private TableColumn<MonOrder, Void> colOrderHuy;
    // Hóa đơn Summary
    @FXML
    private TableView<MonOrder> tblHoaDon;
    @FXML
    private ComboBox<String> promoComboBox;
    @FXML
    private Label lblTongTienMonAn;
    @FXML
    private Label lblPhiDichVu;
    @FXML
    private Label lblThueVAT;
    @FXML
    private Label lblKhuyenMai;
    @FXML
    private Label lblTienCocSummary;
    @FXML
    private Label lblTongTienThanhToan;
    @FXML
    private Label lblSoHD;
    @FXML
    private Label lblBanHD;
    @FXML
    private Label lblThuNgan;
    @FXML
    private Label lblGioVao;
    @FXML
    private Label lblGioRa;
    // Phương thức thanh toán
    @FXML
    private ToggleButton btnTienMat;
    @FXML
    private ToggleButton btnNganHang;
    @FXML
    private ToggleButton btnMoMo;
    @FXML
    private Button btnInHoaDon;

    // =========================================================
    // HÀM KHỞI TẠO (INITIALIZE)
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ----------------------------------------------------
        // BƯỚC 1: TẢI DỮ LIỆU VÀ KHỞI TẠO CẤU TRÚC (DAO calls)
        // ----------------------------------------------------
        // --- 1. KHỞI TẠO COMBOBOX TRẠNG THàI ---
        // --- 1. KHỞI TẠO COMBOBOX TRẠNG THàI (ĐàSỬA THEO YÊU CẦU GIẢNG VIÊN) ---
        if (comboTrangThaiHienTai != null) {
            comboTrangThaiHienTai.setItems(FXCollections.observableArrayList(
                    "Chờ xác nhận", // CHO_XAC_NHAN
                    "Đã đặt", // DAT
                    "Đã nhận bàn" // DANG_SU_DUNG
            ));
        }
        if (btnCapNhatTrangThai != null) {
            btnCapNhatTrangThai.setOnAction(e -> handleCapNhatTrangThaiNhanh());
        }
        if (btnLuuMon != null) {
            btnLuuMon.setOnAction(e -> handleLuuMon());
        }
        if (btnLamMoi != null) {
            btnLamMoi.setOnAction(e -> handleLamMoi());
        }

        // Load danh mục món qua API
        Response resDM = Client.send(CommandType.GET_MENU_CATEGORIES, null);
        if (resDM.getStatusCode() == 200 && resDM.getData() != null) {
            dsDanhMuc = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resDM.getData()), DanhMucMon.class);
        }
        if (dsDanhMuc == null) {
            dsDanhMuc = new ArrayList<>();
        }
        menuGroup = new ToggleGroup();
        ToggleButton[] toggles = { toggleKhaiVi, toggleNuong, toggleLau, toggleXaoHap, toggleChien, toggleDacSan,
                toggleDoUong };
        for (int i = 0; i < toggles.length; i++) {
            if (i < dsDanhMuc.size()) {
                ToggleButton toggle = toggles[i];
                DanhMucMon dm = dsDanhMuc.get(i);
                toggle.setText(dm.getTenDM());
                toggle.setUserData(dm.getMaDM());
                toggle.setToggleGroup(menuGroup);
                toggle.setVisible(true);
                toggle.setManaged(true);
            } else {
                toggles[i].setVisible(false);
                toggles[i].setManaged(false);
            }
        }
        setupMonAnTable();
        setupMonOrderTable();
        // Load thực đơn qua API
        Response resMon = Client.send(CommandType.GET_MENU, null);
        if (resMon.getStatusCode() == 200 && resMon.getData() != null) {
            List<entity.MonAn> dsEntityMonAn = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resMon.getData()),
                    entity.MonAn.class);
            if (dsEntityMonAn != null) {
                loadMonAnTable(dsEntityMonAn.stream().map(MonAn::fromEntity).collect(Collectors.toList()));
            }
        }
        paymentGroup = new ToggleGroup();
        ToggleButton[] paymentToggles = { btnTienMat, btnNganHang, btnMoMo };
        for (ToggleButton toggle : paymentToggles) {
            toggle.setToggleGroup(paymentGroup);
        }
        // Style cho các nút thanh toán
        btnTienMat.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNganHang.setStyle("-fx-background-color: #3b5bdb; -fx-text-fill: white; -fx-font-weight: bold;");
        btnMoMo.setStyle("-fx-background-color: #a61e68; -fx-text-fill: white; -fx-font-weight: bold;");
        loadBookingCards();
        loadTableGrids();
        // ----------------------------------------------------
        // BƯỚC 2: GàN SỰ KIỆN VÀ CẬP NHẬT UI (Sử dụng @FXML fields an toàn)
        // ----------------------------------------------------
        promoComboBox
                .setItems(FXCollections.observableArrayList("Giảm 15% hóa đơn cho khách hàng VIP", "Không áp dụng"));
        promoComboBox.getSelectionModel().selectFirst();
        datePickerThoiGianDen.setValue(LocalDate.now());

        btnXacNhanBan.setOnAction(e -> handleXacNhanBan());
        btnXacNhanBan.setStyle("-fx-background-color: #2f9e44; -fx-text-fill: white; -fx-font-weight: bold;");

        // 🔥 THÊM DEBOUNCING LẮNG NGHE SỐ ĐIỆN THOẠI ĐỂ TỰ ĐỘNG ĐIỀN TÊN
        if (txtSoDienThoai != null) {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
            txtSoDienThoai.textProperty().addListener((obs, oldVal, newVal) -> {
                pause.setOnFinished(event -> {
                    if (newVal != null && newVal.length() >= 10) {
                        handleTimKhachHangNhanh(newVal);
                    }
                });
                pause.playFromStart();
            });
        }

        if (btnThanhToanCoc != null) {
            btnThanhToanCoc.setOnAction(e -> openThanhToanCocPopup());
            btnThanhToanCoc.setStyle("-fx-background-color: #2f9e44; -fx-text-fill: white; -fx-font-weight: bold;");
            btnThanhToanCoc.setDisable(true); // Vô hiệu hóa ban đầu
        }
        btnTim.setOnAction(e -> handleTimBanTrong());
        btnLuuDatHang.setOnAction(e -> handleLuuDatHang());
        btnThanhToan.setOnAction(e -> handleThanhToan());
        ToggleButton[] togglesFinal = { toggleKhaiVi, toggleNuong, toggleLau, toggleXaoHap, toggleChien, toggleDacSan,
                toggleDoUong };
        for (ToggleButton toggle : togglesFinal) {
            if (toggle != null && toggle.getUserData() != null) {
                toggle.setOnAction(e -> handleMenuToggle((String) toggle.getUserData()));
            }
        }
        if (txtTimMon != null) {
            txtTimMon.textProperty().addListener((obs, oldVal, newVal) -> handleTimMon(newVal));
        } else {
        }
        txtSoDienThoai.focusedProperty().addListener((obs, oldVal, isNowFocused) -> {
            // Chỉ chạy khi *mất focus* (!isNowFocused)
            if (!isNowFocused) {
                String sdt = txtSoDienThoai.getText().trim();
                if (sdt.isEmpty() || sdt.length() < 9) {
                    return; // Không làm gì nếu SĐT không hợp lệ
                }
                try {
                    // 🔥 Chỉ TÌM KIẾM, không TẠO MỚI (gọi hàm mới ở Bước 1)
                    Response resKH = Client.sendWithParams(CommandType.FIND_CUSTOMER_BY_PHONE, Map.of("phone", sdt));
                    KhachHang khachTimDuoc = null;
                    if (resKH.getStatusCode() == 200 && resKH.getData() != null) {
                        khachTimDuoc = utils.JsonUtil.convertValue(resKH.getData(), KhachHang.class);
                    }
                    if (khachTimDuoc != null) {
                        // Nếu tìm thấy, điọn tên khách hàng
                        // (Tên có thể là null nếu khách cũ không có tên)
                        txtTenKhachHang.setText(khachTimDuoc.getTenKH());
                    } else {
                        // Nếu không tìm thấy, XÓA TRẮNG ô tên
                        // để ngưọi dùng tự nhập tên cho KHàCH HÀNG MỚI.
                        txtTenKhachHang.clear();
                    }
                } catch (Exception e) {
                }
            }
        });
        updateSelectionLabels();
        calculateTotal();
        if (vboxReceipt != null) {
            vboxReceipt.setVisible(false);
        }
        // === THÊM MỚI: CÀI ĐẶT BỘ LỌC DANH SàCH Đĺ¶T BÀN ===
        // 1. Thêm các lựa chọn vào ComboBox lọc
        if (comboFilter != null) {
            comboFilter.setItems(FXCollections.observableArrayList(
                    "Tất cả",
                    "Chờ xác nhận",
                    "Đã đặt",
                    "Đang phục vụ"));
            comboFilter.setValue("Tất cả"); // Đặt giá trị mặc định
            // 2. Gán sự kiện: Khi thay đổi filter, tải lại danh sách
            comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadBookingCards());
        }
        // 3. Gán sự kiện: Khi gõ vào ô tìm kiếm, tải lại danh sách
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> loadBookingCards());
        }
        loadPromoComboBox();
        if (promoComboBox != null) {
            // Sự kiện: Khi chọn khuyến mãi, tính toán lại tổng tiọn
            promoComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handlePromoSelection(newVal));
        }
        // === THÊM MỚI: GàN SỰ KIỆN CHO CàC NÚT CHỨC NĂNG MỚI ===
        btnSuaMon.setOnAction(e -> handleSuaHoaDon());
        btnDoiBan.setOnAction(e -> openDoiBanPopup());
        btnHuyBan.setOnAction(e -> handleHuyBan());
        // === GàN SỰ KIỆN CHO CàC NÚT CHỨC NĂNG MỚI ===
        btnSuaMon.setOnAction(e -> handleSuaHoaDon());
        btnDoiBan.setOnAction(e -> openDoiBanPopup());
        btnTachBan.setOnAction(e -> openTachBanPopup()); // << SỰ KIệN MỚI CHO TàCH BÀN
        btnGopBan.setOnAction(e -> openGopBanPopup());
        btnHuyBan.setOnAction(e -> handleHuyBan());
        btnInHoaDon.setOnAction(e -> handleInHoaDon());
        btnInHoaDon.setStyle("-fx-background-color: #f08c00; -fx-text-fill: white; -fx-font-weight: bold;");
        if (btnTienMat != null) {
            // 🔥 SỬA TÊN HÀM
            btnTienMat.setOnAction(e -> moPopupThanhToanTienMat());
        }
        if (btnNganHang != null) {
            // 🔥 SỬA TÊN HÀM
            btnNganHang.setOnAction(e -> openNganHangQrPopup());
        }
        if (btnMoMo != null) {
            btnMoMo.setOnAction(e -> openMoMoQrPopup());
        }
        // === THÊM MỚI: ĐẶT TRẠNG THàI NÚT BAN ĐẦU ===
        updateButtonVisibility(false); // Ban đầu là trạng thái TẠO MỚI
        // --- KHỞI TẠO COMBOBOX GIỜ (Cách nhau 30 phút) ---
        ObservableList<String> timeSlots = FXCollections.observableArrayList();
        LocalTime startTime = LocalTime.of(8, 0); // Mở cửa lúc 8h sáng
        LocalTime endTime = LocalTime.of(22, 0); // Đĳ³ng cửa lúc 10h tối
        while (!startTime.isAfter(endTime)) {
            timeSlots.add(startTime.format(timeFormatter));
            startTime = startTime.plusMinutes(30);
        }
        if (comboThoiGian != null) {
            comboThoiGian.setItems(timeSlots);
            // Set giờ hiện tại (làm tròn lên 30p tiếp theo)
            LocalTime now = LocalTime.now();
            if (now.getMinute() > 30) {
                comboThoiGian.setValue(now.plusHours(1).withMinute(0).format(timeFormatter));
            } else {
                comboThoiGian.setValue(now.withMinute(30).format(timeFormatter));
            }
        }
        // --- 🔥 THÊM MỚI: Khởi tạo ComboBox Khu Vực ---
        if (comboKhuVuc != null) {
            comboKhuVuc.setItems(FXCollections.observableArrayList(
                    "Tự động", // Máy tự tính theo số ngưọi
                    "Tầng trệt",
                    "Tầng 1",
                    "Phòng riêng",
                    "Tất cả" // Hiện hết không lọc
            ));
            comboKhuVuc.setValue("Tự động"); // Mặc định để máy tính
        }
        datePickerThoiGianDen.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                filterGioTheoNgay(newVal);
        });

        // Đăng ký nhận sự kiện real-time
        RealTimeClient.getInstance().addListener(rtListener);
        
        Platform.runLater(() -> {
            if (vboxBookingCards.getScene() != null) {
                // Sử dụng addEventFilter để giành quyền ưu tiên xử lý phím trước hệ thống
                vboxBookingCards.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    // --- NHÓM PHÀM F (Ưu tiên xử lý và consume để tránh trùng) ---
                    switch (event.getCode()) {
                        case F12 -> {
                            hienThiTroGiupPhimTat(); // Hiện bảng tra cứu
                            event.consume(); // Chặn không cho Windows/WebView mở Help mặc định
                        }
                        case F1 -> {
                            // help
                        }
                        case F2 -> {
                            handleXacNhanBan(); // Khóa bàn đang chọn
                            event.consume();
                        }
                        case F3 -> {
                            txtSoDienThoai.requestFocus(); // Nhảy tới ô SĐT
                            event.consume();
                        }
                        case F4 -> {
                            txtTimMon.requestFocus(); // Nhảy tới ô tìm món
                            event.consume();
                        }
                        case F5 -> {
                            handleTimBanTrong(); // Làm mới sơ đồ bàn
                            event.consume();
                        }
                        default -> {
                        }
                    }
                    // --- NHÓM PHÀM CTRL (Nghiệp vụ phức tạp) ---
                    if (event.isControlDown()) {
                        switch (event.getCode()) {
                            case S -> handleLuuDatHang(); // Lưu hóa đơn
                            case T -> openTachBanPopup(); // Tách bàn
                            case G -> openGopBanPopup(); // Gộp bàn
                            case D -> openDoiBanPopup(); // Đổi bàn
                            case P -> handleInHoaDon(); // In hóa đơn
                            case ENTER -> handleThanhToan(); // Thanh toán nhanh
                            default -> {
                            }
                        }
                        event.consume();
                    }
                    // --- NHÓM PHÀM XÓA & THOàT ---
                    if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                        MonOrder selectedOrder = tblMonDaChon.getSelectionModel().getSelectedItem();
                        if (selectedOrder != null) {
                            monOrderList.remove(selectedOrder); // Xóa món trong giỏ
                            calculateTotal();
                        } else {
                            handleHuyBan(); // Nếu không chọn món thì hỏi hủy bàn
                        }
                        event.consume();
                    }
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        clearFormDatBan(); // Thoát/Xóa trắng form
                        event.consume();
                    }
                });
            }
        });

    }

    // =========================================================
    // HÀM NGHIỆP VỤ TàCH BÀN (CHàNH)
    // =========================================================
    // ui.DatBan.java
    private void hienThiTroGiupPhimTat() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tứ Hữu - Danh mục phím tắt");
        alert.setHeaderText("Hỗ trợ thao tác nhanh cho Thu ngân");
        alert.setContentText(
                " Ctrl + Enter: Mở bảng thanh toán\n\n" +
                        " Delete: Xóa món đang chọn hoặc Hủy bàn\n" +
                        " ESC: Hủy chọn bàn và xóa trắng form");
        alert.showAndWait();
    }

    /**
     * 🔥 HÀM MỚI: Cập nhật trạng thái từ ComboBox (Có Validate Logic)
     */
    private void handleCapNhatTrangThaiNhanh() {
        if (currentHoaDon == null || currentHoaDon.getMaHD() == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng chọn một hóa đơn để cập nhật.");
            return;
        }
        String trangThaiMoiDisplay = comboTrangThaiHienTai.getValue();
        if (trangThaiMoiDisplay == null)
            return;
        // 1. Map từ tên hiển thị Tiếng Việt vọDB Value
        String trangThaiMoiDb = "";
        if (trangThaiMoiDisplay.equals("Đã nhận bàn"))
            trangThaiMoiDb = TrangThaiHoaDon.DANG_SU_DUNG.getDbValue();
        else if (trangThaiMoiDisplay.equals("Đã đặt"))
            trangThaiMoiDb = TrangThaiHoaDon.DAT.getDbValue();
        else if (trangThaiMoiDisplay.equals("Chờ xác nhận"))
            trangThaiMoiDb = TrangThaiHoaDon.CHO_XAC_NHAN.getDbValue();
        else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Trạng thái không hợp lệ.");
            return;
        }
        // 2. 🔥 LOGIC CHẶN LÙI TRẠNG THàI (Quan trọng)
        String trangThaiHienTaiDb = currentHoaDon.getTrangThai().getDbValue();
        // Nếu đang là ĐàĐẶT hoặc ĐàNHẬN BÀN -> Không được vọCHỜ XàC NHẬN
        if ((trangThaiHienTaiDb.equals(TrangThaiHoaDon.DAT.getDbValue()) ||
                trangThaiHienTaiDb.equals(TrangThaiHoaDon.DANG_SU_DUNG.getDbValue()))
                && trangThaiMoiDb.equals(TrangThaiHoaDon.CHO_XAC_NHAN.getDbValue())) {
            showAlert(Alert.AlertType.WARNING, "Sai quy trình",
                    "Hóa đơn đã được xác nhận hoặc đang phục vụ.\nKhông thể quay lại trạng thái 'Chờ xác nhận'.");
            // Reset lại combobox vọcũ
            loadHoaDonToMainInterface(currentHoaDon, false);
            return;
        }
        try {
            // 3. Cập nhật Hóa đơn qua API
            Response res = Client.sendWithParams(CommandType.CANCEL_INVOICE, Map.of(
                    "maHD", currentHoaDon.getMaHD(),
                    "trangThai", trangThaiMoiDb,
                    "setGioRa", false));
            if (res.getStatusCode() == 200) {
                // Cập nhật lại đối tượng hiện tại để đồng bộ
                currentHoaDon.setTrangThai(trangThaiMoiDb);
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã cập nhật trạng thái thành: " + trangThaiMoiDisplay);
                // 5. Refresh giao diện
                loadHoaDonToMainInterface(currentHoaDon, false);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật: " + res.getMessage());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái: " + e.getMessage());
        }
    }

    /**
     * 🔥 HÀM SỬA CUọI CÙNG: Xử lý nút In Hóa đơn (In Hóa đơn TẠM TàNH).
     * Cho phép in nếu HĐ đang ở trạng thái ĐANG SỬ DỤNG hoặc ĐàĐẶT (Chưa thanh
     * toán).
     */
    private void handleInHoaDon() {
        if (currentHoaDon == null) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn hóa đơn để in.");
            return;
        }

        try {
            // 1. Tải chi tiết hóa đơn từ Server
            Response resDetails = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS,
                    Map.of("maHD", currentHoaDon.getMaHD()));
            if (resDetails.getStatusCode() == 200) {
                List<entity.ChiTietHoaDon> details = utils.JsonUtil.fromJsonList(
                        utils.JsonUtil.toJson(resDetails.getData()),
                        entity.ChiTietHoaDon.class);
                currentHoaDon.setChiTietHoaDon(details);
            }

            // 2. Mở giao diện Preview
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HoaDonPreview_Popup.fxml"));
            Parent root = loader.load();

            HoaDonPreviewController controller = loader.getController();
            controller.setHoaDon(currentHoaDon);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Xem trước hóa đơn - " + currentHoaDon.getMaHD());
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi in ấn", "Không thể mở bản xem trước: " + e.getMessage());
        }
    }

    /**
     * 🔥 HÀM SỬA CUọI CÙNG CHO MOMO: Mở Popup hiển thị QR Thanh toán (Sử dụng
     * LOGO và BIN BVBank).
     * === ĐàTHÊM: Tên tài khoản và Số tài khoản ===
     */
    private void openMoMoQrPopup() {
        if (currentHoaDon == null || currentHoaDon.getMaHD() == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Không có mã hóa đơn hợp lệ để tạo QR.");
            return;
        }
        // 1. Chuẩn bị dữ liệu thanh toán
        calculateTotal();
        double tempTotal = 0;
        try {
            String amountStr = lblTongTienThanhToan.getText().replaceAll("[^0-9]", "");
            tempTotal = Double.parseDouble(amountStr.isEmpty() ? "0" : amountStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xác định tổng tiền thanh toán.");
            return;
        }
        final double tongTienThanhToan = tempTotal;
        if (tongTienThanhToan <= 0) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Tổng tiền thanh toán phải lớn hơn 0.");
            return;
        }
        // 2. Thông tin Tĩnh (Cập nhật BIN BVBank)
        final String YOUR_BANK_CODE = "970454"; // 🔥 BIN BVBank ĐàSỬA
        final String YOUR_ACCOUNT_NUMBER = "99MM24030M69605648"; // Số tài khoản ảo MoMo/BVBank
        // === THÊM MỚI: TÊN TÀI KHOẢN ===
        final String YOUR_ACCOUNT_NAME = "MOMO_LECONGCHUNG"; // <<< THAY TÊN CHỦ TK CỦA BẠN VÀO ĐÂY
        String maHD = currentHoaDon.getMaHD();
        String rawContent = "TT" + maHD.toUpperCase().replace(" ", "_");
        String encodedContent;
        try {
            encodedContent = java.net.URLEncoder.encode(rawContent, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedContent = rawContent;
        }
        // 3. Tạo URL Quicklink (Dùng Quicklink VietQR)
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact.png?amount=%d&addInfo=%s",
                YOUR_BANK_CODE,
                YOUR_ACCOUNT_NUMBER,
                (int) Math.ceil(tongTienThanhToan),
                encodedContent);
        // 4. Tải ảnh QR từ Internet
        Image qrImage = generateQrCodeImageFromUrl(qrUrl, 250);
        if (qrImage == null || qrImage.isError()) {
            return;
        }
        // 5. Tạo giao diện Popup
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(250);
        qrView.setFitHeight(250);
        // TẢI VÀ THAY THẾ LABEL BẰNG LOGO MOMO
        ImageView logoView = new ImageView();
        try {
            Image logoMomo = new Image(getClass().getResourceAsStream("/images/MoMo_Logo.png"));
            logoView.setImage(logoMomo);
            logoView.setFitHeight(40);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            return;
        }
        Label lblAmount = new Label("Số tiền: " + String.format("%,.0f Đ", tongTienThanhToan));
        lblAmount.setStyle("-fx-font-size: 1.2em; -fx-font-weight: 500; -fx-text-fill: red;");
        // === THÊM MỚI: TÊN VÀ SọTÀI KHOẢN ===
        Label lblAccountName = new Label("Tên tài khoản: " + YOUR_ACCOUNT_NAME);
        lblAccountName.setStyle("-fx-font-size: 1.1em; -fx-font-weight: 500; -fx-text-fill: #333;");
        Label lblAccountNumber = new Label("Số tài khoản: " + YOUR_ACCOUNT_NUMBER);
        lblAccountNumber.setStyle("-fx-font-size: 1.1em; -fx-font-weight: 500; -fx-text-fill: #333;");
        // === KẾT THÚC THÊM MỚI ===
        Label lblContent = new Label("Nội dung: " + rawContent);
        lblContent.setStyle("-fx-font-size: 1.0em; -fx-font-weight: 400;");
        // Tạo nút Hủy và Xác nhận (Màu sắc MoMo)
        Button btnHuy = new Button("Hủy");
        Button btnXacNhanThanhToan = new Button("Xác nhận đã thanh toán");
        btnHuy.setStyle(
                "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-font-weight: bold;");
        btnXacNhanThanhToan.setStyle(
                "-fx-background-color: #b0006d; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-font-weight: bold;");
        HBox buttonBox = new HBox(15, btnHuy, btnXacNhanThanhToan);
        buttonBox.setAlignment(Pos.CENTER);
        // === SỬA LẠI LAYOUT: Thêm 2 label mới vào infoBox ===
        VBox infoBox = new VBox(8, lblAmount, lblAccountName, lblAccountNumber, lblContent);
        infoBox.setAlignment(Pos.CENTER);
        VBox root = new VBox(15, logoView, qrView, infoBox, buttonBox); // Giảm spacing
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(400, 600); // Tăng chiọu cao
        Stage popupStage = new Stage();
        popupStage.setTitle("Thanh toán MoMo/VietQR");
        popupStage.setScene(new Scene(root));
        // GàN SỰ KIỆN CHO NÚT (Giữ nguyên)
        btnHuy.setOnAction(e -> popupStage.close());
        btnXacNhanThanhToan.setOnAction(e -> {
            // (Logic xác nhận thanh toán giữ nguyên)
            PTTThanhToan pttt = PTTThanhToan.VI_DIEN_TU;
            String maHDCanThanhToan = currentHoaDon.getMaHD();
            String maNV = "NV_LOI"; // Mã mặc định nếu lỗi
            try {
                // Lấy tài khoản đang đăng nhập từ MainApp
                TaiKhoan tk = MainApp.getLoggedInUser();
                // Kiểm tra và lấy maNV (Giả định NhanVien entity có getMaNV())
                if (tk != null && tk.getNhanVien() != null && tk.getNhanVien().getMaNV() != null) {
                    maNV = tk.getNhanVien().getMaNV();
                } else {
                    // Gán một mã NV mặc định hoặc báo lỗi tùy nghiệp vụ
                    maNV = "NV_DEFAULT";
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (maHDCanThanhToan != null) {
                Response resPay = Client.sendWithParams(CommandType.CHECKOUT, Map.of(
                        "maHD", maHDCanThanhToan,
                        "maNhanVien", maNV,
                        "pttt", pttt.getDbValue(),
                        "maBan", currentHoaDon.getMaBan() != null ? currentHoaDon.getMaBan() : "",
                        "totalAmount", tongTienThanhToan));
                if (resPay.getStatusCode() == 200) {
                    Response resHd = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID,
                            Map.of("maHD", maHDCanThanhToan));
                    HoaDon hdDaThanhToan = null;
                    if (resHd.getStatusCode() == 200) {
                        hdDaThanhToan = utils.JsonUtil.convertValue(resHd.getData(), HoaDon.class);
                    }
                    if (hdDaThanhToan != null) {
                        currentHoaDon = hdDaThanhToan;
                        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                "Hóa đơn " + maHDCanThanhToan + " đã được thanh toán bằng " + pttt.getDisplayName());
                        loadDsBan();
                        disableMiddleActionButtons();
                        disablePaymentButtons();
                        loadBookingCards();
                        handleThanhToan();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã thanh toán, nhưng không tải lại được hóa đơn mới.");
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái hóa đơn.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Không có hóa đơn đang chọn.");
            }
            popupStage.close();
        });
        popupStage.show();
    }

    /**
     * 🔥 HÀM SỬA: Chỉ vô hiệu hóa các nút Phương thức Thanh toán (trên Receipt
     * Panel).
     * Giữ nguyên các thông tin HĐ và kích hoạt nút In Hóa đơn.
     */
    private void disablePaymentButtons() {
        // 1. Vô hiệu hóa nút Thanh Toán (chính) và các nút PTTT (toggle buttons)
        if (btnThanhToan != null)
            btnThanhToan.setDisable(true);
        if (btnTienMat != null)
            btnTienMat.setDisable(true);
        if (btnNganHang != null)
            btnNganHang.setDisable(true);
        if (btnMoMo != null)
            btnMoMo.setDisable(true);
        // 2. Kích hoạt nút In Hóa đơn 🔥
        if (btnInHoaDon != null) {
            btnInHoaDon.setDisable(false);
        }
    }

    /**
     * 🔥 HÀM MỚI: Vô hiệu hóa các nút thao tác nghiệp vụ (Panel Giữa).
     * Đĺ£m bảo các nút Sửa/Đổi/Gộp/Tách/Hủy không dùng được sau khi thanh toán.
     */
    private void disableMiddleActionButtons() {
        // 🔥 GIẢ ĐỊNH CàC BIẾN FXML NÀY ĐàĐƯỢC KHAI BàO
        // @FXML private Button btnSuaMon;
        // @FXML private Button btnDoiBan;
        // @FXML private Button btnTachBan;
        // @FXML private Button btnGopBan;
        // @FXML private Button btnHuyBan;
        if (btnSuaMon != null)
            btnSuaMon.setDisable(true);
        if (btnDoiBan != null)
            btnDoiBan.setDisable(true);
        if (btnTachBan != null)
            btnTachBan.setDisable(true);
        if (btnGopBan != null)
            btnGopBan.setDisable(true);
        if (btnHuyBan != null)
            btnHuyBan.setDisable(true);
    }

    // [Thêm hàm mới này vào file DatBan.java]
    /**
     * 🔥 HÀM MỚI: Mở Popup hiển thị QR Thanh toán Tiọn Cọc.
     * Chỉ hoạt động khi Hóa đơn đang ở trạng thái "Chọxác nhận".
     */
    private void openThanhToanCocPopup() {
        // 1. Kiểm tra hóa đơn và trạng thái
        if (currentHoaDon == null || currentHoaDon.getMaHD() == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng chọn một Hóa đơn để thanh toán cọc.");
            return;
        }
        if (currentHoaDon.getTrangThai() != TrangThaiHoaDon.CHO_XAC_NHAN) {
            showAlert(Alert.AlertType.WARNING, "Lỗi",
                    "Chỉ có thể thanh toán cọc cho Hóa đơn đang ở trạng thái 'Chờ xác nhận'.");
            return;
        }
        // 2. Lấy số tiọn cọc từ TextField
        double tienCoc = 0;
        try {
            String tienCocRaw = txtTienCoc.getText().replaceAll("[^0-9.]", "");
            tienCoc = Double.parseDouble(tienCocRaw.isEmpty() ? "0" : tienCocRaw);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiọn cọc không hợp lệ.");
            return;
        }
        if (tienCoc <= 0) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiọn cọc phải lớn hơn 0.");
            return;
        }
        // 3. Thông tin QR (Giống Ngân hàng, chỉ khác nội dung)
        final String YOUR_BANK_CODE = "970422"; // MB Bank BIN (Ví dụ)
        final String YOUR_ACCOUNT_NUMBER = "0927432020905"; // Số tài khoản của bạn
        String maHD = currentHoaDon.getMaHD();
        // Nội dung: Rõ ràng là thanh toán cọc
        String rawContent = "Coc" + maHD.toUpperCase().replace(" ", "_"); // VD: CocHD079
        String encodedContent;
        try {
            encodedContent = java.net.URLEncoder.encode(rawContent, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedContent = rawContent;
        }
        // 4. Tạo URL Quicklink
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact.png?amount=%d&addInfo=%s",
                YOUR_BANK_CODE,
                YOUR_ACCOUNT_NUMBER,
                (int) Math.ceil(tienCoc), // Số tiọn cọc
                encodedContent);
        // 5. Tải ảnh QR
        Image qrImage = generateQrCodeImageFromUrl(qrUrl, 250);
        if (qrImage == null || qrImage.isError()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                    "Không thể tải mã QR VietQR. Vui lòng kiểm tra lại kết nối.");
            return;
        }
        // 6. Tạo giao diện Popup (Tương tự Ngân hàng)
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(250);
        qrView.setFitHeight(250);
        Label lblTitle = new Label("Quét mã chuyển tiền hóa đơn: " + maHD);
        lblTitle.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-text-fill: #007bff;");
        Label lblAmount = new Label("Số tiền cọc: " + String.format("%,.0f Đ", tienCoc)); // Sửa text
        lblAmount.setStyle("-fx-font-size: 1.2em; -fx-font-weight: 500; -fx-text-fill: red;");
        Label lblContent = new Label("Nội dung: " + rawContent);
        lblContent.setStyle("-fx-font-size: 1.0em; -fx-font-weight: 400;");
        Button btnHuy = new Button("Hủy");
        Button btnXacNhanCoc = new Button("Xác nhận đã cọc"); // Sửa text nút
        btnHuy.setStyle(
                "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-font-weight: bold;");
        btnXacNhanCoc.setStyle(
                "-fx-background-color: #ffc107; -fx-text-fill: black; -fx-padding: 10px 20px; -fx-font-weight: bold;"); // Màu
                                                                                                                        // vàng
                                                                                                                        // cho
                                                                                                                        // cọc
        HBox buttonBox = new HBox(15, btnHuy, btnXacNhanCoc);
        buttonBox.setAlignment(Pos.CENTER);
        VBox root = new VBox(20, lblTitle, qrView, lblAmount, lblContent, buttonBox);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(400, 550);
        Stage popupStage = new Stage();
        popupStage.setTitle("Thanh toán tiền cọc cho HD: " + maHD); // Sửa tiêu đề cửa sổ
        popupStage.setScene(new Scene(root));
        // 7. GÁN SỰ KIỆN CHO NÚT
        btnHuy.setOnAction(e -> popupStage.close());
        btnXacNhanCoc.setOnAction(e -> {
            String maHDCanXacNhan = currentHoaDon.getMaHD();
            try {
                // 🔥 GỌI DAO ĐỂ CẬP NHẬT TRẠNG THÁI TỪ "ChoXacNhan" -> "Dat"
                Response resConfirm = Client.sendWithParams(CommandType.CONFIRM_DEPOSIT,
                        Map.of("maHD", maHDCanXacNhan));
                if (resConfirm.getStatusCode() == 200) {
                    // Tải lại HĐ để lấy trạng thái mới
                    Response resHd = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID,
                            Map.of("maHD", maHDCanXacNhan));
                    HoaDon hdDaDat = null;
                    if (resHd.getStatusCode() == 200) {
                        hdDaDat = utils.JsonUtil.convertValue(resHd.getData(), HoaDon.class);
                    }
                    if (hdDaDat != null) {
                        currentHoaDon = hdDaDat; // Cập nhật HĐ hiện tại
                        this.daThanhToanCoc = true;
                        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                "Đã xác nhận thanh toán cọc cho Hóa đơn " + maHDCanXacNhan
                                        + ".\nTrạng thái chuyển thành: Đã đặt.");
                        // Cập nhật giao diện chính
                        loadBookingCards(); // Cập nhật card list
                        loadTableGrids(); // Cập nhật màu bàn
                        // Cập nhật lại form nếu cần (trạng thái HĐ)
                        loadHoaDonToMainInterface(currentHoaDon, false); // Tải lại để cập nhật nút
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã xác nhận cọc, nhưng không tải lại được hóa đơn.");
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi",
                            "Không thể cập nhật trạng thái hóa đơn (có thể HĐ không ở trạng thái 'Chờ xác nhận').");
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi CSDL", "Lỗi khi xác nhận tiền cọc: " + ex.getMessage());
                ex.printStackTrace();
            }
            popupStage.close();
        });
        popupStage.show();
    }

    // 🔥 HÀM loadDsBan() TẠI VỊ TRÍ CŨ (giữ nguyên nếu bạn có)
    public void loadDsBan() {
        // Gọi lại hàm vẽ lưới bàn của bạn
        loadTableGrids();
    }

    /**
     * 🔥 HÀM SỬA CUỐI CÙNG: Mở Popup hiển thị QR Thanh toán Ngân hàng.
     * === ĐàTHÊM: Tên tài khoản và Số tài khoản ===
     */
    private void openNganHangQrPopup() {
        if (currentHoaDon == null || currentHoaDon.getMaHD() == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Không có mã hóa đơn hợp lệ để tạo QR.");
            return;
        }
        // 1. Chuẩn bị dữ liệu thanh toán
        calculateTotal();
        double tempTotal = 0;
        try {
            String amountStr = lblTongTienThanhToan.getText().replaceAll("[^0-9]", "");
            tempTotal = Double.parseDouble(amountStr.isEmpty() ? "0" : amountStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xác định tổng tiền thanh toán.");
            return;
        }
        final double tongTienThanhToan = tempTotal;
        if (tongTienThanhToan <= 0) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Tổng tiền thanh toán phải lớn hơn 0.");
            return;
        }
        // 2. Thông tin tĩnh và động
        final String YOUR_BANK_CODE = "970422"; // MB Bank BIN
        final String YOUR_ACCOUNT_NUMBER = "0927432020905"; // Số tài khoản của bạn.
        // === THÊM MỚI: TÊN TÀI KHOẢN ===
        final String YOUR_ACCOUNT_NAME = "LÊ CÔNG CHUNG"; // <<< THAY TÊN CHỦ TK CỦA BẠN VÀO ĐÂY
        String maHD = currentHoaDon.getMaHD();
        String rawContent = "TT" + maHD.toUpperCase().replace(" ", "_");
        String encodedContent;
        try {
            encodedContent = java.net.URLEncoder.encode(rawContent, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedContent = rawContent;
        }
        // 3. Tạo URL Quicklink
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact.png?amount=%d&addInfo=%s",
                YOUR_BANK_CODE,
                YOUR_ACCOUNT_NUMBER,
                (int) Math.ceil(tongTienThanhToan),
                encodedContent);
        // 4. Tải ảnh QR từ Internet
        Image qrImage = generateQrCodeImageFromUrl(qrUrl, 250);
        if (qrImage == null || qrImage.isError()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                    "Không thể tải mã QR từ Quicklink VietQR. Vui lòng kiểm tra lại kết nối.");
            return;
        }
        // 5. Tạo giao diện Popup
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(250);
        qrView.setFitHeight(250);
        Label lblTitle = new Label("Quét Mã Thanh Toán VietQR");
        lblTitle.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;");
        Label lblAmount = new Label("Số tiền: " + String.format("%,.0f Đ", tongTienThanhToan));
        lblAmount.setStyle("-fx-font-size: 1.2em; -fx-font-weight: 500; -fx-text-fill: red;");
        // === THÊM MỚI: TÊN VÀ SỐ TÀI KHOẢN ===
        Label lblAccountName = new Label("Tên tài khoản: " + YOUR_ACCOUNT_NAME);
        lblAccountName.setStyle("-fx-font-size: 1.1em; -fx-font-weight: 500; -fx-text-fill: #333;");
        Label lblAccountNumber = new Label("Số tài khoản: " + YOUR_ACCOUNT_NUMBER);
        lblAccountNumber.setStyle("-fx-font-size: 1.1em; -fx-font-weight: 500; -fx-text-fill: #333;");
        // === KẾT THÚC THÊM MỚI ===
        Label lblContent = new Label("Nội dung: " + rawContent);
        lblContent.setStyle("-fx-font-size: 1.0em; -fx-font-weight: 400;");
        // TẠO NÚT HỦY VÀ XÁC NHẬN VÀ ÁP DỤNG MÀU SẮC
        Button btnHuy = new Button("Hủy");
        Button btnXacNhanThanhToan = new Button("Xác nhận đã thanh toán");
        btnHuy.setStyle(
                "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-font-weight: bold;");
        btnXacNhanThanhToan.setStyle(
                "-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-font-weight: bold;");
        HBox buttonBox = new HBox(15, btnHuy, btnXacNhanThanhToan);
        buttonBox.setAlignment(Pos.CENTER);
        // === SỬA LẠI LAYOUT: Thêm 2 label mới vào infoBox ===
        VBox infoBox = new VBox(8, lblAmount, lblAccountName, lblAccountNumber, lblContent);
        infoBox.setAlignment(Pos.CENTER);
        VBox root = new VBox(15, lblTitle, qrView, infoBox, buttonBox); // Giảm spacing
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(400, 600); // Tăng chiều cao 1 chút
        Stage popupStage = new Stage();
        popupStage.setTitle("Thanh toán Ngân hàng");
        popupStage.setScene(new Scene(root));
        // 6. GÁN SỰ KIỆN CHO NÚT (Giữ nguyên)
        btnHuy.setOnAction(e -> popupStage.close());
        btnXacNhanThanhToan.setOnAction(e -> {
            // (Logic xác nhận thanh toán giữ nguyên)
            PTTThanhToan pttt = PTTThanhToan.NGAN_HANG;
            String maHDCanThanhToan = currentHoaDon.getMaHD();
            String maNV = "NV_LOI"; // Mã mặc định nếu lỗi
            try {
                // Lấy tài khoản đang đăng nhập từ MainApp
                TaiKhoan tk = MainApp.getLoggedInUser();
                // Kiểm tra và lấy maNV (Giả định NhanVien entity có getMaNV())
                if (tk != null && tk.getNhanVien() != null && tk.getNhanVien().getMaNV() != null) {
                    maNV = tk.getNhanVien().getMaNV();
                } else {
                    // Gán một mã NV mặc định hoặc báo lỗi tùy nghiệp vụ
                    maNV = "NV_DEFAULT";
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (maHDCanThanhToan != null) {
                Response resPay = Client.sendWithParams(CommandType.CHECKOUT, Map.of(
                        "maHD", maHDCanThanhToan,
                        "maNhanVien", maNV,
                        "pttt", pttt.getDbValue(),
                        "maBan", currentHoaDon.getMaBan() != null ? currentHoaDon.getMaBan() : "",
                        "totalAmount", tongTienThanhToan));
                if (resPay.getStatusCode() == 200) {
                    Response resHd = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID,
                            Map.of("maHD", maHDCanThanhToan));
                    HoaDon hdDaThanhToan = null;
                    if (resHd.getStatusCode() == 200) {
                        hdDaThanhToan = utils.JsonUtil.convertValue(resHd.getData(), HoaDon.class);
                    }
                    if (hdDaThanhToan != null) {
                        currentHoaDon = hdDaThanhToan;
                        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                "Hóa đơn " + maHDCanThanhToan + " đã được thanh toán bằng " + pttt.getDisplayName());
                        loadDsBan();
                        disableMiddleActionButtons();
                        disablePaymentButtons();
                        loadBookingCards();
                        handleThanhToan();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã thanh toán, nhưng không tải lại được hóa đơn mới.");
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái hóa đơn.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Không có hóa đơn đang chọn.");
            }
            popupStage.close();
        });
        popupStage.show();
    }

    /**
     * 🔥 HÀM MỚI: Tải ảnh QR Code từ một URL.
     * KHÔNG CẦN THƯ VIỆN ZXING NỮA.
     *
     * @param url  Chuỗi URL của API tạo QR Code.
     * @param size Kích thước (chỉ dùng để đặt kích thước Image, không ảnh hưởng đến
     *             tải ảnh).
     * @return Ảnh JavaFX Image.
     */
    private Image generateQrCodeImageFromUrl(String url, int size) {
        try {
            // Tải ảnh trực tiếp từ URL
            Image image = new Image(url, size, size, true, true);
            // Kiểm tra lỗi tải ảnh (ví dụ: URL sai, mất mạng)
            if (image.isError()) {
                return null;
            }
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 🔥 HÀM CUỐI CÙNG: Mở Popup Thanh toán Tiền mặt (FIX KÍCH THƯỚC NÚT BẰNG
     * MAX_WIDTH TRỰC TIẾP).
     */
    private void moPopupThanhToanTienMat() {
        if (currentHoaDon == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng chọn Hóa đơn trước khi thanh toán.");
            return;
        }
        // 1. Chuẩn bị dữ liệu tổng tiền
        calculateTotal();
        double tongTienThanhToan = 0;
        try {
            String amountStr = lblTongTienThanhToan.getText().replaceAll("[^0-9]", "");
            tongTienThanhToan = Double.parseDouble(amountStr.isEmpty() ? "0" : amountStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xác định tổng tiền thanh toán.");
            return;
        }
        final double finalTotal = tongTienThanhToan;
        final DecimalFormat currencyFormatter = new DecimalFormat("###,###");
        // 2. Thiết lập các Controls
        FlowPane flowButtonContainer = new FlowPane(10, 10);
        double[] presetValues = { 100000, 200000, 500000, 1000000, 1500000, 2000000, 2500000, 3000000, 5000000 };
        ToggleGroup presetGroup = new ToggleGroup();
        flowButtonContainer.setPrefWrapLength(420);
        // TextFields
        TextField txtTienKhachDua = new TextField("0");
        TextField txtTienTraLai = new TextField("0");
        Button btnXacNhan = new Button("Xác nhận");
        btnXacNhan.setMaxWidth(Double.MAX_VALUE);
        btnXacNhan.setStyle(
                "-fx-background-color: #ff9900; -fx-text-fill: white; -fx-font-size: 1.2em; -fx-font-weight: bold; -fx-padding: 12px 0; -fx-background-radius: 5px; -fx-border-radius: 5px;");
        txtTienTraLai.setEditable(false);
        txtTienKhachDua.setAlignment(Pos.CENTER_RIGHT);
        txtTienTraLai.setAlignment(Pos.CENTER_RIGHT);
        Runnable capNhatTienThoi = () -> {
            String cleanedAmount = txtTienKhachDua.getText().replaceAll("[^0-9]", "");
            double tienKhachDua = 0;
            try {
                tienKhachDua = Double.parseDouble(cleanedAmount.isEmpty() ? "0" : cleanedAmount);
            } catch (NumberFormatException ignored) {
            }
            double tienTraLai = tienKhachDua - finalTotal;
            txtTienTraLai.setText(currencyFormatter.format(Math.max(0, tienTraLai)));
            if (tienKhachDua < finalTotal) {
                btnXacNhan.setStyle(
                        "-fx-background-color: #e0e0e0; -fx-text-fill: #999999; -fx-font-size: 1.2em; -fx-font-weight: bold; -fx-padding: 12px 0; -fx-background-radius: 5px; -fx-border-radius: 5px;");
                txtTienKhachDua.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                btnXacNhan.setDisable(true);
            } else {
                btnXacNhan.setStyle(
                        "-fx-background-color: #ff9900; -fx-text-fill: white; -fx-font-size: 1.2em; -fx-font-weight: bold; -fx-padding: 12px 0; -fx-background-radius: 5px; -fx-border-radius: 5px;");
                txtTienKhachDua.setStyle(null);
                btnXacNhan.setDisable(false);
            }
        };
        // Xử lý nút preset
        for (double value : presetValues) {
            ToggleButton btn = new ToggleButton(currencyFormatter.format(value) + " VNĐ");
            btn.setUserData(value);
            btn.setToggleGroup(presetGroup);
            // Style cơ bản cho nút preset
            btn.setStyle(
                    "-fx-background-color: #ffffff; -fx-text-fill: #0d6efd; -fx-font-weight: 500; -fx-font-size: 0.95em; -fx-padding: 10px 15px; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-border-color: #0d6efd; -fx-border-width: 1px;");
            btn.setPrefWidth(125);
            btn.setPrefHeight(40);
            btn.setOnAction(e -> {
                txtTienKhachDua.setText(currencyFormatter.format(value));
                capNhatTienThoi.run();
            });
            flowButtonContainer.getChildren().add(btn);
        }
        // Xử lý nhập tay
        txtTienKhachDua.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            try {
                if (!filtered.isEmpty()) {
                    double value = Double.parseDouble(filtered);
                    txtTienKhachDua.setText(currencyFormatter.format(value));
                    Platform.runLater(txtTienKhachDua::end);
                } else {
                    txtTienKhachDua.setText("");
                }
            } catch (NumberFormatException ignored) {
            }
            capNhatTienThoi.run();
        });
        // 4. Thiết lập Layout và Stage
        Label lblTitle = new Label("Tiền mặt");
        lblTitle.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;");
        VBox inputContainer = new VBox(10);
        inputContainer.getChildren().addAll(
                createInputGridRow("Tiền khách đưa:", txtTienKhachDua, true),
                createInputGridRow("Tiền trả lại khách:", txtTienTraLai, false));
        GridPane root = new GridPane();
        root.setVgap(20);
        root.setPadding(new Insets(20));
        root.setPrefWidth(450);
        root.setPrefHeight(480);
        root.setStyle("-fx-background-color: white;");
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(100);
        root.getColumnConstraints().add(column);
        root.add(lblTitle, 0, 0);
        root.add(flowButtonContainer, 0, 1);
        root.add(inputContainer, 0, 2);
        root.add(btnXacNhan, 0, 3);
        GridPane.setHalignment(lblTitle, HPos.CENTER);
        GridPane.setHalignment(flowButtonContainer, HPos.CENTER);
        GridPane.setHalignment(btnXacNhan, HPos.CENTER);
        // 🔥 QUAN TRỌNG: FillWidth buộc nút giãn nở 100%
        GridPane.setFillWidth(btnXacNhan, true);
        GridPane.setFillWidth(inputContainer, true);
        // 5. Thiết lập Stage (Cửa sổ Popup)
        Stage popupStage = new Stage();
        popupStage.setTitle("Thanh toán tiền mặt");
        Scene scene = new Scene(root);
        popupStage.setScene(scene);
        // 6. Xử lý nút Xác nhận
        btnXacNhan.setOnAction(e -> {
            String maHDCanThanhToan = currentHoaDon.getMaHD();
            String maNV = ClientSessionManager.getInstance().getCurrentEmployee().getMaNV();
            String maBan = currentHoaDon.getMaBan();
            if (maHDCanThanhToan != null) {
                // GỌI API THANH TOÁN
                Response res = Client.sendWithParams(CommandType.CHECKOUT, Map.of(
                        "maHD", maHDCanThanhToan,
                        "pttt", "TIEN_MAT",
                        "maNhanVien", maNV,
                        "maBan", maBan != null ? maBan : "",
                        "totalAmount", finalTotal));
                if (res.getStatusCode() == 200) {
                    // Tải lại hóa đơn mới nhất qua API
                    Response resHD = Client.sendWithParams(CommandType.GET_INVOICE, Map.of("maHD", maHDCanThanhToan));
                    if (resHD.getStatusCode() == 200) {
                        currentHoaDon = utils.JsonUtil.convertValue(resHD.getData(), HoaDon.class);
                        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                "Hóa đơn " + maHDCanThanhToan + " đã được thanh toán.");
                        // Cập nhật giao diện
                        loadTableGrids();
                        disableMiddleActionButtons();
                        disablePaymentButtons();
                        loadBookingCards();
                        handleThanhToan();
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thanh toán: " + res.getMessage());
                }
            }
            popupStage.close();
        });
        popupStage.show();
    }

    /**
     * Hàm helper createInputGridRow (Đã sửa ở bước trước)
     * Vui lòng đảm bảo bạn có hàm này trong class DatBan.java
     */
    private GridPane createInputGridRow(String labelText, TextField textField, boolean showInputLabel) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setAlignment(Pos.CENTER_LEFT);
        // Cột 1: Label
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 1.05em; -fx-text-fill: #333333; -fx-font-weight: 500;");
        grid.add(label, 0, 1);
        // Cột 2: TextField
        textField.setEditable(showInputLabel);
        textField.setPrefWidth(150);
        textField.setStyle(
                "-fx-border-color: #ced4da; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-padding: 8px 10px;");
        grid.add(textField, 1, 1);
        // === XỬ LÝ LABEL "INPUT" ===
        if (showInputLabel) {
            Label inputLabel = new Label("Input");
            inputLabel.setStyle("-fx-font-size: 0.75em; -fx-text-fill: #999999;");
            GridPane.setHalignment(inputLabel, HPos.LEFT);
            grid.add(inputLabel, 1, 0);
        }
        // Đặt ràng buộc chiều rộng cho các cột
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(150);
        grid.getColumnConstraints().addAll(col1, col2);
        return grid;
    }

    /**
     * 🔥 HÀM MỚI: Xử lý khi người dùng chọn Khuyến mãi
     */
    private void handlePromoSelection(String selectedName) {
        if (selectedName == null || selectedName.equals("Không áp dụng")) {
            selectedUuDai = null;
        } else {
            // Trích xuất tên KM từ chuỗi hiển thị
            String simpleName = selectedName.substring(0, selectedName.indexOf(" (Giảm")).trim();
            // Tìm đối tượng UuDai tương ứng
            selectedUuDai = dsUuDaiDangApDung.stream()
                    .filter(ud -> ud.getTenUuDai().equals(simpleName))
                    .findFirst()
                    .orElse(null);
        }
        calculateTotal(); // Luôn tính lại tổng tiền khi khuyến mãi thay đổi
    }

    /**
     * 🔥 HÀM MỚI: Tải danh sách Khuyến mãi 'Đang áp dụng' vào ComboBox
     */
    private void loadPromoComboBox() {
        // Lấy tất cả ưu đãi và lọc những ưu đãi 'Đang áp dụng'
        Response res = Client.send(CommandType.GET_PROMOS, null);
        List<UuDai> allUuDai = (res.getStatusCode() == 200)
                ? utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), UuDai.class)
                : new ArrayList<>();
        // Lọc các ưu đãi đang áp dụng (hoặc Sắp diễn ra)
        dsUuDaiDangApDung = allUuDai.stream()
                .filter(ud -> ud.getTrangThai().equals("Đang áp dụng"))
                .collect(Collectors.toList());
        ObservableList<String> promoNames = FXCollections.observableArrayList();
        promoNames.add("Không áp dụng"); // Thêm lựa chọn mặc định
        for (UuDai ud : dsUuDaiDangApDung) {
            promoNames.add(String.format("%s (Giảm %.0f%%)", ud.getTenUuDai(), ud.getGiaTri()));
        }
        if (promoComboBox != null) {
            promoComboBox.setItems(promoNames);
            promoComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * 🔥 HÀM MỚI: Mở Popup Gộp Bàn (GopBan_Popup.fxml).
     * 
     * @requires currentHoaDon != null (Phải chọn HĐ Gốc)
     */
    private void openGopBanPopup() {
        // 0. Tự động lưu dữ liệu hiện tại trước khi thực hiện thao tác nghiệp vụ
        if (currentHoaDon != null) {
            handleSuaHoaDon();
        }
        // 1. Kiểm tra HĐ gốc đã được chọn chưa
        if (currentHoaDon == null || currentHoaDon.getMaHDGoc() != null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một Hóa đơn Gốc đang hoạt động để gộp.");
            return;
        }
        try {
            // 2. Tải FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GopBan_Popup.fxml"));
            VBox root = loader.load();
            GopBanPopupController popupController = loader.getController();
            // 3. Truyền dữ liệu: HĐ gốc (Master) và reference đến controller cha
            popupController.setInitialData(currentHoaDon, this);
            // 4. Cấu hình Stage
            Stage popupStage = new Stage();
            popupStage.setTitle("Gộp Bàn Cho HĐ: " + currentHoaDon.getMaHD());
            Scene scene = new Scene(root);
            popupStage.setScene(scene);
            popupStage.showAndWait();
            // 5. Sau khi Popup đóng, refresh giao diện chính
            loadBookingCards();
            loadTableGrids();
            clearFormDatBan();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi UI",
                    "Không thể tải giao diện Gộp Bàn Popup.\nKiểm tra file FXML và Controller đã đúng chưa.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi không xác định",
                    "Đã xảy ra lỗi khi mở Popup Gộp Bàn: " + e.getMessage());
        }
    }

    /**
     * 🔥 HÀM MỚI: Mở Popup Tách Bàn (TachBanPopup.fxml).
     * * Đảm bảo:
     * 1. Hóa đơn gốc (currentHoaDon) đã được chọn.
     * 2. Hóa đơn phải đang ở trạng thái 'Đang sử dụng' (DANG_SU_DUNG) hoặc
     * 'HoaDonTam'.
     */
    private void openTachBanPopup() {
        // 0. Tự động lưu dữ liệu hiện tại trước khi thực hiện thao tác nghiệp vụ
        if (currentHoaDon != null) {
            handleSuaHoaDon();
        }
        // 1. Kiểm tra Hóa đơn gốc đã được chọn chưa
        if (currentHoaDon == null || currentHoaDon.getMaHDGoc() != null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một Hóa đơn Gốc đang hoạt động để tách.");
            return;
        }
        // 2. Kiểm tra trạng thái Hóa đơn
        String trangThaiHd = currentHoaDon.getTrangThai().getDbValue();
        if (!(trangThaiHd.equals(TrangThaiHoaDon.DANG_SU_DUNG.getDbValue())
                || trangThaiHd.equals(TrangThaiHoaDon.HOA_DON_TAM.getDbValue()))) {
            showAlert(Alert.AlertType.WARNING, "Không thể tách",
                    "Chỉ có thể tách Hóa đơn đang phục vụ/tạm (trạng thái hiện tại: "
                            + currentHoaDon.getTrangThai().getDisplayName() + ").");
            return;
        }
        try {
            // Tải FXML và Controller
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TachBan_Popup.fxml"));
            VBox root = loader.load();
            TachBanPopupController popupController = loader.getController(); // << Tên Controller chính xác
            // Truyền dữ liệu: Sử dụng currentHoaDon (Hóa đơn gốc)
            popupController.setInitialData(currentHoaDon, this);
            // Cấu hình Stage (cửa sổ popup)
            Stage popupStage = new Stage();
            popupStage.setTitle("Tách Bàn Cho Hóa Đơn: " + currentHoaDon.getMaHD());
            Scene scene = new Scene(root);
            // (Optional) Thêm CSS cho Popup
            URL cssUrl = getClass().getResource("/css/TachBanPopup.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            popupStage.setScene(scene);
            popupStage.showAndWait();
            // 3. Sau khi Popup đóng, refresh giao diện chính
            loadBookingCards();
            loadTableGrids();
            clearFormDatBan(); // Dọn dẹp form sau thao tác
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Tải Giao Diện",
                    "Không thể tải giao diện Tách Bàn.\nKiểm tra file FXML và Controller đã đúng chưa.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi không xác định",
                    "Đã xảy ra lỗi khi mở Popup Tách Bàn: " + e.getMessage());
        }
    }

    // ui.DatBan.java
    @FXML
    private void handleDoiBan() {
        if (currentHoaDon == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một hóa đơn đang phục vụ để đổi bàn.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DoiBan_Popup.fxml"));
            Parent root = loader.load();
            DoiBanPopupController controller = loader.getController();
            controller.setInitialData(currentHoaDonGocVaPhu != null && !currentHoaDonGocVaPhu.isEmpty() ? currentHoaDonGocVaPhu : List.of(currentHoaDon), this);

            Stage popupStage = new Stage();
            popupStage.setTitle("Đổi Bàn - " + currentHoaDon.getMaHD());
            popupStage.setScene(new Scene(root));
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.showAndWait();

            handleLamMoi(); // Refresh giao diện sau khi đổi
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở popup đổi bàn: " + e.getMessage());
        }
    }
    /**
     * 🔥 HÀM MỚI: Làm mới toàn bộ dữ liệu từ máy chủ (Thủ công)
     */
    private void handleLamMoi() {
        System.out.println("[UI-DATBAN] Đang làm mới dữ liệu thủ công...");
        loadBookingCards();
        loadTableGrids();
        if (currentHoaDon != null) {
            Response res = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", currentHoaDon.getMaHD()));
            if (res.getStatusCode() == 200) {
                HoaDon updatedHd = utils.JsonUtil.convertValue(res.getData(), HoaDon.class);
                loadHoaDonToMainInterface(updatedHd, false);
            }
        }
        // showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã làm mới dữ liệu từ máy chủ.");
    }

    /**
     * 🔥 HÀM MỚI: Lưu danh sách món ăn (Bắn socket ngay lập tức)
     */
    private void handleLuuMon() {
        if (currentHoaDon == null || currentHoaDon.getMaHD() == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một hóa đơn đang phục vụ để lưu món.");
            return;
        }
        
        System.out.println("[UI-DATBAN] Đang lưu danh sách món cho: " + currentHoaDon.getMaHD());
        
        // Gửi yêu cầu cập nhật danh sách món (itemsJson)
        Response res = Client.sendWithParams(CommandType.UPDATE_INVOICE, Map.of(
                "maHD", currentHoaDon.getMaHD(),
                "itemsJson", utils.JsonUtil.toJson(convertToChiTiet(monOrderList))
        ));

        if (res.getStatusCode() == 200) {
            // Server sẽ tự động Service.broadcast(UPDATE_INVOICE) bên trong handleUpdateInvoice
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật món ăn. Các máy khác sẽ thấy sự thay đổi ngay lập tức.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu món: " + res.getMessage());
        }
    }

    private void handleLuuDatHang() {
        if (currentHoaDon == null || currentHoaDon.getMaHD() == null) {
            // Trường hợp 1: Tạo mới (Cần chọn bàn và xác nhận trước)
            if (selectedBanList.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn bàn trước khi lưu đơn.");
                return;
            }
            if (!isBookingConfirmed) {
                showAlert(Alert.AlertType.WARNING, "Thiếu bước Xác nhận", "Vui lòng nhấn nút 'Xác nhận bàn' trước khi lưu đặt hàng.");
                return;
            }
            // Gọi logic tạo đơn hiện tại (Thực hiện qua handleXacNhanBan logic hoặc tiếp tục bên dưới)
        }

        try {
            // 1. Đồng bộ thông tin khách hàng
            String sdt = txtSoDienThoai.getText().trim();
            String tenKH = txtTenKhachHang.getText().trim();
            if (sdt.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập Số điện thoại.");
                return;
            }

            KhachHang khRequest = new KhachHang();
            khRequest.setSoDT(sdt);
            khRequest.setTenKH(tenKH);
            Client.sendWithParams(CommandType.TIM_HOAC_TAO_KH, Map.of("khachHang", khRequest));

            // 2. Đồng bộ tiền cọc
            double tienCoc = 0;
            try {
                String tienCocRaw = txtTienCoc.getText().replaceAll("[^0-9.]", "");
                tienCoc = Double.parseDouble(tienCocRaw.isEmpty() ? "0" : tienCocRaw);
            } catch (NumberFormatException e) {}

            // 3. Nếu đã có HĐ, thực hiện UPDATE_INVOICE (Broadcast Real-time)
            if (currentHoaDon != null && currentHoaDon.getMaHD() != null) {
                Response res = Client.sendWithParams(CommandType.UPDATE_INVOICE, Map.of(
                        "maHD", currentHoaDon.getMaHD(),
                        "soDT", sdt,
                        "tenKH", tenKH,
                        "tienCoc", tienCoc,
                        "itemsJson", utils.JsonUtil.toJson(convertToChiTiet(monOrderList))
                ));

                if (res.getStatusCode() == 200) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật danh sách món cho HĐ: " + currentHoaDon.getMaHD());
                    loadBookingCards();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật: " + res.getMessage());
                }
            } else {
                // Logic tạo mới hoàn toàn (như cũ)
                handleCreateNewInvoice(sdt, tenKH, tienCoc);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi khi lưu đơn: " + e.getMessage());
        }
    }

    private void handleCreateNewInvoice(String sdt, String tenKH, double tienCoc) {
        try {
            // 1. Sinh mã hóa đơn mới từ Server
            Response resId = Client.send(CommandType.GENERATE_INVOICE_ID, null);
            if (resId.getStatusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể sinh mã hóa đơn: " + resId.getMessage());
                return;
            }
            String nextMaHD = (String) resId.getData();

            // 2. Chuẩn bị đối tượng Hóa đơn
            HoaDon hd = new HoaDon();
            hd.setMaHD(nextMaHD);
            hd.setNgayLap(java.time.LocalDateTime.now());
            // 🔥 MỚI: Mặc định là Chờ xác nhận (thay vì Đã nhận bàn ngay lập tức)
            hd.setTrangThai(TrangThaiHoaDon.CHO_XAC_NHAN.getDbValue()); 
            hd.setTienCoc(tienCoc);

            // Gán khách hàng
            KhachHang kh = new KhachHang();
            kh.setSoDT(sdt);
            kh.setTenKH(tenKH);
            hd.setKhachHang(kh);

            // Gán nhân viên hiện tại
            if (utils.ClientSessionManager.getInstance().getCurrentEmployee() != null) {
                hd.setNhanVien(utils.ClientSessionManager.getInstance().getCurrentEmployee());
            }

            // 3. Nếu chọn nhiều bàn, tạo nhiều HĐ (1 Gốc - n Phụ) hoặc xử lý theo nghiệp vụ
            // Ở đây giả định bàn đầu tiên là bàn chính
            if (!selectedBanList.isEmpty()) {
                hd.setBan(selectedBanList.get(0));
            }

            // 4. Chuẩn bị danh sách chi tiết
            List<entity.ChiTietHoaDon> chiTiet = convertToChiTiet(monOrderList);

            // 5. Gửi yêu cầu tạo HĐ lên server
            Response res = Client.sendWithParams(CommandType.CREATE_ORDER, Map.of(
                    "hoaDon", hd,
                    "chiTiet", chiTiet));

            if (res.getStatusCode() == 200) {
                // 6. Nếu có nhiều bàn, tạo các HĐ Phụ (Sub-Invoices)
                if (selectedBanList.size() > 1) {
                    for (int i = 1; i < selectedBanList.size(); i++) {
                        HoaDon subHd = new HoaDon();
                        subHd.setMaHD(nextMaHD + "_S" + i);
                        subHd.setMaHDGoc(nextMaHD);
                        subHd.setBan(selectedBanList.get(i));
                        subHd.setTrangThai(TrangThaiHoaDon.CHO_XAC_NHAN.getDbValue());
                        subHd.setNgayLap(hd.getNgayLap());
                        subHd.setKhachHang(kh);
                        subHd.setNhanVien(hd.getNhanVien());

                        Client.sendWithParams(CommandType.CREATE_ORDER, Map.of(
                                "hoaDon", subHd,
                                "chiTiet", new ArrayList<>())); // Bàn phụ không có món riêng ban đầu
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo hóa đơn mới: " + nextMaHD);
                
                // 7. Refresh và chọn hóa đơn vừa tạo
                loadBookingCards();
                loadTableGrids();
                
                // Tải lại HĐ vừa tạo để hiển thị lên UI
                Response resNew = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", nextMaHD));
                if (resNew.getStatusCode() == 200) {
                    this.currentHoaDon = utils.JsonUtil.convertValue(resNew.getData(), HoaDon.class);
                    loadHoaDonToMainInterface(this.currentHoaDon, false);
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi tạo hóa đơn: " + res.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi không xác định: " + e.getMessage());
        }
    }

    private void openDoiBanPopup() {
        // 0. Tự động lưu dữ liệu hiện tại trước khi thực hiện thao tác nghiệp vụ
        if (currentHoaDon != null) {
            handleSuaHoaDon();
        }
        // 1. Kiểm tra xem có hóa đơn nào đang được chọn không
        if (currentHoaDon == null || currentHoaDonGocVaPhu.isEmpty()) { //
            showAlert(Alert.AlertType.WARNING, "Chưa chọn Hóa đơn",
                    "Vui lòng chọn một Hóa đơn Gốc từ danh sách bên trái để thực hiện đổi bàn.");
            return;
        }
        try {
            // 2. Tải FXML của Popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DoiBan_Popup.fxml")); // <<< Tên file FXML
                                                                                                   // mới
            VBox root = loader.load();
            // 3. Lấy Controller của Popup và truyền dữ liệu
            DoiBanPopupController popupController = loader.getController(); // <<< Controller mới
            popupController.setInitialData(currentHoaDonGocVaPhu, this); // Truyền list HĐ và controller chính
            // 4. Tạo và hiển thị cửa sổ Popup
            Stage popupStage = new Stage();
            popupStage.setTitle("Đổi Bàn cho HĐ: " + currentHoaDon.getMaHD()); //
            Scene scene = new Scene(root);
            // (Optional) Thêm CSS cho Popup nếu muốn
            URL cssUrl = getClass().getResource("/css/DoiBanPopup.css"); // <<< File CSS mới (tùy chọn)
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            popupStage.setScene(scene);
            popupStage.showAndWait(); // Hiển thị và chờ Popup đóng lại
            // 5. Sau khi Popup đóng, refresh lại màn hình chính (nếu cần)
            // Controller Popup sẽ gọi hàm refresh của DatBan nếu đổi bàn thành công.
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi UI", "Không thể mở giao diện Đổi Bàn Popup: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi không xác định",
                    "Đã xảy ra lỗi khi mở Popup Đổi Bàn: " + e.getMessage());
        }
    }

    /**
     * 🔥 HÀM XỬ LÝ NGHIỆP VỤ TÁCH BÀN
     * Tạo một hóa đơn mới (với trạng thái HOADONTAM) dựa trên hóa đơn cũ,
     * và gán cho một bàn TẠM THỜI (NULL).
     * 
     * @param maHDCu Mã hóa đơn gốc cần tách.
     * @return Mã hóa đơn mới được tạo.
     * @throws Exception nếu có lỗi CSDL hoặc lỗi nghiệp vụ.
     */
    public String tachBan(String maHDCu) throws Exception {
        Response res = Client.sendWithParams(CommandType.SPLIT_INVOICE, Map.of("sourceId", maHDCu));
        if (res.getStatusCode() == 200) {
            return (String) res.getData();
        } else {
            throw new Exception(res.getMessage());
        }
    }

    private List<entity.ChiTietHoaDon> convertToChiTiet(List<MonOrder> orders) {
        if (orders == null)
            return new ArrayList<>();
        
        // 🔥 Gộp món trước khi gửi lên Server
        java.util.Map<String, entity.ChiTietHoaDon> aggregated = new java.util.LinkedHashMap<>();
        for (MonOrder o : orders) {
            String ma = o.getMaMon();
            if (aggregated.containsKey(ma)) {
                entity.ChiTietHoaDon existing = aggregated.get(ma);
                existing.setSoLuong(existing.getSoLuong() + o.getSoLuong());
                existing.setThanhTien(existing.getSoLuong() * existing.getDonGia());
            } else {
                entity.ChiTietHoaDon ct = new entity.ChiTietHoaDon();
                ct.setMaMon(ma);
                ct.setTenMon(o.getTenMon());
                ct.setSoLuong(o.getSoLuong());
                ct.setDonGia(o.getDonGia());
                ct.setThanhTien(o.getDonGia() * o.getSoLuong());
                aggregated.put(ma, ct);
            }
        }
        return new ArrayList<>(aggregated.values());
    }

    /**
     * HÀM XỬ LÝ NÚT XÁC NHẬN (Cập nhật UI và tính tiền cọc)
     */
    private void handleXacNhanBan() {
        if (selectedBanList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn ít nhất một bàn để xác nhận.");
            return;
        }
        if (currentHoaDon == null) {
            for (Ban selectedBan : selectedBanList) {
                TrangThaiBan trangThaiHienThi = getTrangThaiHienThi(selectedBan, LocalTime.now());
                if (trangThaiHienThi == TrangThaiBan.DA_DAT || trangThaiHienThi == TrangThaiBan.DANG_SU_DUNG) {
                    showAlert(Alert.AlertType.WARNING, "Bàn đang bận", "Bàn " + selectedBan.getMaBan()
                            + " đang bận hoặc đã được đặt trước (trong vòng 4 tiếng trước giờ vào). Vui lòng chọn bàn khác.");
                    return;
                }
            }
        }
        if (currentHoaDon != null) {
            // 🔥 TRƯỜNG HỢP: NHẬN BÀN Đã ĐẶT TRƯỚC (Bàn màu đỏ -> màu cam)
            String maHD = currentHoaDon.getMaHD();
            if (maHD != null) {
                // Cập nhật trạng thái hóa đơn sang DANG_SU_DUNG và set giờ vào thực tế
                Response res = Client.sendWithParams(CommandType.UPDATE_INVOICE, Map.of(
                        "maHD", maHD,
                        "trangThai", "DangSuDung",
                        "gioVao", java.time.LocalDateTime.now().toString()));
                if (res.getStatusCode() == 200) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Đã nhận bàn thành công. Chúc quý khách ngon miệng!");
                    isBookingConfirmed = true;
                    loadTableGrids();
                    loadBookingCards();
                    // Tải lại HĐ mới cập nhật
                    Response resHD = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", maHD));
                    if (resHD.getStatusCode() == 200) {
                        this.currentHoaDon = utils.JsonUtil.convertValue(resHD.getData(), HoaDon.class);
                        loadHoaDonToMainInterface(this.currentHoaDon, false);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể nhận bàn: " + res.getMessage());
                }
                return;
            }
        }
        // TRƯỜNG HỢP: NHẬN BÀN MỚI (CHƯA CÓ HÓA ĐƠN)
        for (int i = 0; i < selectedBanList.size(); i++) {
            Button button = selectedButtonList.get(i);
            button.getStyleClass().remove("table-button-selected");
            // 🔥 ĐàSỬA: Nhận bàn ngay -> Chuyển sang DANG_SU_DUNG (Cam)
            applyTableStyle(button, TrangThaiBan.DANG_SU_DUNG);
        }
        double tienCoc = calculateTienCoc();
        txtTienCoc.setText(String.format("%.0f", tienCoc));
        isBookingConfirmed = true;
        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                "Đã xác nhận bàn. Vui lòng nhập thông tin khách hàng và món ăn, sau đó nhấn 'Lưu đặt hàng'.");
        updateSelectionLabels();
    }

    // Hàm tính tiền cọc dựa trên chính sách
    private double calculateTienCoc() {
        if (selectedBanList.isEmpty()) {
            return 0.0;
        }
        double tongTienCoc = 0;
        final double COC_MAC_DINH = 150000.0;
        final double PHI_PHONG_RIENG = 100000.0;
        for (Ban ban : selectedBanList) {
            tongTienCoc += COC_MAC_DINH;
            if (ban.getLoaiBan() == LoaiBan.PHONG) {
                tongTienCoc += PHI_PHONG_RIENG;
            }
        }
        return tongTienCoc;
    }

    // =========================================================
    // LOGIC TÍNH TOÁN TRẠNG THÁI HIỂN THỊ MỚI
    // =========================================================
    /**
     * Tính toán trạng thái hiển thị của bàn dựa trên MỘT thời điểm kiểm tra.
     * 🔥 ĐàSỬA: Coi trạng thái CHO_XAC_NHAN tương đương như DAT (Đã đặt - màu đỏ).
     */
    public TrangThaiBan getTrangThaiHienThi(Ban banGoc, LocalTime thoiGianKiemTra) {
        // 1. Tìm HĐ đang hoạt động sớm nhất cho bàn này
        Optional<HoaDon> hdDangCho = dsHoaDonDatTrongNgay.stream()
                .filter(hd -> hd.getBan() != null && hd.getBan().getMaBan().equals(banGoc.getMaBan())
                        && hd.getGioVao() != null)
                // Lọc các trạng thái giữ bàn
                .filter(hd -> hd.getTrangThai() != null &&
                        (hd.getTrangThai() == TrangThaiHoaDon.DAT ||
                                hd.getTrangThai() == TrangThaiHoaDon.DANG_SU_DUNG ||
                                hd.getTrangThai() == TrangThaiHoaDon.HOA_DON_TAM ||
                                hd.getTrangThai() == TrangThaiHoaDon.CHO_XAC_NHAN)) // <<< THÊM MỚI
                .min(Comparator.comparing(HoaDon::getGioVao));
        if (hdDangCho.isPresent()) {
            TrangThaiHoaDon trangThaiHD = hdDangCho.get().getTrangThai();
            // 2. Ưu tiên CAM (DANG_SU_DUNG / HOA_DON_TAM)
            if (trangThaiHD == TrangThaiHoaDon.DANG_SU_DUNG || trangThaiHD == TrangThaiHoaDon.HOA_DON_TAM) {
                return TrangThaiBan.DANG_SU_DUNG; // Cam
            }
            // 3. Xử lý ĐỎ (ĐàĐẶT - DAT hoặc Chờ xác nhận - CHO_XAC_NHAN)
            if (trangThaiHD == TrangThaiHoaDon.DAT || trangThaiHD == TrangThaiHoaDon.CHO_XAC_NHAN) { // <<< THÊM MỚI
                LocalTime gioVao = hdDangCho.get().getGioVao().toLocalTime();
                LocalTime gioCanhBaoSom = gioVao.minusHours(4); // Mốc 4 tiếng
                // Case 1: Thời gian tìm kiếm ĐàĐẾN hoặc QUA giờ vào -> Cam
                if (thoiGianKiemTra.isAfter(gioVao) || thoiGianKiemTra.equals(gioVao)) {
                    return TrangThaiBan.DANG_SU_DUNG;
                }
                // Case 2: Thời gian tìm kiếm TRƯỚC giờ vào (Kiểm tra 4 tiếng cứng) -> Đỏ
                if (thoiGianKiemTra.isAfter(gioCanhBaoSom) || thoiGianKiemTra.equals(gioCanhBaoSom)) {
                    return TrangThaiBan.DA_DAT; // Đỏ
                }
                // Nếu trước 4 tiếng -> Trống (sẽ rơi vào fallback)
            }
        }
        // 4. Fallback: Trống (Trắng)
        return TrangThaiBan.TRONG;
    }

    // =========================================================
    // LOGIC TẢI BẢNG VÀ CHỌN BÀN
    // =========================================================
    // Cần phải là public để Controller khác có thể gọi
    public void loadTableGrids() {
        // Chuyển việc tải dữ liệu sang luồng chạy ngầm để tránh treo UI
        new Thread(() -> {
            try {
                // 1. Tải ds HĐ Đang Chờ qua API
                LocalDate dateToLoadForGrid = datePickerThoiGianDen.getValue() != null ? datePickerThoiGianDen.getValue()
                        : LocalDate.now();
                Response resInvoices = Client.sendWithParams(CommandType.GET_PENDING_INVOICES,
                        Map.of("date", dateToLoadForGrid.toString()));
                
                final List<HoaDon> fetchedInvoices = new ArrayList<>();
                if (resInvoices.getStatusCode() == 200 && resInvoices.getData() != null) {
                    List<HoaDon> list = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resInvoices.getData()), HoaDon.class);
                    if (list != null) fetchedInvoices.addAll(list);
                }

                // 2. Tải danh sách bàn qua API
                Response resTables = Client.send(CommandType.GET_TABLES, null);
                final List<HoaDon> finalInvoices = fetchedInvoices;
                
                Platform.runLater(() -> {
                    this.dsHoaDonDatTrongNgay = finalInvoices;
                    
                    List<Ban> tatCaBan = new ArrayList<>();
                    if (resTables.getStatusCode() == 200 && resTables.getData() != null) {
                        List<Ban> list = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resTables.getData()), Ban.class);
                        if (list != null) tatCaBan.addAll(list);
                    }

                    List<Ban> banHienThi = new ArrayList<>();
                    LocalTime thoiGianHienTai = LocalTime.now();
                    for (Ban ban : tatCaBan) {
                        Ban banMoi = new Ban(ban.getMaBan(), ban.getViTri(), ban.getSucChua(), ban.getLoaiBan(),
                                ban.getTrangThai());
                        TrangThaiBan trangThaiHienThi = getTrangThaiHienThi(banMoi, thoiGianHienTai);
                        banMoi.setTrangThai(trangThaiHienThi);
                        banHienThi.add(banMoi);
                    }
                    loadTableGridsBase(banHienThi);
                });
            } catch (Exception e) {
                System.err.println("[DatBan] Lỗi loadTableGrids: " + e.getMessage());
            }
        }).start();
    }

    private void loadTableGridsBase(List<Ban> dsBan) {
        try {
            // === THÊM MỚI: Clear Map trước khi tạo lại nút ===
            tableButtonMap.clear();
            // ===========================================
            List<Ban> tangTret = dsBan.stream().filter(b -> b.getLoaiBan() == LoaiBan.TANG_TRET)
                    .collect(Collectors.toList()); //
            // ... (code lấy tangMot, phongRieng như cũ) ...
            List<Ban> tangMot = dsBan.stream().filter(b -> b.getLoaiBan() == LoaiBan.TANG_1)
                    .collect(Collectors.toList());
            List<Ban> phongRieng = dsBan.stream().filter(b -> b.getLoaiBan() == LoaiBan.PHONG)
                    .collect(Collectors.toList());
            populateTableGrid(gridTangTret, tangTret); //
            populateTableGrid(gridTang1, tangMot); //
            populateTableGrid(gridPhongRieng, phongRieng); //
        } catch (Exception e) { //
            // ... (xử lý lỗi như cũ) ...
            showAlert(Alert.AlertType.ERROR, "Lỗi tải bàn",
                    "Không thể tải danh sách bàn. Kiểm tra kết nối CSDL và DatBanDAO.");
            tableButtonMap.clear(); // Clear map nếu có lỗi
            populateTableGrid(gridTangTret, createMockTables(20, LoaiBan.TANG_TRET));
            populateTableGrid(gridTang1, createMockTables(10, LoaiBan.TANG_1));
            populateTableGrid(gridPhongRieng, createMockTables(5, LoaiBan.PHONG));
        }
    }

    private List<Ban> createMockTables(int count, LoaiBan loaiBan) {
        List<Ban> mockList = new ArrayList<>();
        TrangThaiBan[] statuses = TrangThaiBan.values();
        for (int i = 1; i <= count; i++) {
            String maBan = loaiBan.toString() + String.format("%02d", i);
            TrangThaiBan status = statuses[i % statuses.length];
            mockList.add(new Ban(maBan, "Vị trí " + i, 4, loaiBan, status));
        }
        return mockList;
    }

    private void populateTableGrid(GridPane grid, List<Ban> dsBan) {
        // Không clear map ở đây, clear ở loadTableGridsBase
        grid.getChildren().clear();
        int col = 0;
        int row = 0;
        int maxCols = 5;
        for (Ban ban : dsBan) {
            Button btn = createTableButton(ban); //
            btn.setOnAction(e -> handleChonBan(ban, btn)); //
            // === THÊM MỚI: Lưu nút vào Map ===
            tableButtonMap.put(ban.getMaBan(), btn);
            // ================================
            grid.add(btn, col, row); //
            GridPane.setMargin(btn, new Insets(8));
            col++;
            if (col >= maxCols) { //
                col = 0;
                row++;
            }
        }
    }

    private Button createTableButton(Ban ban) {
        String soBan = ban.getMaBan().replaceAll("[^0-9]", "");
        Button btn = new Button(soBan);
        btn.getStyleClass().add("table-button");
        btn.setPrefSize(70, 70);
        btn.setAlignment(Pos.CENTER);
        TrangThaiBan trangThaiHienThi = ban.getTrangThai();
        applyTableStyle(btn, trangThaiHienThi);
        return btn;
    }

    private void applyTableStyle(Button button, TrangThaiBan trangThai) {
        button.getStyleClass().removeAll("table-button-booked", "table-button-serving", "table-button-available",
                "table-button-selected");
        if (trangThai == TrangThaiBan.DA_DAT) {
            button.getStyleClass().add("table-button-booked");
        } else if (trangThai == TrangThaiBan.DANG_SU_DUNG) {
            button.getStyleClass().add("table-button-serving");
        } else {
            button.getStyleClass().add("table-button-available");
        }
    }

    /**
     * Xử lý khi người dùng bấm chọn/bỏ chọn một bàn trên sơ đồ.
     * === ĐàSỬA: Logic Cảnh báo 8 tiếng khi chọn bàn ===
     */
    private void handleChonBan(Ban ban, Button currentButton) {
        // 1. Lấy thời gian kiểm tra từ UI
        LocalTime thoiGianKiemTra;
        LocalDate ngayKiemTra;
        try {
            ngayKiemTra = datePickerThoiGianDen.getValue();
            String gioStr = comboThoiGian.getValue();
            if (ngayKiemTra == null || gioStr == null || gioStr.trim().isEmpty()) {
                thoiGianKiemTra = LocalTime.now();
            } else {
                thoiGianKiemTra = LocalTime.parse(gioStr, timeFormatter);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng giờ",
                    "Giờ nhập không hợp lệ (cần HH:mm). Vui lòng sửa lại trước khi chọn bàn.");
            return;
        }
        // 2. 🔥 MỚI: Nếu bàn đang BẬN (Serving/Booked), tự động load Hóa đơn đó lên
        TrangThaiBan trangThaiHienThi = getTrangThaiHienThi(ban, thoiGianKiemTra);
        if (trangThaiHienThi == TrangThaiBan.DA_DAT || trangThaiHienThi == TrangThaiBan.DANG_SU_DUNG) {
            Optional<HoaDon> hdActive = dsHoaDonDatTrongNgay.stream()
                    .filter(hd -> hd.getBan() != null && hd.getBan().getMaBan().equals(ban.getMaBan()))
                    .filter(hd -> hd.getTrangThai() != null && (hd.getTrangThai() == TrangThaiHoaDon.DANG_SU_DUNG ||
                            hd.getTrangThai() == TrangThaiHoaDon.DAT ||
                            hd.getTrangThai() == TrangThaiHoaDon.HOA_DON_TAM ||
                            hd.getTrangThai() == TrangThaiHoaDon.CHO_XAC_NHAN))
                    .findFirst();

            if (hdActive.isPresent()) {
                loadHoaDonToMainInterface(hdActive.get(), false);
                return;
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo",
                        "Bàn bận nhưng không tìm thấy dữ liệu hóa đơn liên quan.");
                return;
            }
        }
        // 3. Xử lý Cảnh báo MỀM (4 - 8 tiếng)
        Optional<HoaDon> datGanNhat = dsHoaDonDatTrongNgay.stream()
                .filter(hd -> hd.getBan() != null && hd.getBan().getMaBan().equals(ban.getMaBan())
                        && hd.getGioVao() != null)
                .filter(hd -> hd.getTrangThai() != null && hd.getTrangThai().getDbValue().equals("Dat"))
                .min(Comparator.comparing(hd -> hd.getGioVao()));
        if (datGanNhat.isPresent()) {
            LocalTime gioVao = datGanNhat.get().getGioVao().toLocalTime();
            LocalTime gioCanhBaoMem = gioVao.minusHours(8); // Mốc 8 tiếng
            LocalTime gioCanhBaoCung = gioVao.minusHours(4); // Mốc 4 tiếng (đã kiểm tra ở bước 2)
            // Kiểm tra: nằm trong khoảng [8 tiếng trước, 4 tiếng trước)
            if (thoiGianKiemTra.isAfter(gioCanhBaoMem) && thoiGianKiemTra.isBefore(gioCanhBaoCung)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Cảnh báo bàn sắp có khách");
                confirm.setHeaderText("Bàn " + ban.getMaBan() + " đã được đặt lúc " + gioVao.format(timeFormatter));
                confirm.setContentText(
                        "Bàn này có đơn đặt trước trong vòng 8 tiếng tới (vùng cảnh báo mềm). Bạn có chắc chắn muốn chọn không?");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && (result.get() == ButtonType.CANCEL || result.get() == ButtonType.CLOSE)) {
                    return; // Ngăn chọn nếu người dùng hủy
                }
            }
        }
        // 4. Xử lý Chọn/Bỏ chọn (Giữ nguyên)
        boolean alreadySelected = selectedBanList.contains(ban);
        if (alreadySelected) {
            isBookingConfirmed = false;
            txtTienCoc.setText("0");
            selectedBanList.remove(ban);
            selectedButtonList.remove(currentButton);
            currentButton.getStyleClass().remove("table-button-selected");
            // Trả lại màu TRẮNG/TRỐNG sau khi bỏ chọn (vì nó đã vượt qua bước 2)
            applyTableStyle(currentButton, TrangThaiBan.TRONG);
        } else {
            isBookingConfirmed = false;
            txtTienCoc.setText("0");
            selectedBanList.add(ban);
            selectedButtonList.add(currentButton);
            currentButton.getStyleClass().removeAll("table-button-booked", "table-button-serving",
                    "table-button-available");
            currentButton.getStyleClass().add("table-button-selected"); // Màu xanh lá cây (chọn)
        }
        updateSelectionLabels();
    }

    private void updateSelectionLabels() {
        if (selectedBanList.isEmpty()) {
            if (lblBanDangChon != null)
                lblBanDangChon.setText("Chưa chọn");
            if (lblTrangThaiBan != null)
                lblTrangThaiBan.setText("---");
            return;
        }
        String banNames = selectedBanList.stream()
                .map(Ban::getMaBan)
                .collect(Collectors.joining(", "));
        String statusText = selectedBanList.size() + " bàn đã chọn";
        if (lblBanDangChon != null)
            lblBanDangChon.setText(banNames);
        if (lblTrangThaiBan != null)
            lblTrangThaiBan.setText(statusText);
    }

    // Cần phải là public để Controller khác có thể gọi
    // Cần phải là public để Controller khác có thể gọi
    // Cần phải là public để Controller khác có thể gọi
    public void loadBookingCards() {
        new Thread(() -> {
            try {
                Response res = Client.send(CommandType.GET_PENDING_INVOICES, null);
                final List<HoaDon> allPendingBookings = new ArrayList<>();
                if (res.getStatusCode() == 200 && res.getData() != null) {
                    List<HoaDon> list = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), HoaDon.class);
                    if (list != null) allPendingBookings.addAll(list);
                }

                LocalDate dateToLoad = datePickerThoiGianDen.getValue() != null ? datePickerThoiGianDen.getValue() : LocalDate.now();
                Response resToday = Client.sendWithParams(CommandType.GET_INVOICES_TODAY, Map.of("date", dateToLoad.toString()));
                final List<HoaDon> todayInvoices = new ArrayList<>();
                if (resToday.getStatusCode() == 200 && resToday.getData() != null) {
                    List<HoaDon> list = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resToday.getData()), HoaDon.class);
                    if (list != null) todayInvoices.addAll(list);
                }

                Platform.runLater(() -> {
                    this.dsHoaDonDatTrongNgay = todayInvoices;
                    vboxBookingCards.getChildren().clear();
                    String selectedStatus = comboFilter.getValue();
                    String searchQuery = txtSearch.getText().trim().toLowerCase();

                    List<HoaDon> filteredList = allPendingBookings.stream()
                            .filter(hd -> {
                                if (selectedStatus == null || selectedStatus.equals("Tất cả")) return true;
                                TrangThaiHoaDon selectedEnum = TrangThaiHoaDon.fromDisplayName(selectedStatus);
                                if (selectedEnum == null) return true;
                                if (selectedEnum == TrangThaiHoaDon.DANG_SU_DUNG) {
                                    return hd.getTrangThai() == TrangThaiHoaDon.DANG_SU_DUNG || hd.getTrangThai() == TrangThaiHoaDon.HOA_DON_TAM;
                                }
                                return hd.getTrangThai() != null && hd.getTrangThai() == selectedEnum;
                            })
                            .filter(hd -> {
                                if (searchQuery.isEmpty()) return true;
                                String sdt = (hd.getKhachHang() != null) ? hd.getKhachHang().getSoDT() : "";
                                return hd.getMaHD().toLowerCase().contains(searchQuery) || sdt.toLowerCase().contains(searchQuery);
                            })
                            .collect(Collectors.toList());

                    List<HoaDon> hdGocFilter = filteredList.stream()
                            .filter(hd -> hd.getMaHDGoc() == null || hd.getTrangThai() == TrangThaiHoaDon.HOA_DON_TAM)
                            .collect(Collectors.toList());

                    if (hdGocFilter.isEmpty()) {
                        Label lbl = new Label("Không tìm thấy đơn nào khớp.");
                        lbl.setPadding(new Insets(10));
                        vboxBookingCards.getChildren().add(lbl);
                    } else {
                        for (HoaDon hd : hdGocFilter) {
                            VBox card = createBookingCard(hd, allPendingBookings);
                            card.setOnMouseClicked(e -> loadHoaDonToMainInterface(hd, false));
                            vboxBookingCards.getChildren().add(card);
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("[DatBan] Lỗi loadBookingCards ngầm: " + e.getMessage());
            }
        }).start();
    }

    // (Bên dưới hàm handleSelectBookingCard)
    /**
     * 🔥 HÀM MỚI: Tìm và chọn một hóa đơn theo ID (Dùng để điều hướng sau khi tách)
     */
    public void selectInvoiceById(String maHD) {
        if (maHD == null || maHD.isEmpty())
            return;
        try {
            Response res = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", maHD));
            if (res.getStatusCode() == 200) {
                HoaDon hd = utils.JsonUtil.convertValue(res.getData(), HoaDon.class);
                if (hd != null) {
                    Platform.runLater(() -> loadHoaDonToMainInterface(hd, false));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void loadHoaDonToMainInterface(HoaDon hd, boolean isRealTimeUpdate) {
        // 0. Tự động lưu hóa đơn cũ trước khi chuyển sang hóa đơn mới (CHỈ LÀM NẾU KHÔNG PHẢI REALTIME UPDATE)
        if (!isRealTimeUpdate && this.currentHoaDon != null && this.currentHoaDon.getMaHD() != null) {
            handleSuaHoaDon();
        }
        System.out.println("LOG: Đang tải Hóa đơn " + (hd.getMaHD() != null ? hd.getMaHD() : "Mới"));
        // === BƯỚC 1: RESET MÀU CŨ ===
        if (!currentHoaDonGocVaPhu.isEmpty()) {
            for (HoaDon hdCu : currentHoaDonGocVaPhu) {
                if (hdCu.getBan() != null) {
                    Button btn = tableButtonMap.get(hdCu.getBan().getMaBan());
                    if (btn != null) {
                        TrangThaiBan trangThaiThuc = getTrangThaiHienThi(hdCu.getBan(), LocalTime.now());
                        applyTableStyle(btn, trangThaiThuc);
                    }
                }
            }
        }
        // 1. Set hóa đơn hiện tại
        this.currentHoaDon = hd;
        // 2. KIỂM TRA GỐC/PHỤ (Chỉ ép về gốc nếu không phải là Hóa đơn tách)
        boolean isSplitInvoice = "HoaDonTam".equalsIgnoreCase(hd.getTrangThai() != null ? hd.getTrangThai().getDbValue() : "");
        
        if (hd.getMaHDGoc() != null && !isSplitInvoice) {
            Response resGoc = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", hd.getMaHDGoc()));
            if (resGoc.getStatusCode() == 200) {
                this.currentHoaDon = utils.JsonUtil.convertValue(resGoc.getData(), HoaDon.class);
            }
        }
        // 3. TÌM TẤT CẢ HĐ PHỤ
        currentHoaDonGocVaPhu.clear();
        currentHoaDonGocVaPhu.add(this.currentHoaDon);
        Response resSub = Client.sendWithParams(CommandType.GET_SUB_INVOICES,
                Map.of("maHDGoc", this.currentHoaDon.getMaHD()));
        if (resSub.getStatusCode() == 200) {
            List<HoaDon> hoaDonPhu = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resSub.getData()), HoaDon.class);
            currentHoaDonGocVaPhu.addAll(hoaDonPhu);
        }
        // 4. TẢI MÓN ĂN QUA API
        monOrderList.clear();
        if (this.currentHoaDon.getMaHD() != null) {
            Response resDetails = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS,
                    Map.of("maHD", this.currentHoaDon.getMaHD()));
            if (resDetails.getStatusCode() == 200) {
                List<entity.ChiTietHoaDon> details = utils.JsonUtil
                        .fromJsonList(utils.JsonUtil.toJson(resDetails.getData()), entity.ChiTietHoaDon.class);
                System.out.println("[UI-DATBAN] Tải thành công " + details.size() + " món cho HĐ: " + this.currentHoaDon.getMaHD());
                // 🔥 SỬA: Gộp món khi load từ Server về UI
                java.util.Map<String, MonOrder> aggregatedMap = new java.util.LinkedHashMap<>();
                for (entity.ChiTietHoaDon ct : details) {
                    String maMon = ct.getMaMon() != null ? ct.getMaMon() : ct.getTenMon();
                    if (aggregatedMap.containsKey(maMon)) {
                        MonOrder existing = aggregatedMap.get(maMon);
                        existing.setSoLuong(existing.getSoLuong() + ct.getSoLuong());
                    } else {
                        aggregatedMap.put(maMon, new MonOrder(maMon, ct.getTenMon(), ct.getDonGia(), ct.getSoLuong()));
                    }
                }
                monOrderList.addAll(aggregatedMap.values());
            }
        }
        // ---------------------------------------------------------
        // 🔥 ĐOẠN LOGIC MỚI: LOAD ƯU ĐíI TỪ HÓA ĐƠN LÊN COMBOBOX
        // ---------------------------------------------------------
        // Mặc định là null
        this.selectedUuDai = null;
        if (currentHoaDon.getMaUuDai() != null) {
            // Tìm ưu đãi trong danh sách đang load (dsUuDaiDangApDung) khớp với Mã Ưu Đãi
            // của HĐ
            UuDai uuDaiDaChon = dsUuDaiDangApDung.stream()
                    .filter(ud -> ud.getMaUuDai().equals(currentHoaDon.getMaUuDai()))
                    .findFirst()
                    .orElse(null);
            if (uuDaiDaChon != null) {
                this.selectedUuDai = uuDaiDaChon;
                // Tạo chuỗi hiển thị giống format trong loadPromoComboBox
                String displayString = String.format("%s (Giảm %.0f%%)", uuDaiDaChon.getTenUuDai(),
                        uuDaiDaChon.getGiaTri());
                promoComboBox.setValue(displayString);
            } else {
                // Trường hợp ưu đãi cũ đã hết hạn hoặc bị xóa -> Reset
                promoComboBox.getSelectionModel().selectFirst();
            }
        } else {
            // Nếu HĐ chưa có ưu đãi -> Chọn "Không áp dụng"
            promoComboBox.getSelectionModel().selectFirst();
        }
        tblMonDaChon.refresh();
        calculateTotal();
        // 5. HIỂN THỊ THÔNG TIN (MIDDLE PANEL)
        if (currentHoaDon.getKhachHang() != null) {
            txtTenKhachHang.setText(currentHoaDon.getKhachHang().getTenKH());
            txtSoDienThoai.setText(currentHoaDon.getKhachHang().getSoDT());
        } else {
            txtTenKhachHang.clear();
            txtSoDienThoai.clear();
        }
        // 🔥 SỬA: Hiển thị thời gian lên ComboBox và DatePicker
        if (currentHoaDon.getGioVao() != null) {
            datePickerThoiGianDen.setValue(currentHoaDon.getGioVao().toLocalDate());
            // Format giờ từ DB thành chuỗi (ví dụ "18:00")
            String gioDB = currentHoaDon.getGioVao().toLocalTime().format(timeFormatter);
            // Set giá trị cho ComboBox (nó sẽ hiển thị ngay cả khi không nằm trong list
            // options)
            comboThoiGian.setValue(gioDB);
        } else {
            datePickerThoiGianDen.setValue(LocalDate.now());
            // Mặc định chọn giờ đầu tiên hoặc giờ hiện tại
            comboThoiGian.getSelectionModel().selectFirst();
        }
        // 🔥 SỬA: Nếu là hóa đơn phụ (tách bàn), tiền cọc = 0 (vì tiền cọc nằm ở hóa đơn gốc)
        double hienThiTienCoc = currentHoaDon.getTienCoc();
        if (currentHoaDon.getMaHDGoc() != null && !currentHoaDon.getMaHDGoc().isEmpty()) {
            hienThiTienCoc = 0;
        }
        txtTienCoc.setText(String.format("%,.0f", hienThiTienCoc));
        // 6. Populate Label Bàn
        String tatCaBan = currentHoaDonGocVaPhu.stream()
                .map(h -> (h.getBan() != null) ? h.getBan().getMaBan() : "N/A")
                .collect(Collectors.joining(", "));
        lblBanDangChon.setText(tatCaBan);
        lblTrangThaiBan.setText(String.format("Đang xem %d bàn", currentHoaDonGocVaPhu.size()));
        if (currentHoaDon.getBan() != null) {
            txtSoLuongKhach.setText(String.valueOf(currentHoaDon.getBan().getSucChua()));
        }
        txtYeuCau.clear();
        // Update ComboBox Trạng Thái HĐ
        if (comboTrangThaiHienTai != null) {
            String dbVal = currentHoaDon.getTrangThai().getDbValue();
            String displayStatus;
            if (dbVal.equals(TrangThaiHoaDon.DANG_SU_DUNG.getDbValue()))
                displayStatus = "Đã nhận bàn";
            else if (dbVal.equals(TrangThaiHoaDon.DAT.getDbValue()))
                displayStatus = "Đã đặt";
            else if (dbVal.equals(TrangThaiHoaDon.CHO_XAC_NHAN.getDbValue()))
                displayStatus = "Chờ xác nhận";
            else
                displayStatus = "";
            comboTrangThaiHienTai.setValue(displayStatus);
        }
        // 7. TÔ MÀU BÀN TRÊN SƠ ĐỒ
        TrangThaiHoaDon trangThaiHdGoc = TrangThaiHoaDon.fromDbValue(currentHoaDon.getTrangThai().getDbValue());
        TrangThaiBan trangThaiCanTo = (trangThaiHdGoc == TrangThaiHoaDon.DANG_SU_DUNG)
                ? TrangThaiBan.DANG_SU_DUNG
                : TrangThaiBan.DA_DAT;
        for (HoaDon hoadon : currentHoaDonGocVaPhu) {
            if (hoadon.getBan() != null) {
                Button btn = tableButtonMap.get(hoadon.getBan().getMaBan());
                if (btn != null)
                    applyTableStyle(btn, trangThaiCanTo);
            }
        }
        // 8. Cập nhật nút
        isBookingConfirmed = true;
        updateButtonVisibility(true);
        if (btnThanhToanCoc != null) {
            boolean enableCocButton = (currentHoaDon != null
                    && currentHoaDon.getTrangThai() == TrangThaiHoaDon.CHO_XAC_NHAN);
            btnThanhToanCoc.setDisable(!enableCocButton);
        }
        if (vboxReceipt != null && !isRealTimeUpdate)
            vboxReceipt.setVisible(false);
    }

    private VBox createBookingCard(HoaDon hd, List<HoaDon> allPendingBookings) {
        // 1. Khởi tạo Card
        VBox card = new VBox(8);
        card.getStyleClass().add("booking-card");
        card.setPadding(new Insets(15));
        // 2. Lấy dữ liệu từ Hóa đơn
        String maGiaoDich = hd.getMaHD();
        // Xử lý trạng thái hiển thị tiếng Việt
        String trangThaiDb = hd.getTrangThai() != null ? hd.getTrangThai().getDbValue() : "Unknown";
        String trangThaiViet = switch (trangThaiDb) {
            case "Dat" -> "Đã đặt";
            case "DangSuDung" -> "Đã nhận bàn";
            case "HoaDonTam" -> "Hóa đơn tách";
            case "ChoXacNhan" -> "Chờ xác nhận";
            default -> trangThaiDb;
        };
        String gioVao = (hd.getGioVao() != null) ? hd.getGioVao().toLocalTime().format(timeFormatter) : "N/A";
        String sdtKhach = (hd.getKhachHang() != null && hd.getKhachHang().getSoDT() != null)
                ? hd.getKhachHang().getSoDT()
                : "N/A";
        // 3. Logic tìm tên bàn (SỬA LẠI: Tìm từ list CỤC BỘ thay vì gọi API N+1)
        String danhSachBanDayDu = "N/A";
        try {
            List<HoaDon> allRelatedHDs = new ArrayList<>();
            String maHDGocDeTim = (hd.getMaHDGoc() == null) ? hd.getMaHD() : hd.getMaHDGoc();
            
            // Tìm tất cả hóa đơn có cùng mã gốc (hoặc bản thân nó là gốc)
            for (HoaDon item : allPendingBookings) {
                if (item.getMaHD().equals(maHDGocDeTim) || maHDGocDeTim.equals(item.getMaHDGoc())) {
                    allRelatedHDs.add(item);
                }
            }

            // Gom danh sách mã bàn
            Set<String> uniqueBanSet = allRelatedHDs.stream()
                    .filter(h -> h.getBan() != null && h.getBan().getMaBan() != null
                            && !h.getBan().getMaBan().trim().isEmpty())
                    .map(h -> h.getBan().getMaBan())
                    .collect(Collectors.toSet());
            danhSachBanDayDu = uniqueBanSet.isEmpty() ? "N/A" : String.join(", ", new TreeSet<>(uniqueBanSet));
        } catch (Exception e) {
            Ban banHienTai = hd.getBan();
            danhSachBanDayDu = (banHienTai != null ? banHienTai.getMaBan() : "Lỗi");
        }
        // 4. Tạo các Label hiển thị
        Label lblMaHD = new Label(maGiaoDich != null ? maGiaoDich : "Mã: N/A");
        lblMaHD.getStyleClass().add("booking-card-id");
        Label lblSDT = new Label("SĐT: " + sdtKhach);
        Label lblTrangThai = new Label("Trạng thái: " + trangThaiViet);
        lblTrangThai.getStyleClass().add("booking-status-" + trangThaiDb.toLowerCase()); // CSS class theo trạng thái
        Label lblThoiGian = new Label("Giờ : " + gioVao);
        Label lblBan = new Label("Bàn: " + danhSachBanDayDu);
        // 5. 🔥 TẠO NÚT "SMART CommandType" 🔥
        Button btnSmartAction = new Button();
        btnSmartAction.setMaxWidth(Double.MAX_VALUE);
        btnSmartAction.setPrefHeight(40);
        btnSmartAction.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
        // --- CẤU HÌNH NÚT THEO TRẠNG THàI ---
        TrangThaiHoaDon ttEnum = hd.getTrangThai();
        if (ttEnum == TrangThaiHoaDon.DAT) {
            // Trưọng hợp 1: ĐàĐẶT -> Hành động: NHẬN BÀN
            btnSmartAction.setText("▶ Nhận bàn ngay");
            btnSmartAction.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
            btnSmartAction.setOnAction(e -> {
                e.consume();
                handleNhanBanNhanh(hd); // Gọi hàm nhận bàn
            });
        } else if (ttEnum == TrangThaiHoaDon.CHO_XAC_NHAN) {
            // Trưọng hợp 2: CHỜ CỌC -> Hành động: XàC NHẬN CỌC
            btnSmartAction.setText("💰 Xác nhận cọc");
            btnSmartAction.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
            btnSmartAction.setOnAction(e -> {
                e.consume();
                loadHoaDonToMainInterface(hd, false);
                openThanhToanCocPopup();
            });
        } else if (ttEnum == TrangThaiHoaDon.DANG_SU_DUNG || ttEnum == TrangThaiHoaDon.HOA_DON_TAM) {
            // Trưọng hợp 3: ĐANG PHỤC VỤ / ĐàNHẬN BÀN -> Hành động: THANH TOàN
            btnSmartAction.setText("💲 Thanh toán");
            btnSmartAction.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
            btnSmartAction.setOnAction(e -> {
                e.consume();
                loadHoaDonToMainInterface(hd, false);
                handleThanhToan();
            });
        } else {
            // Trưọng hợp khác: Chỉ xem
            btnSmartAction.setText("Xem chi tiết");
            btnSmartAction.setOnAction(e -> {
                e.consume();
                loadHoaDonToMainInterface(hd, false);
            });
        }
        // 6. Thêm tất cả vào Card
        card.getChildren().addAll(lblMaHD, lblSDT, lblTrangThai, lblThoiGian, lblBan, btnSmartAction);
        return card;
    }

    // ui.DatBan.java
    /**
     * 🔥 HÀM THÔNG MINH: Chuyển nhanh từ "Đã đặt" sang "Đang phục vụ" (Check-in).
     * Cập nhật cả HĐ Gốc và các HĐ Phụ, cập nhật màu bàn.
     */
    private void handleNhanBanNhanh(HoaDon hd) {
        new Thread(() -> {
            String maHDGoc = (hd.getMaHDGoc() != null) ? hd.getMaHDGoc() : hd.getMaHD();
            try {
                List<HoaDon> allRelated = new ArrayList<>();
                Response resGoc = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("maHD", maHDGoc));
                if (resGoc.getStatusCode() == 200) {
                    allRelated.add(utils.JsonUtil.convertValue(resGoc.getData(), HoaDon.class));
                }
                Response resSub = Client.sendWithParams(CommandType.GET_SUB_INVOICES, Map.of("maHDGoc", maHDGoc));
                if (resSub.getStatusCode() == 200) {
                    allRelated.addAll(utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resSub.getData()), HoaDon.class));
                }

                String newStatusDb = TrangThaiHoaDon.DANG_SU_DUNG.getDbValue();
                String newBanStatus = TrangThaiBan.DANG_SU_DUNG.getDbValue();
                for (HoaDon item : allRelated) {
                    Client.sendWithParams(CommandType.UPDATE_INVOICE, Map.of("maHD", item.getMaHD(), "trangThai", newStatusDb));
                    if (item.getBan() != null) {
                        Client.sendWithParams(CommandType.UPDATE_TABLE_STATUS, Map.of("maBan", item.getBan().getMaBan(), "newStatus", newBanStatus));
                    }
                }
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Nhận bàn thành công", "Đã nhận bàn cho HĐ " + maHDGoc + ". Khách bắt đầu sử dụng.");
                    loadTableGrids();
                    loadBookingCards();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi nhận bàn: " + e.getMessage()));
            }
        }).start();
    }

    // =========================================================
    // LOGIC TẢI MENU VÀ GỌI MÓN (ĐàSỬA)
    // =========================================================
    private void handleTimMon(String keyword) {
        if (keyword.trim().isEmpty()) {
            Response res = Client.send(CommandType.GET_MENU, null);
            List<entity.MonAn> dsEntityMonAn = (res.getStatusCode() == 200)
                    ? utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), entity.MonAn.class)
                    : new ArrayList<>();
            loadMonAnTable(dsEntityMonAn.stream().map(MonAn::fromEntity).collect(Collectors.toList()));
        } else {
            Response res = Client.sendWithParams(CommandType.GET_MENU, Map.of("query", keyword));
            List<entity.MonAn> dsEntityMonAn = (res.getStatusCode() == 200)
                    ? utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), entity.MonAn.class)
                    : new ArrayList<>();
            loadMonAnTable(dsEntityMonAn.stream().map(MonAn::fromEntity).collect(Collectors.toList()));
        }
    }

    private void handleMenuToggle(String maDanhMuc) {
        if (maDanhMuc == null)
            return;
        Response res = Client.sendWithParams(CommandType.GET_MENU, Map.of("maDM", maDanhMuc));
        List<entity.MonAn> dsEntityMonAn = (res.getStatusCode() == 200)
                ? utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), entity.MonAn.class)
                : new ArrayList<>();
        loadMonAnTable(dsEntityMonAn.stream().map(MonAn::fromEntity).collect(Collectors.toList()));
        txtTimMon.clear();
    }

    private void loadMonAnTable(List<MonAn> dsMon) {
        ObservableList<MonAn> monAnObservableList = FXCollections.observableArrayList(dsMon);
        tblMonAn.setItems(monAnObservableList);
        tblMonAn.refresh();
    }

    private void setupMonAnTable() {
        colTenMon.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTenMon()));
        // Format giá tiọn hiển thị cho đẹp (VD: 120,000)
        colGia.setCellValueFactory(cellData -> cellData.getValue().giaProperty());
        colGia.setCellFactory(tc -> new TableCell<MonAn, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f", item.doubleValue()));
                }
            }
        });
        colHinhAnh.setCellFactory(param -> new TableCell<MonAn, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    MonAn monAn = getTableView().getItems().get(getIndex());
                    String url = monAn.getHinhAnhUrl();
                    byte[] bytes = monAn.getHinhAnhBytes();

                    try {
                        if (url != null && !url.isEmpty()) {
                            imageView.setImage(new Image(url, true)); // true = load in background
                            setGraphic(imageView);
                        } else if (bytes != null && bytes.length > 0) {
                            imageView.setImage(new Image(new ByteArrayInputStream(bytes)));
                            setGraphic(imageView);
                        } else {
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });
        // 🔥 NÚT CHỌN: MÀU XANH Là
        colChon.setCellFactory(tc -> new TableCell<MonAn, Void>() {
            final Button btn = new Button("chọn");
            {
                // Style cho nút chọn
                btn.setStyle(
                        "-fx-background-color: #2f9e44; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btn.setPrefWidth(60);
                btn.setOnAction(event -> {
                    MonAn mon = getTableView().getItems().get(getIndex());
                    handleChonMon(mon);
                    calculateTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void setupMonOrderTable() {
        colOrderTenMon.setCellValueFactory(cellData -> cellData.getValue().tenMonProperty());
        // Format đơn giá trong bảng Order
        colOrderDonGia.setCellValueFactory(cellData -> cellData.getValue().donGiaProperty());
        colOrderDonGia.setCellFactory(tc -> new TableCell<MonOrder, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%,.0f", item.doubleValue()));
            }
        });
        colOrderSoLuong.setCellValueFactory(cellData -> cellData.getValue().soLuongProperty().asObject());
        // 🔥 NÚT TĂNG GIẢM: MÀU CAM VÀ XANH DƯƠNG
        colOrderTangGiam.setCellFactory(tc -> new TableCell<MonOrder, Void>() {
            final HBox box = new HBox(5);
            final Button btnMinus = new Button("-");
            final Button btnPlus = new Button("+");
            {
                // Style nút Trừ (-) : Màu Cam/Đọnhạt
                btnMinus.setStyle(
                        "-fx-background-color: #f08c00; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 30px; -fx-cursor: hand;");
                // Style nút Cộng (+) : Màu Xanh dương
                btnPlus.setStyle(
                        "-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 30px; -fx-cursor: hand;");
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(btnMinus, btnPlus);
                btnPlus.setOnAction(event -> {
                    MonOrder order = getTableView().getItems().get(getIndex());
                    order.setSoLuong(order.getSoLuong() + 1);
                    tblMonDaChon.refresh();
                    calculateTotal();
                    handleSuaHoaDon(); // Tự động lưu
                });
                btnMinus.setOnAction(event -> {
                    MonOrder order = getTableView().getItems().get(getIndex());
                    if (order.getSoLuong() > 1) {
                        order.setSoLuong(order.getSoLuong() - 1);
                        tblMonDaChon.refresh();
                        calculateTotal();
                        handleSuaHoaDon(); // Tự động lưu
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        // 🔥 NÚT HỦY: MÀU ĐỎ
        colOrderHuy.setCellFactory(tc -> new TableCell<MonOrder, Void>() {
            final Button btnHuy = new Button("X");
            {
                // Style nút Hủy : Màu Đọđậm
                btnHuy.setStyle(
                        "-fx-background-color: #e03131; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 30px; -fx-cursor: hand;");
                btnHuy.setOnAction(event -> {
                    MonOrder order = getTableView().getItems().get(getIndex());
                    monOrderList.remove(order);
                    tblMonDaChon.refresh();
                    calculateTotal();
                    handleSuaHoaDon(); // Tự động lưu
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnHuy);
            }
        });
        tblMonDaChon.setItems(monOrderList);
    }

    private void handleChonMon(MonAn mon) {
        Optional<MonOrder> existingOrder = monOrderList.stream()
                .filter(o -> o.getMaMon().equals(mon.getMaMon()))
                .findFirst();
        if (existingOrder.isPresent()) {
            MonOrder order = existingOrder.get();
            order.setSoLuong(order.getSoLuong() + 1);
        } else {
            monOrderList.add(new MonOrder(mon.getMaMon(), mon.getTenMon(), mon.getGiaBan(), 1));
        }
        tblMonDaChon.refresh();
        calculateTotal();
        handleSuaHoaDon(); // Tự động lưu
    }

    /**
     * TàNH TOàN VÀ HIỂN THỊ TỔNG TIỀN
     */
    /**
     * TàNH TOàN VÀ HIỂN THỊ TỔNG TIỀN (ĐàSỬA LOGIC VAT)
     */
    private void calculateTotal() {
        double tongTienMonAn = monOrderList.stream()
                .mapToDouble(order -> order.getDonGia() * order.getSoLuong())
                .sum();
        // 1. Phí dịch vụ (5% trên tổng món)
        final double SERVICE_FEE_RATE = 0.05;
        double phiDichVu = tongTienMonAn * SERVICE_FEE_RATE;
        // 2. 🔥 SỬA: Thuế VAT (8% TRÊN TỔNG MÓN ĂN - Theo yêu cầu)
        final double VAT_RATE = 0.08;
        // Code cũ (sai): double thueVAT = (tongTienMonAn + phiDichVu) * VAT_RATE;
        double thueVAT = tongTienMonAn * VAT_RATE; // Code mới (Đúng)
        // 3. Khuyến mãi
        double tienKhuyenMai = 0.0;
        if (selectedUuDai != null) {
            tienKhuyenMai = tongTienMonAn * (selectedUuDai.getGiaTri() / 100.0);
        }
        // Cập nhật model
        if (currentHoaDon != null) {
            currentHoaDon.setKhuyenMai(tienKhuyenMai);
            currentHoaDon.setPhiDichVu(phiDichVu);
            currentHoaDon.setThueVAT(thueVAT);
        }
        // 4. Tiọn cọc
        double tienCocDaThanhToan = 0.0;
        try {
            String tienCocRaw = txtTienCoc.getText().replaceAll("[^0-9.]", "");
            tienCocDaThanhToan = Double.parseDouble(tienCocRaw.isEmpty() ? "0" : tienCocRaw);
        } catch (NumberFormatException e) {
            tienCocDaThanhToan = 0.0;
        }
        // 5. Tổng thanh toán
        double tongTienThanhToan = tongTienMonAn + phiDichVu + thueVAT - tienKhuyenMai - tienCocDaThanhToan;
        // 6. Hiển thị lên UI
        if (lblTongTienMonAn != null)
            lblTongTienMonAn.setText(String.format("%,.0f Đ", tongTienMonAn));
        if (lblPhiDichVu != null)
            lblPhiDichVu.setText(String.format("%,.0f Đ", phiDichVu));
        if (lblThueVAT != null)
            lblThueVAT.setText(String.format("%,.0f Đ", thueVAT));
        if (lblKhuyenMai != null)
            lblKhuyenMai.setText(String.format("%,.0f Đ", tienKhuyenMai));
        if (lblTienCocSummary != null)
            lblTienCocSummary.setText(String.format("%,.0f Đ", tienCocDaThanhToan));
        if (lblTongTienThanhToan != null)
            lblTongTienThanhToan.setText(String.format("%,.0f Đ", Math.max(0, tongTienThanhToan)));
    }

    public void clearFormDatBan() {
        // 1. Clear các trưọng nhập liệu
        comboThoiGian.getSelectionModel().selectFirst();
        datePickerThoiGianDen.setValue(LocalDate.now()); //
        txtTenKhachHang.clear(); //
        txtSoDienThoai.clear(); //
        txtSoLuongKhach.clear(); //
        txtYeuCau.clear(); //
        txtTienCoc.clear(); //
        if (lblBanDangChon != null)
            lblBanDangChon.setText("Chưa chọn"); //
        if (lblTrangThaiBan != null)
            lblTrangThaiBan.setText("Trống"); //
        monOrderList.clear(); //
        calculateTotal(); //
        if (vboxReceipt != null) { //
            vboxReceipt.setVisible(false);
        }
        // === BƯỚC MỚI 2: RESET MÀU CỦA CỤM HÓA ĐƠN ĐANG XEM VỀ TRẠNG THàI THỰC TẾ ===
        // (Dùng list HĐ gốc/phụ để reset màu, sau đó clear list này)
        if (currentHoaDonGocVaPhu != null && !currentHoaDonGocVaPhu.isEmpty()) {
            System.out.println("LOG clearFormDatBan: Reset màu cho " + currentHoaDonGocVaPhu.size() + " bàn từ HĐ cũ.");
            for (HoaDon hdCu : currentHoaDonGocVaPhu) {
                if (hdCu.getBan() != null) {
                    Button btn = tableButtonMap.get(hdCu.getBan().getMaBan());
                    if (btn != null) {
                        // Trả bàn vọtrạng thái thực tế (tính toán lại theo giọhiện tại)
                        Ban banHienTai = hdCu.getBan();
                        TrangThaiBan trangThaiThuc = getTrangThaiHienThi(banHienTai, LocalTime.now());
                        applyTableStyle(btn, trangThaiThuc);
                    }
                }
            }
        }
        currentHoaDonGocVaPhu.clear(); // Reset list HĐ gốc/phụ
        // =========================================================================
        // 2. Reset màu của các bàn ĐANG CHỌN (màu xanh lá) vọmàu gốc
        for (Button btn : selectedButtonList) { //
            // Tìm đối tượng Ban tương ứng (cần để gọi getTrangThaiHienThi)
            Ban banGoc = selectedBanList.stream()
                    .filter(b -> selectedButtonList.indexOf(btn) == selectedBanList.indexOf(b))
                    .findFirst().orElse(null);
            if (banGoc != null) {
                TrangThaiBan trangThaiGoc = getTrangThaiHienThi(banGoc, LocalTime.now()); //
                applyTableStyle(btn, trangThaiGoc); // Trả lại màu trắng/đọtùy trạng thái gốc
            } else {
                // Fallback: Nếu không tìm thấy Ban, cứ trả vọmàu trắng
                applyTableStyle(btn, TrangThaiBan.TRONG);
            }
        }
        selectedBanList.clear(); // Xóa danh sách bàn đang chọn
        selectedButtonList.clear(); // Xóa danh sách nút đang chọn
        // 🔥 THÊM MỚI: Vô hiệu hóa nút Thanh toán cọc
        if (btnThanhToanCoc != null) {
            btnThanhToanCoc.setDisable(true);
        }
        // 3. Reset trạng thái logic
        isBookingConfirmed = false; //
        currentHoaDon = null; // Reset hóa đơn đang xem
        this.daThanhToanCoc = false;
        // 4. Cập nhật hiển thị nút vọtrạng thái TẠO MỚI
        updateButtonVisibility(false); //
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void filterGioTheoNgay(LocalDate ngayChon) {
        ObservableList<String> dynamicSlots = FXCollections.observableArrayList();
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(22, 0);
        LocalTime limit = LocalTime.now();
        while (!start.isAfter(end)) {
            // Nếu chọn hôm nay, chỉ hiện các giọsau hiện tại 15 phút
            if (ngayChon.equals(LocalDate.now())) {
                if (start.isAfter(limit.plusMinutes(15))) {
                    dynamicSlots.add(start.format(timeFormatter));
                }
            } else {
                dynamicSlots.add(start.format(timeFormatter));
            }
            start = start.plusMinutes(30);
        }
        comboThoiGian.setItems(dynamicSlots);
        if (!dynamicSlots.isEmpty())
            comboThoiGian.getSelectionModel().selectFirst();
    }

    // =========================================================
    // XỬ Là TÌM BÀN TRọNG - LOGIC TọI ƯU (SMART FIT)
    // =========================================================
    private void handleTimBanTrong() {
        LocalDate ngay;
        String gioStr;
        LocalTime gio;
        java.sql.Timestamp ts;
        int soLuongKhach = 0;
        try {
            ngay = datePickerThoiGianDen.getValue();
            gioStr = comboThoiGian.getValue();
            String khuVucChon = comboKhuVuc.getValue();
            // Parse số lượng khách
            String slKhachStr = txtSoLuongKhach.getText().trim();
            if (!slKhachStr.isEmpty()) {
                try {
                    soLuongKhach = Integer.parseInt(slKhachStr);
                    if (soLuongKhach < 0)
                        throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số lượng khách phải là số nguyên dương.");
                    return;
                }
            }
            if (ngay == null || gioStr == null || gioStr.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn Ngày và Giờ .");
                return;
            }
            gio = LocalTime.parse(gioStr, timeFormatter);
            LocalDateTime thoiGianChon = ngay.atTime(gio);
            if (thoiGianChon.isBefore(LocalDateTime.now())) {
                showAlert(Alert.AlertType.ERROR, "Thọi gian khýng hợp lệ",
                        "Bạn không thể đặt bàn cho thọi điểm đã trôi qua. Vui lòng chọn lại!");
                return;
            }
            ts = java.sql.Timestamp.valueOf(thoiGianChon);
            // Reset Form & Restore UI
            clearFormDatBan();
            this.currentHoaDon = null;
            this.isBookingConfirmed = false;
            datePickerThoiGianDen.setValue(ngay);
            comboThoiGian.setValue(gioStr);
            if (soLuongKhach > 0)
                txtSoLuongKhach.setText(String.valueOf(soLuongKhach));
            comboKhuVuc.setValue(khuVucChon);
            // Lấy dữ liệu từ API
            Response resTables = Client.sendWithParams(CommandType.GET_TABLES_WITH_AVAILABILITY,
                    Map.of("timestamp", ts.toString()));
            List<Ban> allBanInfo;
            if (resTables.getStatusCode() == 200) {
                allBanInfo = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resTables.getData()), Ban.class);
            } else {
                allBanInfo = new ArrayList<>();
            }
            Response resToday = Client.sendWithParams(CommandType.GET_INVOICES_TODAY, Map.of("date", ngay.toString()));
            if (resToday.getStatusCode() == 200) {
                this.dsHoaDonDatTrongNgay = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resToday.getData()),
                        HoaDon.class);
            } else {
                this.dsHoaDonDatTrongNgay = new ArrayList<>();
            }
            List<Ban> banHienThi = new ArrayList<>();
            for (Ban ban : allBanInfo) {
                // Ban ban = (Ban) banInfo.get("ban"); // Dồng bộ với List<Ban>
                // 1. ĐIỀU KIỆN TIÊN QUYẾT: BÀN PHẢI ĐỦ CHỖ
                if (soLuongKhach > 0 && ban.getSucChua() < soLuongKhach) {
                    continue;
                }
                boolean passKhuVuc = true;
                // 2. LOGIC LỌC
                if (khuVucChon != null && soLuongKhach > 0) {
                    if (khuVucChon.equals("Tự động")) {
                        // --- A. PHÂN LOẠI KHU VỰC ---
                        // Nhóm < 9 ngưọi: Ẩn Phòng riêng (để dành phòng cho nhóm 9-10 trở lên)
                        if (soLuongKhach < 9 && ban.getLoaiBan() == LoaiBan.PHONG) {
                            passKhuVuc = false;
                        }
                        // Nhóm >= 10 ngưọi: Ẩn bàn Sảnh (thưọng bàn sảnh chỉ max 8)
                        else if (soLuongKhach >= 10 && ban.getLoaiBan() != LoaiBan.PHONG) {
                            passKhuVuc = false;
                        }
                        // --- B. ĐỘ VỪA VẶN (FIT LOGIC) ---
                        if (passKhuVuc) {
                            int gheDu = ban.getSucChua() - soLuongKhach;
                            if (soLuongKhach < 10) {
                                // Với nhóm nhỏ (< 10): Cho phép dư tối đa 4 ghế
                                if (gheDu > 4)
                                    passKhuVuc = false;
                            } else {
                                // Với nhóm lớn (>= 10): àp dụng "Linh hoạt" (Loose Fit)
                                // Cho phép dư tối đa 5 ghế (vì bàn lớn hiếm)
                                // VD: Khách 13 -> Phòng 15 (Dư 2) OK.
                                if (gheDu > 5)
                                    passKhuVuc = false;
                            }
                        }
                    }
                    // Logic lọc cứng nếu user chọn cụ thể Khu vực (Tầng trệt/1/Phòng)
                    else if (!khuVucChon.equals("Tất cả")) {
                        if (khuVucChon.equals("Tầng trệt") && ban.getLoaiBan() != LoaiBan.TANG_TRET)
                            passKhuVuc = false;
                        else if (khuVucChon.equals("Tầng 1") && ban.getLoaiBan() != LoaiBan.TANG_1)
                            passKhuVuc = false;
                        else if (khuVucChon.equals("Phòng riêng") && ban.getLoaiBan() != LoaiBan.PHONG)
                            passKhuVuc = false;
                    }
                }
                if (!passKhuVuc)
                    continue;
                // 3. XàC ĐỊNH TRẠNG THàI MÀU SẮC
                Ban banMoi = new Ban(ban.getMaBan(), ban.getViTri(), ban.getSucChua(), ban.getLoaiBan(),
                        ban.getTrangThai());
                TrangThaiBan trangThaiHienThi = getTrangThaiHienThi(banMoi, gio);
                if (trangThaiHienThi != TrangThaiBan.TRONG) {
                    banMoi.setTrangThai(trangThaiHienThi);
                } else {
                    banMoi.setTrangThai(TrangThaiBan.TRONG);
                }
                banHienThi.add(banMoi);
            }
            loadTableGridsBase(banHienThi);
            if (banHienThi.isEmpty()) {
                String msg = "Không tìm thấy bàn trống phù hợp.";
                if (soLuongKhach > 0 && "Tự động".equals(khuVucChon)) {
                    msg += "\n(Hệ thống đang ẩn các bàn quá rộng hoặc quá chật để tối ưu. Hãy thử chọn khu vực 'Tất cả' để xem toàn bộ bàn).";
                }
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", msg);
            }
        } catch (java.time.format.DateTimeParseException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giờ  không hợp lệ.");
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tìm bàn: " + ex.getMessage());
            loadTableGrids();
        }
    }

    /**
     * 🔥 HÀM MỚI: Cập nhật trạng thái hiển thị của các nút chức năng
     * 
     * @param isViewingOldHoaDon true nếu đang xem HĐ cũ, false nếu đang tạo HĐ mới.
     */
    private void updateButtonVisibility(boolean isViewingOldHoaDon) {
        // Nút cho HĐ MỚI
        btnLuuDatHang.setVisible(!isViewingOldHoaDon);
        btnXacNhanBan.setVisible(!isViewingOldHoaDon); // Ẩn nút xác nhận bàn luôn
        // Nút cho HĐ CŨ
        // Nút cho HĐ CŨ - Áp dụng Style chuẩn Premium
        btnSuaMon.setVisible(isViewingOldHoaDon);
        btnSuaMon.setStyle(
                "-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        btnDoiBan.setVisible(isViewingOldHoaDon);
        btnDoiBan.setStyle(
                "-fx-background-color: #f08c00; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        btnTachBan.setVisible(isViewingOldHoaDon);
        btnTachBan.setStyle(
                "-fx-background-color: #e64980; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        btnGopBan.setVisible(isViewingOldHoaDon);
        btnGopBan.setStyle(
                "-fx-background-color: #7048e8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        btnHuyBan.setVisible(isViewingOldHoaDon);
        btnHuyBan.setStyle(
                "-fx-background-color: #e03131; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        // Nút Thanh Toán luôn hiển thị khi click
        btnThanhToan.setVisible(true);
        btnThanhToan.setStyle(
                "-fx-background-color: #0b7285; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
    }
    // === THÊM MỚI: CàC HÀM XỬ Là SỰ KIỆN CHO NÚT MỚI ===
    // =========================================================
    // HÀNH ĐỘNG CHO HÓA ĐƠN CŨ (SỬA, HỦY, ĐỔI)
    // =========================================================
    /**
     * 🔥 XỬ Là SỬA HÓA ĐƠN (Thông tin KH, Tiọn cọc, Danh sách món)
     */

    // ui.DatBan.java
    /**
     * 🔥 XỬ Là SỬA HÓA ĐƠN (Thông tin KH, Tiọn cọc, Danh sách món, VÀ KHUYẾN
     * MíI)
     */
    // 🔥 THÊM: Bộ đệm để tránh lag khi chọn món liên tục
    private javafx.animation.Timeline saveDelayTimeline;

    private void handleSuaHoaDon() {
        if (saveDelayTimeline != null) {
            saveDelayTimeline.stop();
        }
        saveDelayTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(800), // Đợi 800ms sau lần click cuối mới lưu
                e -> performSuaHoaDonActual()));
        saveDelayTimeline.play();
    }

    private void performSuaHoaDonActual() {
        if (currentHoaDon == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có hóa đơn nào được chọn để sửa.");
            return;
        }
        try {
            // 1. Cập nhật/Tìm Khách hàng qua API
            String sdt = txtSoDienThoai.getText();
            String tenKH = txtTenKhachHang.getText();

            // 🔥 ĐàSỬA: Gửi đúng cấu trúc KhachHang object cho Server
            KhachHang khRequest = new KhachHang();
            khRequest.setSoDT(sdt);
            khRequest.setTenKH(tenKH);

            Response resKH = Client.sendWithParams(CommandType.TIM_HOAC_TAO_KH, Map.of(
                    "khachHang", khRequest));
            KhachHang kh;
            if (resKH.getStatusCode() == 200) {
                kh = utils.JsonUtil.convertValue(resKH.getData(), KhachHang.class);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xử lý thông tin khách hàng: " + resKH.getMessage());
                return;
            }
            // 2. Cập nhật Tiền cọc
            double tienCoc = 0;
            try {
                String tienCocRaw = txtTienCoc.getText().replaceAll("[^0-9.]", "");
                tienCoc = Double.parseDouble(tienCocRaw.isEmpty() ? "0" : tienCocRaw);
            } catch (NumberFormatException e) {
            }
            // 3. Gộp tất cả thay đổi vào MỘT yêu cầu mạng duy nhất để tránh lag
            Map<String, Object> updateParams = new java.util.HashMap<>();
            updateParams.put("maHD", currentHoaDon.getMaHD());
            updateParams.put("soDT", kh.getSoDT());
            updateParams.put("tenKH", kh.getTenKH());
            updateParams.put("tienCoc", tienCoc);
            updateParams.put("itemsJson", utils.JsonUtil.toJson(convertToChiTiet(monOrderList)));
            
            if (selectedUuDai != null) {
                updateParams.put("maUuDai", selectedUuDai.getMaUuDai());
            }

            Client.sendWithParams(CommandType.UPDATE_INVOICE, updateParams);
            System.out.println("[PERF] Đã lưu hóa đơn tự động (Gộp + Debounced).");
            // Cập nhật lại object hiện tại trên RAM để đồng bộ
            if (selectedUuDai != null) {
                currentHoaDon.setMaUuDai(selectedUuDai.getMaUuDai());
                double tongTienMon = monOrderList.stream().mapToDouble(m -> m.getDonGia() * m.getSoLuong()).sum();
                currentHoaDon.setKhuyenMai(tongTienMon * (selectedUuDai.getGiaTri() / 100.0));
            } else {
                currentHoaDon.setMaUuDai(null);
                currentHoaDon.setKhuyenMai(0.0);
            }
            // ---------------------------------------------------------
            // Chỉ hiện thông báo thành công nếu ngưọi dùng nhấn nút Sửa Món
            if (btnSuaMon.isFocused()) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã cập nhật Hóa đơn " + currentHoaDon.getMaHD() + " thành công.");
            }
            // Tải lại danh sách bên trái
            loadBookingCards();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi CSDL", "Không thể cập nhật hóa đơn: " + e.getMessage());
        }
    }


    // [Trong file DatBan.java]
    /**
     * 🔥 XỬ LÝ HỦY BÀN (ĐàSỬA: Thêm Popup hiển thị tiền hoàn cọc + Inline CSS)
     */
    private void handleHuyBan() {
        if (currentHoaDon == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có hóa đơn nào được chọn để hủy.");
            return;
        }
        if (currentHoaDon.getTrangThai() != null && currentHoaDon.getTrangThai().getDbValue().equals("DaThanhToan")) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy Hóa đơn đã thanh toán.");
            return;
        }
        if (currentHoaDon.getGioVao() == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xác định giờ vào của hóa đơn để tính hoàn cọc.");
            return;
        }
        // --- 1. Tính toán thời gian còn lại và tiền hoàn cọc ---
        LocalDateTime gioVao = currentHoaDon.getGioVao();
        LocalDateTime now = LocalDateTime.now();
        long minutesToArrival = java.time.Duration.between(now, gioVao).toMinutes();
        double tienCoc = currentHoaDon.getTienCoc();
        double refundPercentage = 0.0;
        String policyApplied;
        if (minutesToArrival >= 120) { // Hủy trước >= 2 giờ
            refundPercentage = 0.8;
            policyApplied = "Hủy trước ≥2 giờ: hoàn 80% tiền cọc.";
        } else if (minutesToArrival >= 60) { // Hủy trong 12 giờ
            refundPercentage = 0.5;
            policyApplied = "Hủy trong 12 giờ: hoàn 50% tiền cọc.";
        } else { // Hủy < 1 giờ hoặc đã qua giờ vào
            refundPercentage = 0.0;
            if (now.isAfter(gioVao.plusMinutes(30))) {
                policyApplied = "Đến muộn trên 30 phút: không hoàn tiền cọc.";
            } else {
                policyApplied = "Hủy sát giờ (<1 giờ): không hoàn tiền cọc.";
            }
        }
        double refundAmount = tienCoc * refundPercentage;
        DecimalFormat currencyFormatter = new DecimalFormat("###,### VNĐ");
        // --- 2. Tạo Alert và ÁP DỤNG CSS ---
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("XÁC NHẬN HỦY BÀN");
        confirmationAlert.setHeaderText("Hủy Hóa đơn " + currentHoaDon.getMaHD() + "?");
        String contentText = String.format(
                "Chính sách áp dụng: %s\n" +
                        "Tiền cọc đã nhận: %s\n" +
                        "Số tiền hoàn lại cho khách: %s (%.0f%%)\n\n" +
                        "Bạn có chắc chắn muốn hủy toàn bộ cụm Hóa đơn này và trả bàn về trống không?",
                policyApplied,
                currencyFormatter.format(tienCoc),
                currencyFormatter.format(refundAmount),
                refundPercentage * 100);
        confirmationAlert.setContentText(contentText);
        // Đổi tên nút
        ButtonType confirmButton = new ButtonType("Xác nhận Hủy");
        ButtonType cancelButton = ButtonType.CANCEL;
        confirmationAlert.getButtonTypes().setAll(confirmButton, cancelButton);
        // --- 🔥 THÊM CSS TRỰC TIẾP ---
        DialogPane dialogPane = confirmationAlert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #f8f9fa; " + // Nền sáng
                        "-fx-border-color: #dee2e6; " + // Viền xám nhạt
                        "-fx-border-width: 1px;");
        // Style Header Text
        dialogPane.lookup(".header-panel .label").setStyle(
                "-fx-font-size: 1.2em; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #dc3545;" // Màu đỏ cảnh báo
        );
        // Style Content Text
        dialogPane.lookup(".content.label").setStyle(
                "-fx-font-size: 1.0em; " +
                        "-fx-line-spacing: 5px;");
        // Style Buttons
        Button confirmBtnNode = (Button) dialogPane.lookupButton(confirmButton);
        confirmBtnNode.setStyle(
                "-fx-background-color: #dc3545; " + // Nút xác nhận màu đỏ
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8px 15px;");
        Button cancelBtnNode = (Button) dialogPane.lookupButton(cancelButton);
        cancelBtnNode.setStyle(
                "-fx-background-color: #6c757d; " + // Nút hủy màu xám
                        "-fx-text-fill: white; " +
                        "-fx-padding: 8px 15px;");
        // -----------------------------
        Optional<ButtonType> result = confirmationAlert.showAndWait();
        // --- 3. Xử lý kết quả từ Popup ---
        if (result.isPresent() && result.get() == confirmButton) {
            try {
                List<HoaDon> allRelatedHDs = new ArrayList<>(currentHoaDonGocVaPhu);
                for (HoaDon hdToHuy : allRelatedHDs) {
                    Client.sendWithParams(CommandType.CANCEL_INVOICE, Map.of(
                            "maHD", hdToHuy.getMaHD(),
                            "trangThai", TrangThaiHoaDon.DA_HUY.getDbValue(),
                            "setGioRa", true));
                    if (hdToHuy.getBan() != null) {
                        Client.sendWithParams(CommandType.UPDATE_TABLE_STATUS, Map.of(
                                "maBan", hdToHuy.getBan().getMaBan(),
                                "newStatus", TrangThaiBan.TRONG.getDbValue()));
                    }
                }
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã hủy toàn bộ cụm Hóa đơn gốc " + currentHoaDon.getMaHD() +
                                ".\nSố tiền cần hoàn lại cho khách: " + currencyFormatter.format(refundAmount));
                clearFormDatBan();
                loadBookingCards();
                loadTableGrids();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi CSDL", "Không thể hủy hóa đơn: " + e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Đã hủy", "Thao tác hủy bàn đã được hủy bỏ.");
        }
    }

    // =========================================================
    // XỬ LÝ THANH TOÁN (MỚI)
    // =========================================================
    @SuppressWarnings("unchecked")
    private void handleThanhToan() {
        if (currentHoaDon == null) {
            showAlert(Alert.AlertType.WARNING, "Không có hóa đơn",
                    "Vui lòng chọn đơn đặt/đang phục vụ bên trái để xem hóa đơn.");
            return;
        }
        if (currentHoaDon.getTrangThai().getDbValue().equals("DaThanhToan")) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Hóa đơn này đã được thanh toán.");
            return;
        }
        // 1. Cập nhật và lưu thay đổi vào CSDL
        handleSuaHoaDon();
        // 2. Tải lại chi tiết món ăn (monOrderList) từ CSDL mới nhất
        monOrderList.clear();
        if (currentHoaDon.getMaHD() != null) {
            Response resDetails = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS,
                    Map.of("maHD", currentHoaDon.getMaHD()));
            if (resDetails.getStatusCode() == 200) {
                List<entity.ChiTietHoaDon> details = utils.JsonUtil
                        .fromJsonList(utils.JsonUtil.toJson(resDetails.getData()), entity.ChiTietHoaDon.class);
                // 🔥 Gộp món khi load lại cho thanh toán
                java.util.Map<String, MonOrder> aggregatedMap = new java.util.LinkedHashMap<>();
                for (entity.ChiTietHoaDon ct : details) {
                    String maMon = ct.getMaMon() != null ? ct.getMaMon() : ct.getTenMon();
                    if (aggregatedMap.containsKey(maMon)) {
                        MonOrder existing = aggregatedMap.get(maMon);
                        existing.setSoLuong(existing.getSoLuong() + ct.getSoLuong());
                    } else {
                        aggregatedMap.put(maMon, new MonOrder(maMon, ct.getTenMon(), ct.getDonGia(), ct.getSoLuong()));
                    }
                }
                monOrderList.addAll(aggregatedMap.values());
            }
        }
        // 3. Tính toán lại tổng tiền
        calculateTotal();
        // 4. HIỂN THỊ PANEL THANH TOÁN (vboxReceipt)
        if (vboxReceipt != null) {
            vboxReceipt.setVisible(true);
            // -------------------------------------------------------------
            // CẬP NHẬT THÔNG TIN CHUNG HÓA ĐƠN
            // -------------------------------------------------------------
            String maHD = currentHoaDon.getMaHD() != null ? currentHoaDon.getMaHD() : "N/A";
            // Bàn: Lấy từ HĐ Gốc
            String tenBan = currentHoaDon.getBan() != null ? currentHoaDon.getBan().getMaBan() : "N/A";
            // Giờ vào: Lấy từ trường GioVao của HĐ
            String gioVaoStr = currentHoaDon.getGioVao() != null
                    ? currentHoaDon.getGioVao().toLocalTime().format(timeFormatter)
                    : "N/A";
            // Giờ ra: Lấy giờ hệ thống hiện tại
            String gioRaStr = LocalTime.now().format(timeFormatter);
            // Thu ngân: Lấy tên nhân viên đang đăng nhập (Cần có biến NhanVien đang đăng
            // nhập)
            String tenThuNgan = "N/A";
            try {
                // Gọi hàm static từ MainApp để lấy đối tượng TaiKhoan
                TaiKhoan tk = MainApp.getLoggedInUser();
                if (tk != null && tk.getNhanVien() != null) {
                    // Giả định tên nhân viên được lấy bằng getHoTen()
                    tenThuNgan = tk.getNhanVien().getHoTen();
                }
            } catch (Exception e) {
                // Xử lý lỗi nếu việc lấy thông tin thất bại
            }
            // Đổ dữ liệu vào Labels
            if (lblSoHD != null)
                lblSoHD.setText("Số HĐ: " + maHD);
            if (lblBanHD != null)
                lblBanHD.setText("Bàn: " + tenBan);
            if (lblThuNgan != null)
                lblThuNgan.setText("Thu ngân: " + tenThuNgan);
            if (lblGioVao != null)
                lblGioVao.setText("Giờ vào: " + gioVaoStr);
            if (lblGioRa != null)
                lblGioRa.setText("Giờ ra: " + gioRaStr);
            // ---------------------------------------------------------
            // 5. CẬP NHẬT TABLE VIEW CHO THANH TOÁN (Danh sách món ăn)
            if (tblHoaDon != null) {
                ObservableList<TableColumn<MonOrder, ?>> columns = tblHoaDon.getColumns();
                if (columns.size() >= 5) {
                    TableColumn<MonOrder, Integer> colStt = (TableColumn<MonOrder, Integer>) columns.get(0);
                    colStt.setCellValueFactory(
                            data -> new SimpleIntegerProperty(tblHoaDon.getItems().indexOf(data.getValue()) + 1)
                                    .asObject());
                    TableColumn<MonOrder, String> colTenMon = (TableColumn<MonOrder, String>) columns.get(1);
                    colTenMon.setCellValueFactory(data -> data.getValue().tenMonProperty());
                    TableColumn<MonOrder, Integer> colSL = (TableColumn<MonOrder, Integer>) columns.get(2);
                    colSL.setCellValueFactory(data -> data.getValue().soLuongProperty().asObject());
                    TableColumn<MonOrder, Double> colDonGia = (TableColumn<MonOrder, Double>) columns.get(3);
                    colDonGia.setCellValueFactory(data -> data.getValue().donGiaProperty().asObject());
                    TableColumn<MonOrder, Double> colTong = (TableColumn<MonOrder, Double>) columns.get(4);
                    colTong.setCellValueFactory(data -> {
                        double total = data.getValue().getSoLuong() * data.getValue().getDonGia();
                        return new SimpleDoubleProperty(total).asObject();
                    });
                    tblHoaDon.setItems(monOrderList);
                    tblHoaDon.refresh();
                } else {
                }
            }
            // 6. TÍNH TOÁN VÀ HIỂN THỊ TỔNG TIỀN (Sử dụng hàm tập trung để tránh sai lệch)
            calculateTotal();
        }
    }

    // =========================================================
    // LỚP VIEWMODEL (SỬA LỖI KIỂU DỮ LIỆU)
    // =========================================================
    public static class MonAn {
        private final SimpleStringProperty maMon;
        private final SimpleStringProperty tenMon;
        private final byte[] hinhAnhBytes;
        private final String hinhAnhUrl;
        private final SimpleDoubleProperty giaBan;

        public MonAn(String maMon, String tenMon, double giaBan, byte[] hinhAnhBytes, String hinhAnhUrl) {
            this.maMon = new SimpleStringProperty(maMon);
            this.tenMon = new SimpleStringProperty(tenMon);
            this.giaBan = new SimpleDoubleProperty(giaBan);
            this.hinhAnhBytes = hinhAnhBytes;
            this.hinhAnhUrl = hinhAnhUrl;
        }

        public String getMaMon() {
            return maMon.get();
        }

        public String getTenMon() {
            return tenMon.get();
        }

        public SimpleStringProperty tenMonProperty() {
            return tenMon;
        }

        public double getGiaBan() {
            return giaBan.get();
        }

        public SimpleDoubleProperty giaProperty() {
            return giaBan;
        }

        public byte[] getHinhAnhBytes() {
            return hinhAnhBytes;
        }

        public String getHinhAnhUrl() {
            return hinhAnhUrl;
        }

        public static MonAn fromEntity(entity.MonAn entity) {
            return new MonAn(entity.getMaMon(), entity.getTenMon(), entity.getGiaBan(), entity.getHinhAnh(),
                    entity.getHinhAnhUrl());
        }
    }

    public static class MonOrder implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private String maMon_entity;
        private String tenMon_entity;
        private double donGia_entity;
        private int soLuong_entity;
        private transient SimpleStringProperty maMon;
        private transient SimpleStringProperty tenMon;
        private transient SimpleDoubleProperty donGia;
        private transient SimpleIntegerProperty soLuong;

        public MonOrder(String maMon, String tenMon, double donGia, int soLuong) {
            this.maMon_entity = maMon;
            this.tenMon_entity = tenMon;
            this.donGia_entity = donGia;
            this.soLuong_entity = soLuong;
            initProperties();
        }

        private void initProperties() {
            if (maMon == null)
                maMon = new SimpleStringProperty(maMon_entity);
            if (tenMon == null)
                tenMon = new SimpleStringProperty(tenMon_entity);
            if (donGia == null)
                donGia = new SimpleDoubleProperty(donGia_entity);
            if (soLuong == null)
                soLuong = new SimpleIntegerProperty(soLuong_entity);
        }

        public String getMaMon() {
            initProperties();
            return maMon.get();
        }

        public String getTenMon() {
            initProperties();
            return tenMon.get();
        }

        public SimpleStringProperty tenMonProperty() {
            initProperties();
            return tenMon;
        }

        public double getDonGia() {
            initProperties();
            return donGia.get();
        }

        public SimpleDoubleProperty donGiaProperty() {
            initProperties();
            return donGia;
        }

        public int getSoLuong() {
            initProperties();
            return soLuong.get();
        }

        public void setSoLuong(int value) {
            initProperties();
            this.soLuong.set(value);
            this.soLuong_entity = value;
        }

        public SimpleIntegerProperty soLuongProperty() {
            initProperties();
            return soLuong;
        }
    }

    /**
     * 🔥 HELPER MỚI: Tải lại danh sách HĐ đang chờ cho logic tô màu.
     * Cần phải PUBLIC để DoiBanPopupController có thể gọi.
     */
    public void loadDsHoaDonDatTrongNgay(LocalDate date) {
        Response res = Client.sendWithParams(CommandType.GET_INVOICES_BY_DATE, Map.of("date", date.toString()));
        if (res.getStatusCode() == 200) {
            this.dsHoaDonDatTrongNgay = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), HoaDon.class);
        } else {
            this.dsHoaDonDatTrongNgay = new ArrayList<>();
        }
    }

    /**
     * HELPER: Kiểm tra xem bàn có nằm trong cụm HĐ đang xem (HĐ Gốc/Phụ) không.
     * Cần phải PUBLIC để TachBanPopupController và DoiBanPopupController có thể
     * gọi.
     */
    public boolean isBanInCurrentBooking(Ban ban) {
        if (ban == null || currentHoaDonGocVaPhu.isEmpty())
            return false;
        return currentHoaDonGocVaPhu.stream()
                .filter(hd -> hd.getBan() != null)
                .anyMatch(hd -> hd.getBan().getMaBan().equals(ban.getMaBan()));
    }
    /**
     * 🔥 THÊM MỚI: Tự động tìm tên khách hàng khi gõ số điện thoại
     */
    private void handleTimKhachHangNhanh(String phone) {
        try {
            Response res = Client.sendWithParams(CommandType.FIND_CUSTOMER_BY_PHONE, java.util.Map.of("phone", phone));
            if (res.getStatusCode() == 200 && res.getData() != null) {
                entity.KhachHang kh = utils.JsonUtil.convertValue(res.getData(), entity.KhachHang.class);
                if (kh != null && kh.getTenKH() != null) {
                    javafx.application.Platform.runLater(() -> {
                        // Chỉ điền tên nếu ô tên đang trống để tránh ghi đè dữ liệu nhân viên đang nhập dở
                        if (txtTenKhachHang.getText() == null || txtTenKhachHang.getText().trim().isEmpty() || txtTenKhachHang.getText().startsWith("Khách ")) {
                            txtTenKhachHang.setText(kh.getTenKH());
                        }
                    });
                }
            }
        } catch (Exception e) {
            // Im lặng bỏ qua nếu không tìm thấy (Khách mới)
        }
    }
}