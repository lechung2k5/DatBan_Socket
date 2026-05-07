package ui;
import network.Client;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import entity.PTTThanhToan;
// === IMPORT MỚI CHO IN TRỰC TIẾP ===
import entity.TaiKhoan; // << MỚI: Để lấy thông tin NV đăng nhập
import javafx.application.Platform; // << MỚI: Để chạy alert từ thread
 // << MỚI
 // << MỚI
// << MỚI
// << MỚI
// << MỚI: Để lấy thông tin NV đăng nhập
 // << MỚI
 // << MỚI
// << MỚI
 // << MỚI
import java.io.InputStream; // << MỚI
// ======================================
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat; // Giữ lại cho createReceiptPdf
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// === CÁC IMPORT CHO Apache PDFBox (Giữ nguyên) ===





// =====================================
/**
* Lớp Controller cho giao diện Quản lý Hóa Đơn (HoaDon.fxml)
* 🔥 ĐàCẬP NHẬT:
* - Thay thế hàm 'inHoaDonPDF' (lưu file) bằng 'handleInHoaDon' (in trực tiếp).
* - Logic in mới (createReceiptPdf) tự động lấy tên NV đang đăng nhập từ MainApp.
* - Đã xóa các hàm helper PDF cũ không còn sử dụng.
*/
public class HoaDonUI {
    // === CÁC THÀNH PHẦN GIAO DIỆN (FXML) ===
    @FXML private DatePicker datePickerFilter;
    @FXML private Button btnClearDate;
    @FXML private ComboBox<String> comboFilter;
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private TableView<HoaDon> tableHoaDon;
    @FXML private TableColumn<HoaDon, String> colMaHD;
    @FXML private TableColumn<HoaDon, String> colNgay;
    @FXML private TableColumn<HoaDon, PTTThanhToan> colHinhThucThanhToan;
    @FXML private TableColumn<HoaDon, String> colSoDienThoaiKH;
    @FXML private TableColumn<HoaDon, Double> colTongTien;
    @FXML private TableColumn<HoaDon, Void> colXemChiTiet;
    @FXML private Button btnXuatExcel;
    @FXML private Button btnInHoaDon;
    @FXML private TableView<ChiTietHoaDon> tableChiTietHD;
    // Các Label hiển thị thông tin chi tiết
    @FXML private Label lblMaHDValue;
    @FXML private Label lblMaKHValue;
    @FXML private Label lblNgayValue;
    @FXML private Label lblThuNganValue;
    @FXML private Label lblBanValue;
    @FXML private Label lblGioVaoValue;
    @FXML private Label lblGioRaValue;
    @FXML private Label lblTongCongMonAnValue;
    @FXML private Label lblPhiDichVuValue;
    @FXML private Label lblThueVATValue;
    @FXML private Label lblTienDatCocValue;
    @FXML private Label lblKhuyenMaiValue;
    @FXML private Label lblTongTienThanhToanValue;
    // === BIẾN QUẢN LÝ DỮ LIỆU ===
    // private HoaDonDAO hoaDonDAO; // Removed
    private ObservableList<HoaDon> allHoaDonList;
    private ObservableList<HoaDon> danhSachHoaDon;
    private ObservableList<ChiTietHoaDon> danhSachChiTietHD;
    // Định dạng ngày giờ
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    // Hằng số cho ComboBox
    private final String ALL_METHODS = "Tất cả PT";
    private final String VI_DIEN_TU = PTTThanhToan.VI_DIEN_TU.getDisplayName();
    private final String NGAN_HANG = PTTThanhToan.NGAN_HANG.getDisplayName();
    private final String TIEN_MAT = PTTThanhToan.TIEN_MAT.getDisplayName();
    // === FONT CHO PDF (Đã xóa FONT_PATH cũ) ===
    // (Font sẽ được load bên trong createReceiptPdf)
    @FXML
    private void initialize() {
        // hoaDonDAO = new HoaDonDAO(); // Removed
        allHoaDonList = FXCollections.observableArrayList();
        danhSachHoaDon = FXCollections.observableArrayList();
        danhSachChiTietHD = FXCollections.observableArrayList();
        setupTableHoaDonColumns();
        setupTableChiTietHoaDon();
        comboFilter.getItems().addAll(ALL_METHODS, VI_DIEN_TU, NGAN_HANG, TIEN_MAT);
        comboFilter.setValue(ALL_METHODS);
        datePickerFilter.setOnAction(event -> filterData());
        btnClearDate.setOnAction(event -> {
            datePickerFilter.setValue(null);
            filterData();
        });
        comboFilter.setOnAction(event -> filterData());
        btnSearch.setOnAction(event -> filterData());
        txtSearch.setOnAction(event -> filterData());
        loadAndFilterData();
        btnXuatExcel.setOnAction(e -> xuatExcel());
        // 🔥 THAY ĐỔI LOGIC NÚT IN
        btnInHoaDon.setOnAction(e -> {
            HoaDon selectedHoaDon = tableHoaDon.getSelectionModel().getSelectedItem();
            if (selectedHoaDon == null) {
                showAlert("Vui lòng chọn một hóa đơn để in.");
                return;
            }
            // Gọi hàm in trực tiếp (lấy từ TraCuu.java)
            handleInHoaDon(selectedHoaDon);
        });
        tableHoaDon.setItems(danhSachHoaDon);
        tableHoaDon.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                hienThiThongTinHoaDon(newSelection);
            } else {
            hienThiThongTinHoaDon(null);
        }
    });
}
// ... (Các hàm setupTableHoaDonColumns, loadAndFilterData, filterData, setupTableChiTietHoaDon, hienThiThongTinHoaDon giữ nguyên) ...
private void setupTableHoaDonColumns() {
    colMaHD.setCellValueFactory(new PropertyValueFactory<>("maHD"));
    colNgay.setCellValueFactory(cellData -> {
        LocalDateTime ngayLap = cellData.getValue().getNgayLap();
        String formattedDate = (ngayLap != null) ? ngayLap.format(dateFormatter) : "N/A";
        return new javafx.beans.property.SimpleStringProperty(formattedDate);
    });
    colHinhThucThanhToan.setCellValueFactory(new PropertyValueFactory<>("hinhThucTT"));
    colHinhThucThanhToan.setCellFactory(column -> new TableCell<HoaDon, PTTThanhToan>() {
        @Override
        protected void updateItem(PTTThanhToan item, boolean empty) {
            super.updateItem(item, empty);
            setText( (empty || item == null) ? null : item.getDisplayName() );
        }
    });
    colSoDienThoaiKH.setCellValueFactory(new PropertyValueFactory<>("soDienThoaiKH"));
    colTongTien.setCellValueFactory(new PropertyValueFactory<>("tongTienThanhToan"));
    colTongTien.setCellFactory(column -> new TableCell<>() {
        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : String.format("%,.0f VNĐ", item));
        }
    });
    colXemChiTiet.setCellFactory(param -> new TableCell<>() {
        private final Button btnXem = new Button("Xem");
        private final HBox pane = new HBox(btnXem);
        {
            pane.setAlignment(Pos.CENTER);
            btnXem.getStyleClass().add("view-button");
            btnXem.setOnAction(event -> {
                HoaDon hoaDon = getTableView().getItems().get(getIndex());
                if (hoaDon != null) {
                    hienThiThongTinHoaDon(hoaDon);
                }
            });
        }
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    });
}
private void loadAndFilterData() {
    Response res = Client.send(CommandType.GET_INVOICES_ALL, null);
    if (res.getStatusCode() == 200) {
        List<HoaDon> list = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), HoaDon.class);
        allHoaDonList.setAll(list);
    }
    datePickerFilter.setValue(null); // << Reset DatePicker khi tải lại
    filterData();
    if (!danhSachHoaDon.isEmpty()) {
        tableHoaDon.getSelectionModel().selectFirst();
    } else {
    hienThiThongTinHoaDon(null);
}
}
private void filterData() {
    String selectedPaymentMethodDisplay = comboFilter.getValue();
    String searchText = txtSearch.getText().toLowerCase().trim();
    LocalDate selectedDate = datePickerFilter.getValue(); // << Lấy giá trị từ DatePicker
    List<HoaDon> filteredList = allHoaDonList.stream()
    .filter(hd -> {
        boolean paymentMatch = ALL_METHODS.equals(selectedPaymentMethodDisplay) ||
        (hd.getHinhThucTT() != null &&
        selectedPaymentMethodDisplay.equalsIgnoreCase(hd.getHinhThucTT().getDisplayName()));
        boolean searchMatch = searchText.isEmpty() ||
        (hd.getSoDienThoaiKH() != null &&
        hd.getSoDienThoaiKH().toLowerCase().contains(searchText));
        // << Logic lọc theo ngày >>
        boolean dateMatch = (selectedDate == null) ||
        (hd.getNgayLap() != null &&
        hd.getNgayLap().toLocalDate().equals(selectedDate));
        return paymentMatch && searchMatch && dateMatch ; // << Thêm dateMatch vào điều kiện
    })
    .collect(Collectors.toList());
    danhSachHoaDon.setAll(filteredList);
    if (danhSachHoaDon.isEmpty()) {
        hienThiThongTinHoaDon(null);
    } else {
    // Cố gắng giữ lại lựa chọn cũ nếu có thể, nếu không thì chọn dòng đầu
    HoaDon selected = tableHoaDon.getSelectionModel().getSelectedItem();
    if (selected != null && danhSachHoaDon.contains(selected)) {
        tableHoaDon.getSelectionModel().select(selected);
    } else {
    tableHoaDon.getSelectionModel().selectFirst();
}
}
}
private void setupTableChiTietHoaDon() {
    tableChiTietHD.getColumns().clear();
    TableColumn<ChiTietHoaDon, Void> colSTT = new TableColumn<>("STT");
    colSTT.setPrefWidth(40); colSTT.setSortable(false);
    colSTT.setCellFactory(col -> new TableCell<>() {
        @Override public void updateIndex(int index) { super.updateIndex(index); setText(isEmpty() || index < 0 ? null : Integer.toString(index + 1)); }
    });
    TableColumn<ChiTietHoaDon, String> colTenMon = new TableColumn<>("Tên món");
    colTenMon.setCellValueFactory(new PropertyValueFactory<>("tenMon")); colTenMon.setPrefWidth(150);
    TableColumn<ChiTietHoaDon, Integer> colSoLuong = new TableColumn<>("Số lượng");
    colSoLuong.setCellValueFactory(new PropertyValueFactory<>("soLuong")); colSoLuong.setPrefWidth(70);
    TableColumn<ChiTietHoaDon, Double> colDonGia = new TableColumn<>("Đơn giá");
    colDonGia.setCellValueFactory(new PropertyValueFactory<>("donGia")); colDonGia.setPrefWidth(90);
    colDonGia.setCellFactory(column -> new TableCell<>() {
        @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : String.format("%,.0f", item)); }
    });
    TableColumn<ChiTietHoaDon, Double> colThanhTien = new TableColumn<>("Thành tiền");
    colThanhTien.setCellValueFactory(new PropertyValueFactory<>("thanhTien")); colThanhTien.setPrefWidth(100);
    colThanhTien.setCellFactory(column -> new TableCell<>() {
        @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : String.format("%,.0f", item)); }
    });
    @SuppressWarnings("unchecked")
    TableColumn<ChiTietHoaDon, ?>[] columnsArr = new TableColumn[] {colSTT, colTenMon, colSoLuong, colDonGia, colThanhTien};
    tableChiTietHD.getColumns().addAll(columnsArr);
    tableChiTietHD.setItems(danhSachChiTietHD);
}
private void hienThiThongTinHoaDon(HoaDon hoaDon) {
    if (hoaDon == null) {
        lblMaHDValue.setText("..."); lblMaKHValue.setText("..."); lblNgayValue.setText("...");
        lblThuNganValue.setText("..."); lblBanValue.setText("..."); lblGioVaoValue.setText("...");
        lblGioRaValue.setText("..."); lblTongCongMonAnValue.setText("0 VNĐ"); lblPhiDichVuValue.setText("0 VNĐ");
        lblThueVATValue.setText("0 VNĐ"); lblTienDatCocValue.setText("0 VNĐ"); lblKhuyenMaiValue.setText("0 VNĐ");
        lblTongTienThanhToanValue.setText("0 VNĐ");
        danhSachChiTietHD.clear();
        return;
    }
    lblMaHDValue.setText(hoaDon.getMaHD());
    lblMaKHValue.setText(hoaDon.getSoDienThoaiKH() != null ? hoaDon.getSoDienThoaiKH() : "N/A");
    lblNgayValue.setText(hoaDon.getNgayLap() != null ? hoaDon.getNgayLap().format(dateFormatter) : "N/A");
    lblThuNganValue.setText(hoaDon.getTenNhanVien() != null ? hoaDon.getTenNhanVien() : "N/A");
    lblBanValue.setText(hoaDon.getMaBan() != null ? hoaDon.getMaBan() : "N/A");
    lblGioVaoValue.setText(hoaDon.getGioVao() != null ? hoaDon.getGioVao().format(timeFormatter) : "N/A");
    lblGioRaValue.setText(hoaDon.getGioRa() != null ? hoaDon.getGioRa().format(timeFormatter) : "N/A");
    lblTongCongMonAnValue.setText(String.format("%,.0f VNĐ", hoaDon.getTongCongMonAn()));
    lblPhiDichVuValue.setText(String.format("%,.0f VNĐ", hoaDon.getPhiDichVu()));
    lblThueVATValue.setText(String.format("%,.0f VNĐ", hoaDon.getThueVAT()));
    lblTienDatCocValue.setText(String.format("%,.0f VNĐ", hoaDon.getTienCoc()));
    lblKhuyenMaiValue.setText(String.format("%,.0f VNĐ", hoaDon.getKhuyenMai()));
    lblTongTienThanhToanValue.setText(String.format("%,.0f VNĐ", hoaDon.getTongTienThanhToan()));
    Response res = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, java.util.Map.of("invoiceId", hoaDon.getMaHD()));
    if (res.getStatusCode() == 200) {
        List<ChiTietHoaDon> chiTietList = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), ChiTietHoaDon.class);
        danhSachChiTietHD.setAll(chiTietList);
    }
}
// ... (Hàm xuatExcel và các hàm helper của nó (createHeaderStyle, createCurrencyStyle, v.v.) giữ nguyên) ...
private void xuatExcel() {
    ObservableList<HoaDon> dataToExport = tableHoaDon.getItems();
    if (dataToExport == null || dataToExport.isEmpty()) {
        showAlert("Không có dữ liệu hóa đơn để xuất.");
        return;
    }
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Lưu file Excel");
    fileChooser.setInitialFileName("DanhSachHoaDon_" + LocalDate.now() + ".xlsx");
    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx");
    fileChooser.getExtensionFilters().add(extFilter);
    Stage stage = (Stage) tableHoaDon.getScene().getWindow();
    File file = fileChooser.showSaveDialog(stage);
    if (file != null) {
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet("DanhSachHoaDon");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateTimeStyle = createDateTimeStyle(workbook);
            CellStyle basicStyle = createBasicCellStyle(workbook);
            String[] columns = {"Mã HĐ", "Ngày Lập", "PT Thanh Toán", "Trạng Thái", "SĐT Khách", "Thu Ngân", "Bàn", "Giờ Vào", "Giờ Ra", "Tiền Cọc", "Tổng Tiền Món", "Tổng Thanh Toán"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowNum = 1;
            for (HoaDon hd : dataToExport) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, hd.getMaHD(), basicStyle);
                createCell(row, 1, hd.getNgayLap(), dateTimeStyle);
                createCell(row, 2, (hd.getHinhThucTT() != null ? hd.getHinhThucTT().getDisplayName() : ""), basicStyle);
                createCell(row, 3, (hd.getTrangThai() != null ? hd.getTrangThai().getDisplayName() : ""), basicStyle);
                createCell(row, 4, hd.getSoDienThoaiKH(), basicStyle);
                createCell(row, 5, hd.getTenNhanVien(), basicStyle);
                createCell(row, 6, hd.getMaBan(), basicStyle);
                createCell(row, 7, hd.getGioVao(), dateTimeStyle);
                createCell(row, 8, hd.getGioRa(), dateTimeStyle);
                createCell(row, 9, hd.getTienCoc(), currencyStyle);
                createCell(row, 10, hd.getTongCongMonAn(), currencyStyle);
                createCell(row, 11, hd.getTongTienThanhToan(), currencyStyle);
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(fileOut);
            showAlert("Xuất file Excel thành công!\nĐã lưu tại: " + file.getAbsolutePath());
        } catch (IOException e) {
        showAlert("Lỗi khi ghi file Excel: " + e.getMessage());
        e.printStackTrace();
    } catch (Exception e) {
    showAlert("Đã xảy ra lỗi không mong muốn: " + e.getMessage());
    e.printStackTrace();
}
} else {
System.out.println("Hủy thao tác lưu file Excel.");
}
}
private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont(); font.setBold(true); font.setFontHeightInPoints((short) 12); style.setFont(font);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER); style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setBorderBottom(BorderStyle.THIN); style.setBorderTop(BorderStyle.THIN); style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN);
    return style;
}
private CellStyle createCurrencyStyle(Workbook workbook) {
    CellStyle style = createBasicCellStyle(workbook);
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("#,##0\" VNĐ\""));
    return style;
}
private CellStyle createDateTimeStyle(Workbook workbook) {
    CellStyle style = createBasicCellStyle(workbook);
    CreationHelper createHelper = workbook.getCreationHelper();
    style.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));
    return style;
}
private CellStyle createBasicCellStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setBorderBottom(BorderStyle.THIN); style.setBorderTop(BorderStyle.THIN); style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
}
private void createCell(Row row, int column, Object value, CellStyle style) {
    Cell cell = row.createCell(column);
    if (value instanceof String) {
        cell.setCellValue((String) value);
    } else if (value instanceof Double) {
    cell.setCellValue((Double) value);
} else if (value instanceof Integer) {
cell.setCellValue((Integer) value);
} else if (value instanceof LocalDateTime) {
cell.setCellValue((LocalDateTime) value);
} else if (value instanceof LocalDate) {
cell.setCellValue((LocalDate) value);
} else if (value == null) {
cell.setBlank();
}
if (style != null) {
    cell.setCellStyle(style);
}
}
// === CÁC HÀM TIỆN ÍCH CHO PDFBox (ĐàXÓA) ===
// ... (Đã xóa các hàm drawTextLeft, drawTextRight, drawLine, v.v. cũ) ...
// === HÀM IN HÓA ĐƠN (PDF) - (ĐàXÓA HÀM CŨ) ===
// === 🔥 HÀM MỚI: HELPER CLASS CHO VỊ TRÍ Y (Copy từ TraCuu) ===
private static class YPosition {
    public float y;
    public YPosition(float initialY) {
        this.y = initialY;
    }
}
// === 🔥 HÀM MỚI: XỬ LÝ IN (Copy từ TraCuu) ===
    private void handleInHoaDon(HoaDon hd) {
        if (hd == null) return;
        try {
            // Đảm bảo có chi tiết hóa đơn
            if (hd.getChiTietHoaDon() == null || hd.getChiTietHoaDon().isEmpty()) {
                Response res = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, java.util.Map.of("invoiceId", hd.getMaHD()));
                if (res.getStatusCode() == 200) {
                    List<ChiTietHoaDon> chiTietList = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), ChiTietHoaDon.class);
                    hd.setChiTietHoaDon(chiTietList);
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HoaDonPreview_Popup.fxml"));
            Parent root = loader.load();
            
            HoaDonPreviewController controller = loader.getController();
            controller.setHoaDon(hd);
            
            Stage stage = new Stage();
            stage.setTitle("In Hóa Đơn - " + hd.getMaHD());
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở bản xem trước hóa đơn: " + e.getMessage());
        }
    }

private void showAlert(String message) {
    Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    });
}
// === HÀM HIỂN THỊ THÔNG BÁO LỖI CÓ TIÊU ĐỀ RIÊNG ===
private void showAlert(String title, String message) {
    Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    });
}
}