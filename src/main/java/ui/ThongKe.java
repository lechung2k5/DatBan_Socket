package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart; // Import LineChart
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.scene.text.Font;
public class ThongKe {
    // === DAO REMOVED ===
    // Using Client instead
    // === Frame Switching ===
    @FXML private ComboBox<String> viewSelectorCombo;
    @FXML private VBox frameHoaDon;
    @FXML private VBox frameMonAn;
    @FXML private VBox frameDoanhThu;
    @FXML private VBox frameKhuVuc;
    // === General KPIs ===
    @FXML private Label lblNgayHienTai;
    @FXML private Label lblTongHoaDon;
    @FXML private Label lblTongDoanhThu;
    // === Frame Hoa Don ===
    @FXML private ComboBox<Integer> cbHoaDonNgay;
    @FXML private ComboBox<Integer> cbHoaDonThang;
    @FXML private ComboBox<Integer> cbHoaDonNam;
    @FXML private Button btnLocHoaDon, btnHoaDonHienTai;
    @FXML private TableView<HoaDonThongKe> tblHoaDon;
    @FXML private TableColumn<HoaDonThongKe, String> colMaHoaDon;
    @FXML private TableColumn<HoaDonThongKe, String> colThoiGianVao;
    @FXML private TableColumn<HoaDonThongKe, Number> colTongTien;
    @FXML private TableColumn<HoaDonThongKe, Void> colXemChiTiet;
    @FXML private Button btnExportHoaDon;
    @FXML private Label lblHoaDon_TongSoHD;
    @FXML private Label lblHoaDon_TongDoanhThu;
    // === Frame Mon An ===
    @FXML private ComboBox<Integer> cbMonAnNgay;
    @FXML private ComboBox<Integer> cbMonAnThang;
    @FXML private ComboBox<Integer> cbMonAnNam;
    @FXML private Button btnLocMonAn, btnMonAnHienTai;
    @FXML private TableView<MonBanChay> tblMonBanChay;
    @FXML private TableColumn<MonBanChay, Integer> colSTT;
    @FXML private TableColumn<MonBanChay, String> colTenMon;
    @FXML private TableColumn<MonBanChay, Integer> colSoLuong;
    @FXML private TableColumn<MonBanChay, Number> colDoanhThuMon;
    @FXML private Button btnExportMonAn;
    @FXML private PieChart pieChartMonAn;
    // === Frame Doanh Thu (ĐàSỬA) ===
    @FXML private LineChart<String, Number> chartDoanhThu; // Đổi thành LineChart
    @FXML private DatePicker dpDoanhThuNgay;
    @FXML private Button btnDoanhThuHienTai;
    @FXML private Button btnDoanhThuTroVe;
    @FXML private Button btnDoanhThuTiep;
    private LocalDate currentDoanhThuWeekStartDate;
    // === Frame Khu Vuc ===
    @FXML private BarChart<String, Number> chartKhuVuc; // Giữ là BarChart
    @FXML private Button btnKhuVucHienTai;
    @FXML private Button btnKhuVucTroVe;
    @FXML private Button btnKhuVucTiep;
    @FXML private DatePicker dpKhuVucNgay;
    private LocalDate currentWeekStartDate;
    // Formatters
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter chartDateFormatter = DateTimeFormatter.ofPattern("E dd/MM");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    @FXML
    public void initialize() {
        // DAOs removed
        currencyFormatter.setMaximumFractionDigits(0);
        setupViewSelector();
        setupHoaDonFilters();
        setupMonAnFilters();
        // setupOtherFilters(); // Đã xóa
        setupInvoiceTable();
        setupBestSellersTable();
        setupBestSellersPieChart();
        // Cài đặt và tải dữ liệu ban đầu cho frame Doanh thu (MỚI)
        dpDoanhThuNgay.setValue(LocalDate.now());
        setupDoanhThuNavigation();
        loadDoanhThuDataForWeek();
        // Cài đặt và tải dữ liệu ban đầu cho frame Khu Vực
        dpKhuVucNgay.setValue(LocalDate.now());
        setupKhuVucNavigation();
        loadKhuVucDataForWeek();
        loadInitialKpiData(); // Load KPI ban đầu
    }
    // --- Setup Methods ---
    private void setupViewSelector() {
        ObservableList<String> views = FXCollections.observableArrayList("Số lượng hóa đơn", "Món bán chạy",
        "Biểu đồ Doanh thu", "Doanh thu Khu vực");
        viewSelectorCombo.setItems(views);
        viewSelectorCombo.setValue(views.get(0)); // Mặc định hiển thị "Số lượng hóa đơn"
        viewSelectorCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Ẩn tất cả các frame trước
                frameHoaDon.setVisible(false);
                frameMonAn.setVisible(false);
                frameDoanhThu.setVisible(false);
                frameKhuVuc.setVisible(false);
                // Hiển thị frame được chọn
                switch (newVal) {
                    case "Số lượng hóa đơn": frameHoaDon.setVisible(true); break;
                    case "Món bán chạy": frameMonAn.setVisible(true); break;
                    case "Biểu đồ Doanh thu": frameDoanhThu.setVisible(true); break;
                    case "Doanh thu Khu vực": frameKhuVuc.setVisible(true); break;
                }
            }
        });
        // Thiết lập trạng thái hiển thị ban đầu
        frameHoaDon.setVisible(true);
        frameMonAn.setVisible(false);
        frameDoanhThu.setVisible(false);
        frameKhuVuc.setVisible(false);
    }
    // Hàm này không còn cần thiết nữa vì ComboBox đã bị xóa
    // private void setupOtherFilters() { ... }
    private void setupHoaDonFilters() {
        ObservableList<Integer> days = FXCollections.observableArrayList();
        days.add(null);
        days.addAll(IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toList()));
        
        ObservableList<Integer> months = FXCollections.observableArrayList();
        months.add(null);
        months.addAll(IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList()));
        
        // Tạo danh sách năm động từ 2024 đến năm hiện tại
        int currentYear = LocalDate.now().getYear();
        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int y = 2024; y <= currentYear; y++) {
            years.add(y);
        }
        
        cbHoaDonNgay.setItems(days);
        cbHoaDonThang.setItems(months);
        cbHoaDonNam.setItems(years);
        
        // Mặc định là tháng hiện tại, năm hiện tại (hoặc Tất cả tháng nếu muốn)
        cbHoaDonNgay.setValue(null);
        cbHoaDonThang.setValue(null);
        cbHoaDonNam.setValue(currentYear);
        
        btnLocHoaDon.setOnAction(event -> loadHoaDonDataFromDAO());
        
        // Nút "Hôm nay" - Đặt về ngày/tháng/năm hiện tại
        btnHoaDonHienTai.setOnAction(event -> {
            LocalDate today = LocalDate.now();
            cbHoaDonNgay.setValue(today.getDayOfMonth());
            cbHoaDonThang.setValue(today.getMonthValue());
            cbHoaDonNam.setValue(today.getYear());
            loadHoaDonDataFromDAO();
        });

        loadHoaDonDataFromDAO();
    }
    private void setupMonAnFilters() {
        ObservableList<Integer> days = FXCollections.observableArrayList();
        days.add(null);
        days.addAll(IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toList()));
        cbMonAnNgay.setItems(days);
        cbMonAnNgay.setValue(null);

        ObservableList<Integer> months = FXCollections.observableArrayList();
        months.add(null);
        months.addAll(IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList()));
        cbMonAnThang.setItems(months);
        cbMonAnThang.setValue(null);

        int currentYear = LocalDate.now().getYear();
        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int y = 2024; y <= currentYear; y++) {
            years.add(y);
        }
        cbMonAnNam.setItems(years);
        cbMonAnNam.setValue(currentYear);

        btnLocMonAn.setOnAction(event -> loadMonAnDataFromDAO());
        
        btnMonAnHienTai.setOnAction(event -> {
            LocalDate today = LocalDate.now();
            cbMonAnNgay.setValue(today.getDayOfMonth());
            cbMonAnThang.setValue(today.getMonthValue());
            cbMonAnNam.setValue(today.getYear());
            loadMonAnDataFromDAO();
        });

        loadMonAnDataFromDAO();
    }
    private void loadInitialKpiData() {
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(dateFormatter);
        Response res = Client.sendWithParams(CommandType.GET_KPIS_FOR_DATE, Map.of("date", today.toString()));
        if (res.getStatusCode() == 200) {
            Map<String, Object> kpiData = (Map<String, Object>) res.getData();
            int totalInvoicesToday = ((Number) kpiData.getOrDefault("totalInvoices", 0)).intValue();
            double totalRevenueToday = ((Number) kpiData.getOrDefault("totalRevenue", 0.0)).doubleValue();
            lblNgayHienTai.setText(formattedDate);
            lblTongHoaDon.setText(String.valueOf(totalInvoicesToday));
            lblTongDoanhThu.setText(currencyFormatter.format(totalRevenueToday));
        }
    }
    private void loadHoaDonDataFromDAO() {
        Integer ngay = cbHoaDonNgay.getValue();
        Integer thang = cbHoaDonThang.getValue();
        Integer nam = cbHoaDonNam.getValue();
        Map<String, Object> params = new HashMap<>();
        params.put("day", ngay);
        params.put("month", thang);
        params.put("year", nam);
        Response res = Client.sendWithParams(CommandType.GET_INVOICE_STATS, params);
        if (res.getStatusCode() == 200) {
            Map<String, Object> result = (Map<String, Object>) res.getData();
            List<HoaDonThongKe> listFromApi = JsonUtil.fromJsonList(JsonUtil.toJson(result.get("list")), HoaDonThongKe.class);
            ObservableList<HoaDonThongKe> list = FXCollections.observableArrayList(listFromApi);
            int totalInvoices = ((Number) result.get("totalInvoices")).intValue();
            double totalRevenue = ((Number) result.get("totalRevenue")).doubleValue();
            tblHoaDon.setItems(list);
            lblHoaDon_TongSoHD.setText(String.valueOf(totalInvoices));
            lblHoaDon_TongDoanhThu.setText(currencyFormatter.format(totalRevenue));
        }
    }
    private void loadMonAnDataFromDAO() {
        Integer ngay = cbMonAnNgay.getValue();
        Integer thang = cbMonAnThang.getValue();
        Integer nam = cbMonAnNam.getValue();
        Map<String, Object> params = new HashMap<>();
        params.put("day", ngay);
        params.put("month", thang);
        params.put("year", nam);
        Response res = Client.sendWithParams(CommandType.GET_TOP_SELLING_ITEMS, params);
        if (res.getStatusCode() == 200) {
            List<MonBanChay> listFromApi = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), MonBanChay.class);
            ObservableList<MonBanChay> data = FXCollections.observableArrayList(listFromApi);
            tblMonBanChay.setItems(data);
            updateBestSellersPieChart(data);
        }
    }
    private void setupInvoiceTable() {
        tblHoaDon.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Tự động co giãn cột
        colMaHoaDon.setCellValueFactory(new PropertyValueFactory<>("maHD"));
        colThoiGianVao.setCellValueFactory(new PropertyValueFactory<>("thoiGianVao"));
        colTongTien.setCellValueFactory(new PropertyValueFactory<>("tongTien"));
        // Định dạng cột Tổng tiền
        colTongTien.setCellFactory(column -> new TableCell<HoaDonThongKe, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormatter.format(item.doubleValue()));
            }
        });
        // Cài đặt nút Xem trong cột Xem chi tiết
        colXemChiTiet.setCellFactory(param -> new TableCell<HoaDonThongKe, Void>() {
            private final Button btn = new Button("Xem");
            private final HBox pane = new HBox(btn); // Đặt nút vào HBox để căn giữa
            {
                btn.getStyleClass().add("view-button"); // Áp dụng style CSS
                btn.setOnAction(event -> { // Gán sự kiện click
                    HoaDonThongKe data = getTableView().getItems().get(getIndex()); // Lấy dữ liệu dòng hiện tại
                    if (data != null) {
                        showChiTietHoaDonDialog(data); // Gọi hàm hiển thị dialog chi tiết
                    }
                });
                pane.setAlignment(Pos.CENTER); // Căn giữa HBox (và nút bên trong)
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane); // Hiển thị HBox chứa nút nếu ô không rỗng
            }
        });
    }
    private void showChiTietHoaDonDialog(HoaDonThongKe selectedHoaDon) {
        // 1. Lấy dữ liệu đầy đủ từ API
        HoaDon hoaDonFull = null;
        List<ChiTietHoaDon> chiTietList = new ArrayList<>();
        Response resHD = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("id", selectedHoaDon.getMaHD()));
        if (resHD.getStatusCode() == 200) {
            hoaDonFull = JsonUtil.fromJson(JsonUtil.toJson(resHD.getData()), HoaDon.class);
        }
        Response resCT = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, Map.of("invoiceId", selectedHoaDon.getMaHD()));
        if (resCT.getStatusCode() == 200) {
            chiTietList = JsonUtil.fromJsonList(JsonUtil.toJson(resCT.getData()), ChiTietHoaDon.class);
        }
        if (hoaDonFull == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy chi tiết cho hóa đơn: " + selectedHoaDon.getMaHD());
            return;
        }
        // 2. Tạo Dialog và Layout chính
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết hóa đơn - " + selectedHoaDon.getMaHD());
        dialog.setHeaderText(null);
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setStyle("-fx-background-color: rgba(255, 161, 0, 0.4);"); // Tái sử dụng style thẻ màu
        // 3. Tạo Tiêu đề
        Label title = new Label("CHI TIẾT HÓA ĐƠN " + selectedHoaDon.getMaHD());
        title.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: #BF360C;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        // 4. Tạo GridPane thông tin chung
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(8);
        ColumnConstraints col1=new ColumnConstraints(); col1.setPrefWidth(70); ColumnConstraints col2=new ColumnConstraints(); col2.setPrefWidth(150); col2.setHgrow(Priority.ALWAYS); ColumnConstraints col3=new ColumnConstraints(); col3.setPrefWidth(100); ColumnConstraints col4=new ColumnConstraints(); col4.setPrefWidth(150); col4.setHgrow(Priority.ALWAYS); infoGrid.getColumnConstraints().addAll(col1, col2, col3, col4);
        int rowIndex = 0;
        infoGrid.add(createBoldLabel("Ngày:"), 0, rowIndex); infoGrid.add(new Label(hoaDonFull.getNgayLap() != null ? hoaDonFull.getNgayLap().format(dateFormatter) : "N/A"), 1, rowIndex); infoGrid.add(createBoldLabel("SĐT Khách:"), 2, rowIndex); infoGrid.add(new Label(hoaDonFull.getKhachHang() != null ? hoaDonFull.getKhachHang().getSoDT() : "Khách lẻ"), 3, rowIndex); rowIndex++;
        infoGrid.add(createBoldLabel("Bàn:"), 0, rowIndex); infoGrid.add(new Label(hoaDonFull.getBan() != null ? hoaDonFull.getBan().getMaBan() : "N/A"), 1, rowIndex); infoGrid.add(createBoldLabel("Thu ngân:"), 2, rowIndex); infoGrid.add(new Label(hoaDonFull.getTenNhanVien() != null ? hoaDonFull.getTenNhanVien() : "N/A"), 3, rowIndex); rowIndex++;
        infoGrid.add(createBoldLabel("Giờ vào:"), 0, rowIndex); infoGrid.add(new Label(hoaDonFull.getGioVao() != null ? hoaDonFull.getGioVao().format(timeFormatter) : "N/A"), 1, rowIndex); infoGrid.add(createBoldLabel("Giờ ra:"), 2, rowIndex); infoGrid.add(new Label(hoaDonFull.getGioRa() != null ? hoaDonFull.getGioRa().format(timeFormatter) : "N/A"), 3, rowIndex);
        // 5. Tạo TableView chi tiết món ăn
        TableView<ChiTietHoaDon> detailTable = new TableView<>();
        detailTable.setPrefHeight(150); detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<ChiTietHoaDon, Integer> colCT_STT = new TableColumn<>("STT"); colCT_STT.setPrefWidth(50); colCT_STT.setStyle("-fx-alignment: CENTER;"); colCT_STT.setCellFactory(col -> { TableCell<ChiTietHoaDon, Integer> cell = new TableCell<>(); cell.textProperty().bind(cell.indexProperty().add(1).asString()); return cell; });
        TableColumn<ChiTietHoaDon, String> colCT_TenMon = new TableColumn<>("Tên món"); colCT_TenMon.setPrefWidth(180); colCT_TenMon.setCellValueFactory(new PropertyValueFactory<>("tenMon"));
        TableColumn<ChiTietHoaDon, Integer> colCT_SoLuong = new TableColumn<>("Số lượng"); colCT_SoLuong.setPrefWidth(80); colCT_SoLuong.setStyle("-fx-alignment: CENTER;"); colCT_SoLuong.setCellValueFactory(new PropertyValueFactory<>("soLuong"));
        TableColumn<ChiTietHoaDon, Number> colCT_DonGia = new TableColumn<>("Đơn giá"); colCT_DonGia.setPrefWidth(100); colCT_DonGia.setStyle("-fx-alignment: CENTER-RIGHT;"); colCT_DonGia.setCellValueFactory(new PropertyValueFactory<>("donGia")); colCT_DonGia.setCellFactory(column -> formatCurrencyCellDialog());
        TableColumn<ChiTietHoaDon, Number> colCT_ThanhTien = new TableColumn<>("Thành tiền"); colCT_ThanhTien.setPrefWidth(110); colCT_ThanhTien.setStyle("-fx-alignment: CENTER-RIGHT;"); colCT_ThanhTien.setCellValueFactory(new PropertyValueFactory<>("thanhTien")); colCT_ThanhTien.setCellFactory(column -> formatCurrencyCellDialog());
        detailTable.getColumns().addAll(colCT_STT, colCT_TenMon, colCT_SoLuong, colCT_DonGia, colCT_ThanhTien); detailTable.setItems(FXCollections.observableArrayList(chiTietList)); if(chiTietList.isEmpty()) detailTable.setPlaceholder(new Label("Không có chi tiết món ăn."));
        // 6. Tạo GridPane phần Tổng kết
        GridPane totalsGrid = new GridPane();
        totalsGrid.setHgap(10); totalsGrid.setVgap(3); ColumnConstraints totalCol1 = new ColumnConstraints(); totalCol1.setPrefWidth(380); totalCol1.setHgrow(Priority.ALWAYS); ColumnConstraints totalCol2 = new ColumnConstraints(); totalCol2.setPrefWidth(120); totalCol2.setHalignment(javafx.geometry.HPos.RIGHT); totalsGrid.getColumnConstraints().addAll(totalCol1, totalCol2);
        int totalRowIndex = 0;
        totalsGrid.add(new Label("Tổng cộng món ăn:"), 0, totalRowIndex); totalsGrid.add(new Label(currencyFormatter.format(hoaDonFull.getTongCongMonAn())), 1, totalRowIndex++);
        totalsGrid.add(new Label("Phí dịch vụ (5%):"), 0, totalRowIndex); totalsGrid.add(new Label(currencyFormatter.format(hoaDonFull.getPhiDichVu())), 1, totalRowIndex++);
        totalsGrid.add(new Label("Thuế VAT (8%):"), 0, totalRowIndex); totalsGrid.add(new Label(currencyFormatter.format(hoaDonFull.getThueVAT())), 1, totalRowIndex++);
        totalsGrid.add(new Label("Tiền đặt cọc bàn:"), 0, totalRowIndex); totalsGrid.add(new Label(currencyFormatter.format(hoaDonFull.getTienCoc())), 1, totalRowIndex++);
        totalsGrid.add(new Label("Khuyến mãi:"), 0, totalRowIndex); totalsGrid.add(new Label(currencyFormatter.format(hoaDonFull.getKhuyenMai())), 1, totalRowIndex++);
        Label finalTotalLabel = createBoldLabel("Tổng tiền thanh toán:"); Label finalTotalValue = createBoldLabel(currencyFormatter.format(hoaDonFull.getTongTienThanhToan())); finalTotalValue.setStyle("-fx-text-fill: #BF360C;"); totalsGrid.add(finalTotalLabel, 0, totalRowIndex); totalsGrid.add(finalTotalValue, 1, totalRowIndex);
        // 7. Thêm các phần tử vào Layout chính
        mainLayout.getChildren().addAll(title, infoGrid, detailTable, totalsGrid);
        // 8. Cấu hình và hiển thị Dialog
        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(550);
        // Áp dụng CSS nếu có
        String cssPath = getClass().getResource("/css/ThongKe.css") != null ? getClass().getResource("/css/ThongKe.css").toExternalForm() : null;
        if(cssPath != null) {
            dialog.getDialogPane().getStylesheets().add(cssPath);
            dialog.getDialogPane().setStyle("-fx-background-color: transparent;"); // Xóa nền mặc định của dialog pane
            mainLayout.getStyleClass().add("colored-card"); // Áp dụng style thẻ màu
        } else {
        mainLayout.setStyle("-fx-background-color: #FFF3E0;"); // Màu nền dự phòng
    }
    dialog.showAndWait(); // Hiển thị dialog và chờ đóng
}
private Label createBoldLabel(String text) {
    Label label = new Label(text);
    label.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 13)); // Hoặc size 14 như FXML
    return label;
}
private TableCell<ChiTietHoaDon, Number> formatCurrencyCellDialog() {
    return new TableCell<ChiTietHoaDon, Number>() {
        @Override
        protected void updateItem(Number item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : currencyFormatter.format(item.doubleValue()));
        }
    };
}
private void setupBestSellersTable() {
    tblMonBanChay.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    colSTT.setCellValueFactory(new PropertyValueFactory<>("stt"));
    colTenMon.setCellValueFactory(new PropertyValueFactory<>("tenMon"));
    colSoLuong.setCellValueFactory(new PropertyValueFactory<>("soLuongBan"));
    colDoanhThuMon.setCellValueFactory(new PropertyValueFactory<>("doanhThu"));
    colDoanhThuMon.setCellFactory(column -> new TableCell<MonBanChay, Number>() {
        @Override
        protected void updateItem(Number item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : currencyFormatter.format(item.doubleValue()));
        }
    });
}
private void setupBestSellersPieChart() {
    pieChartMonAn.setLegendVisible(false);
    pieChartMonAn.setLabelsVisible(true);
    pieChartMonAn.setStartAngle(90);
}
private void updateBestSellersPieChart(ObservableList<MonBanChay> data) {
    int limit = Math.min(data.size(), 5);
    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
    for (int i = 0; i < limit; i++) {
        MonBanChay item = data.get(i);
        pieChartData.add(new PieChart.Data(item.getTenMon(), item.getDoanhThu()));
    }
    pieChartMonAn.setData(pieChartData);
}
// --- CÁC HÀM CHO FRAME DOANH THU ---
private void setupDoanhThuNavigation() {
    currentDoanhThuWeekStartDate = getMondayOfWeek(LocalDate.now());
    dpDoanhThuNgay.setValue(LocalDate.now());
    dpDoanhThuNgay.valueProperty().addListener((obs, oldDate, newDate) -> {
        if (newDate != null) {
            LocalDate newWeekStart = getMondayOfWeek(newDate);
            if (!newWeekStart.equals(currentDoanhThuWeekStartDate)) {
                currentDoanhThuWeekStartDate = newWeekStart;
                loadDoanhThuDataForWeek();
            }
        }
    });
    btnDoanhThuHienTai.setOnAction(e -> {
        LocalDate today = LocalDate.now();
        dpDoanhThuNgay.setValue(today);
        LocalDate todayWeekStart = getMondayOfWeek(today);
        currentDoanhThuWeekStartDate = todayWeekStart;
        loadDoanhThuDataForWeek();
    });
    btnDoanhThuTroVe.setOnAction(e -> {
        currentDoanhThuWeekStartDate = currentDoanhThuWeekStartDate.minusWeeks(1);
        dpDoanhThuNgay.setValue(currentDoanhThuWeekStartDate);
        loadDoanhThuDataForWeek();
    });
    btnDoanhThuTiep.setOnAction(e -> {
        currentDoanhThuWeekStartDate = currentDoanhThuWeekStartDate.plusWeeks(1);
        dpDoanhThuNgay.setValue(currentDoanhThuWeekStartDate);
        loadDoanhThuDataForWeek();
    });
}
private void loadDoanhThuDataForWeek() {
    LocalDate startDate = currentDoanhThuWeekStartDate;
    LocalDate endDate = startDate.plusDays(6);
    Response res = Client.sendWithParams(CommandType.GET_DAILY_REVENUE_FOR_WEEK, Map.of("start", startDate.toString(), "end", endDate.toString()));
    if (res.getStatusCode() == 200) {
        Map<String, Double> dailyData = (Map<String, Double>) res.getData();
        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Tổng doanh thu");
        for (int i = 0; i < 7; i++) {
            LocalDate d = startDate.plusDays(i);
            double val = ((Number) dailyData.getOrDefault(d.toString(), 0.0)).doubleValue();
            revenueSeries.getData().add(new XYChart.Data<>(d.format(chartDateFormatter), val));
        }
        chartDoanhThu.getData().clear();
        chartDoanhThu.setAnimated(false);
        chartDoanhThu.getData().add(revenueSeries);
    }
}
// --- CÁC HÀM CHO FRAME KHU VỰC ---
private void setupKhuVucNavigation() {
    currentWeekStartDate = getMondayOfWeek(LocalDate.now());
    dpKhuVucNgay.setValue(LocalDate.now());
    dpKhuVucNgay.valueProperty().addListener((obs, oldDate, newDate) -> {
        if (newDate != null) {
            LocalDate newWeekStart = getMondayOfWeek(newDate);
            if (!newWeekStart.equals(currentWeekStartDate)) {
                currentWeekStartDate = newWeekStart;
                loadKhuVucDataForWeek();
            }
        }
    });
    btnKhuVucHienTai.setOnAction(e -> {
        LocalDate today = LocalDate.now();
        dpKhuVucNgay.setValue(today);
        LocalDate todayWeekStart = getMondayOfWeek(today);
        if (!todayWeekStart.equals(currentWeekStartDate)){
            currentWeekStartDate = todayWeekStart;
            loadKhuVucDataForWeek();
        } else {
        loadKhuVucDataForWeek();
    }
});
btnKhuVucTroVe.setOnAction(e -> {
    currentWeekStartDate = currentWeekStartDate.minusWeeks(1);
    dpKhuVucNgay.setValue(currentWeekStartDate);
    loadKhuVucDataForWeek(); // Gọi trực tiếp
});
btnKhuVucTiep.setOnAction(e -> {
    currentWeekStartDate = currentWeekStartDate.plusWeeks(1);
    dpKhuVucNgay.setValue(currentWeekStartDate);
    loadKhuVucDataForWeek(); // Gọi trực tiếp
});
}
private void loadKhuVucDataForWeek() {
    LocalDate startDate = currentWeekStartDate;
    LocalDate endDate = startDate.plusDays(6);
    Response res = Client.sendWithParams(CommandType.GET_ZONE_REVENUE_FOR_WEEK, Map.of("start", startDate.toString(), "end", endDate.toString()));
    if (res.getStatusCode() == 200) {
        Map<String, Map<String, Double>> zoneDataFromApi = (Map<String, Map<String, Double>>) res.getData();
        chartKhuVuc.getData().clear();
        chartKhuVuc.setAnimated(false);
        String[] zones = {"Tầng trệt", "Tầng 1", "Phòng"};
        for (String z : zones) {
            if (zoneDataFromApi.containsKey(z)) {
                Map<String, Double> dayMap = zoneDataFromApi.get(z);
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(z);
                for (int i = 0; i < 7; i++) {
                    LocalDate d = startDate.plusDays(i);
                    double val = ((Number) dayMap.getOrDefault(d.toString(), 0.0)).doubleValue();
                    series.getData().add(new XYChart.Data<>(d.format(chartDateFormatter), val));
                }
                chartKhuVuc.getData().add(series);
            }
        }
    }
}
private LocalDate getMondayOfWeek(LocalDate date) {
    return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
}
// --- Inner classes ---
public static class HoaDonThongKe {
    private final SimpleStringProperty maHD; private final SimpleStringProperty thoiGianVao; private final SimpleDoubleProperty tongTien;
    public HoaDonThongKe(String maHD, String thoiGianVao, double tongTien) { this.maHD = new SimpleStringProperty(maHD); this.thoiGianVao = new SimpleStringProperty(thoiGianVao); this.tongTien = new SimpleDoubleProperty(tongTien); }
    public String getMaHD() { return maHD.get(); } public String getThoiGianVao() { return thoiGianVao.get(); } public double getTongTien() { return tongTien.get(); }
}
public static class MonBanChay {
    private final SimpleIntegerProperty stt; private final SimpleStringProperty tenMon; private final SimpleIntegerProperty soLuongBan; private final SimpleDoubleProperty doanhThu;
    public MonBanChay(int stt, String tenMon, int soLuongBan, double doanhThu) { this.stt = new SimpleIntegerProperty(stt); this.tenMon = new SimpleStringProperty(tenMon); this.soLuongBan = new SimpleIntegerProperty(soLuongBan); this.doanhThu = new SimpleDoubleProperty(doanhThu); }
    public int getStt() { return stt.get(); } public String getTenMon() { return tenMon.get(); } public int getSoLuongBan() { return soLuongBan.get(); } public double getDoanhThu() { return doanhThu.get(); }
}
// Hàm tiện ích hiển thị Alert
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
}
} // End class ThongKe