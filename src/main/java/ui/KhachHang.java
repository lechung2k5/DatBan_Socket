package ui;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List; // Import List
import java.util.Map;
import java.util.Optional; // Import Optional
import java.time.LocalDateTime;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import java.util.regex.Pattern; // === THAY ĐỔI ===: Thêm import để kiểm tra regex
import entity.TaiKhoan; // Để lấy tên NV
import ui.MainApp;
//=== CÁC IMPORT CHO IN ẤN VÀ PDFBOX ===











import java.io.InputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import javafx.application.Platform;
import network.Client;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.*;
import javafx.beans.property.SimpleObjectProperty; // THÊM IMPORT SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets; // THÊM IMPORT Insets
import javafx.geometry.Pos;
import javafx.scene.control.*; // Import tất cả control
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority; // THÊM IMPORT Priority
import javafx.scene.layout.VBox; // THÊM IMPORT VBox
public class KhachHang {
    // === Khai báo FXML ===
    @FXML private TableView<entity.KhachHang> tblKhachHang;
    @FXML private TableColumn<entity.KhachHang, String> colMaKH;
    @FXML private TableColumn<entity.KhachHang, String> colHoTen;
    @FXML private TableColumn<entity.KhachHang, String> colSDT;
    @FXML private TableColumn<entity.KhachHang, String> colDiaChi;
    @FXML private TableColumn<entity.KhachHang, String> colEmail;
    @FXML private TableColumn<entity.KhachHang, String> colNgayDangKy;
    @FXML private TableColumn<entity.KhachHang, String> colLoaiKH;
    @FXML private TableColumn<entity.KhachHang, Integer> colTongTien; // Kiểu String vì sẽ hiển thị text
    @FXML private TableColumn<entity.KhachHang, Void> colXemLichSu;
    @FXML private TextField txtHoTen, txtSDT, txtDiaChi, txtEmail, txtSearch, txtDiem;
    @FXML private DatePicker datePickerNgayDangKy;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button btnThem, btnXoa, btnSua, btnXoaTrang;
    @FXML private Button btnTim; // Nút tìm kiếm
    // === Thuộc tính khác ===
    // ✅ Đảm bảo các formatter này được khởi tạo đúng và là final
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private ObservableList<entity.KhachHang> masterCustomerList; // Danh sách gốc
    private FilteredList<entity.KhachHang> filteredCustomerList; // Danh sách hiển thị sau lọc
    // === THAY ĐỔI ===: Thêm các biến Regex để kiểm tra
    private static final Pattern PHONE_REGEX = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    // ==================================
    // KHỞI TẠO (INITIALIZE)
    // ==================================
    public void initialize() {
        setupTableColumns();        // Cài đặt các cột bảng
        loadDatabaseData();         // Tải dữ liệu ban đầu
        setupFiltersAndSearch();    // Cài đặt bộ lọc và tìm kiếm
        setupActionButtons();       // Gán sự kiện cho các nút Thêm/Sửa/Xóa
        setupSelectionListener();   // Lắng nghe sự kiện chọn dòng
        clearForm();                // Xóa trắng form ban đầu

        // 🔥 THÊM: Lắng nghe sự kiện Real-time để tự động làm mới danh sách KH
        network.RealTimeClient.getInstance().addListener(this::handleRealTimeEvent);
    }

    private void handleRealTimeEvent(network.RealTimeEvent event) {
        if (event.getType() == network.CommandType.UPDATE_CUSTOMER || 
            event.getType() == network.CommandType.CHECK_OUT || 
            event.getType() == network.CommandType.CREATE_ORDER) {
            javafx.application.Platform.runLater(() -> loadDatabaseData());
        }
    }
    // ==================================
    // CÀI ĐẶT GIAO DIỆN
    // ==================================
    /** Cài đặt bộ lọc ComboBox và sự kiện nút Tìm */
    private void setupFiltersAndSearch() {
        // === THAY ĐỔI: Đã xóa "Guest" khỏi danh sách ===
        filterComboBox.setItems(FXCollections.observableArrayList(
        "Tất cả", "Member", "Gold", "Diamond"
        ));
        // === KẾT THÚC THAY ĐỔI ===
        filterComboBox.setValue("Tất cả"); // Giá trị mặc định
        // Lắng nghe thay đổi ComboBox và ô tìm kiếm, gọi updateFilter
        filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        //txtSearch.textProperty().addListener((obs, oldVal, newVal) -> updateFilter()); // Lọc ngay khi gõ
        // Nút Tìm có thể không cần thiết nếu lọc live, nhưng vẫn giữ lại
        btnTim.setOnAction(e -> updateFilter());
    }
    /** Gán sự kiện cho các nút Thêm, Sửa, Xóa, Xóa trắng */
    private void setupActionButtons() {
        btnThem.setOnAction(e -> handleThem());
        btnSua.setOnAction(e -> handleSua());
        btnXoa.setOnAction(e -> handleXoa());
        btnXoaTrang.setOnAction(e -> {
            clearForm();
            txtHoTen.requestFocus(); // Chỉ focus khi người dùng cố tình bấm nút Xóa trắng
        });
    }
    /** Lắng nghe sự kiện chọn dòng trên TableView */
        private void setupSelectionListener() {
        tblKhachHang.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtHoTen.setText(newSelection.getTenKH());
                txtSDT.setText(newSelection.getSoDT());
                txtDiaChi.setText(newSelection.getDiaChi());
                txtEmail.setText(newSelection.getEmail());
                txtDiem.setText(String.valueOf(newSelection.getDiemTichLuy()));
                datePickerNgayDangKy.setValue(newSelection.getNgayDangKy());
            } else {
                clearForm();
            }
        });
    }

/** Cấu hình các cột cho TableView Khách Hàng */
    private void setupTableColumns() {
        tblKhachHang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        colMaKH.setCellValueFactory(new PropertyValueFactory<>("maKH"));
        colHoTen.setCellValueFactory(new PropertyValueFactory<>("tenKH"));
        colSDT.setCellValueFactory(new PropertyValueFactory<>("soDT"));
        colDiaChi.setCellValueFactory(new PropertyValueFactory<>("diaChi"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colLoaiKH.setCellValueFactory(new PropertyValueFactory<>("thanhVien"));
        colTongTien.setCellValueFactory(new PropertyValueFactory<>("diemTichLuy"));
        colNgayDangKy.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getNgayDangKy();
            String formattedDate = (date != null) ? date.format(dateFormatter) : "";
            return new SimpleStringProperty(formattedDate);
        });
    // Cột Tổng tiền HĐ (Hiện đang ẩn và hiển thị "N/A")
    // Cần getTongTienHoaDon() hoặc tính toán riêng nếu muốn hiển thị giá trị
     // ẩn cột này đi
    // Cột nút Xem Lịch Sử
    colXemLichSu.setCellFactory(param -> new TableCell<entity.KhachHang, Void>() {
        private final Button viewButton = new Button("Xem");
        private final HBox pane = new HBox(viewButton);
        {
            viewButton.getStyleClass().add("view-button");
            pane.setAlignment(Pos.CENTER);
            viewButton.setOnAction(event -> {
                entity.KhachHang customer = getTableView().getItems().get(getIndex());
                if (customer != null && customer.getMaKH() != null) {
                    showInvoiceHistoryDialog(customer); // Gọi hàm hiển thị lịch sử
                } else {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Không thể lấy thông tin khách hàng.");
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
// ==================================
// XỬ LÝ DỮ LIỆU VÀ SỰ KIỆN
// ==================================
/** Tải dữ liệu từ CSDL vào danh sách gốc và áp dụng bộ lọc */
    /** Tải dữ liệu từ CSDL vào danh sách gốc và áp dụng bộ lọc */
    private void loadDatabaseData() {
        try {
            Response res = Client.send(CommandType.GET_CUSTOMERS, null);
            if (res.getStatusCode() == 200) {
                List<entity.KhachHang> list = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), entity.KhachHang.class);
                list.sort((a, b) -> {
                    if (a.getNgayDangKy() == null) return 1;
                    if (b.getNgayDangKy() == null) return -1;
                    return b.getNgayDangKy().compareTo(a.getNgayDangKy());
                });
                if (masterCustomerList == null) {
                    masterCustomerList = FXCollections.observableArrayList(list);
                    filteredCustomerList = new FilteredList<>(masterCustomerList, p -> true);
                    tblKhachHang.setItems(filteredCustomerList);
                } else {
                    masterCustomerList.setAll(list);
                }
                System.out.println("LOG: Đã tìm thấy " + list.size() + " khách hàng.");
            } else {
                if (masterCustomerList == null) masterCustomerList = FXCollections.observableArrayList();
                showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Không thể tải danh sách khách hàng: " + res.getMessage());
            }
        } catch (Exception e) {
            if (masterCustomerList == null) masterCustomerList = FXCollections.observableArrayList();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu", "Đã xảy ra lỗi: " + e.getMessage());
        }
        if (filteredCustomerList != null) updateFilter();
    }
/** Cập nhật bộ lọc dựa trên ComboBox và ô tìm kiếm */
private void updateFilter() {
    String selectedTier = filterComboBox.getValue();
    String searchText = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
    if (filteredCustomerList == null) return;
    filteredCustomerList.setPredicate(customer -> {
        boolean tierMatch = selectedTier == null || selectedTier.equals("Tất cả") ||
        (customer.getThanhVien() != null && customer.getThanhVien().equalsIgnoreCase(selectedTier));
        // Tìm kiếm tương đối theo SĐT hoặc Tên
        boolean searchMatch = searchText.isEmpty() ||
        (customer.getSoDT() != null && customer.getSoDT().toLowerCase().contains(searchText)) ||
        (customer.getTenKH() != null && customer.getTenKH().toLowerCase().contains(searchText));
        return tierMatch && searchMatch;
    });
    // Xử lý việc chọn dòng sau khi lọc
    if (filteredCustomerList.isEmpty()) {
        clearForm();
    } else {
    // Chỉ chọn lại dòng đầu tiên nếu dòng đang chọn không còn trong danh sách lọc
    entity.KhachHang currentSelection = tblKhachHang.getSelectionModel().getSelectedItem();
    if (currentSelection == null || !filteredCustomerList.contains(currentSelection)) {
        tblKhachHang.getSelectionModel().selectFirst();
    }
    // Nếu dòng đang chọn vẫn hợp lệ, không cần làm gì, listener sẽ giữ form được cập nhật
}
}
/** Xử lý nút Thêm */
private void handleThem() {
    String hoTen = txtHoTen.getText().trim();
    String sdt = txtSDT.getText().trim();
    String diaChi = txtDiaChi.getText().trim();
    String email = txtEmail.getText().trim();
    LocalDate ngayDangKy = datePickerNgayDangKy.getValue();
    // === THAY ĐỔI ===: Cập nhật lời gọi hàm, truyền thêm 'diaChi'
    if (!validateInput(hoTen, sdt, diaChi, email, ngayDangKy, null)) {
        return; // Dừng nếu dữ liệu không hợp lệ
    }
    // === KẾT THÚC THAY ĐỔI ===
    String loaiKH = "Member"; // Mặc định khi thêm mới
    int diem = 0;
    try { diem = Integer.parseInt(txtDiem.getText().trim()); } catch(Exception ex){}
    entity.KhachHang newCustomer = new entity.KhachHang("KH" + System.currentTimeMillis(), hoTen, sdt, email, ngayDangKy, diaChi, loaiKH);
    newCustomer.setDiemTichLuy(diem);
    Response res = Client.sendWithParams(CommandType.TIM_HOAC_TAO_KH, Map.of("khachHang", newCustomer));
    if (res.getStatusCode() == 200) {
        masterCustomerList.add(newCustomer);
        tblKhachHang.getSelectionModel().select(newCustomer);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm khách hàng mới.");
    } else {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Thêm khách hàng thất bại: " + res.getMessage());
    }
}
/** Xử lý nút Sửa */
private void handleSua() {
    entity.KhachHang selectedCustomer = tblKhachHang.getSelectionModel().getSelectedItem();
    if (selectedCustomer == null) {
        showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn khách hàng cần sửa.");
        return;
    }
    String hoTen = txtHoTen.getText().trim();
    String sdt = txtSDT.getText().trim();
    String diaChi = txtDiaChi.getText().trim();
    String email = txtEmail.getText().trim();
    LocalDate ngayDangKy = datePickerNgayDangKy.getValue();

    if (!validateInput(hoTen, sdt, diaChi, email, ngayDangKy, selectedCustomer)) {
        return; // Dừng nếu dữ liệu không hợp lệ
    }
    // === KẾT THÚC THAY ĐỔI ===
    // Cập nhật đối tượng trong bộ nhớ
    selectedCustomer.setTenKH(hoTen);
    selectedCustomer.setSoDT(sdt);
    selectedCustomer.setDiaChi(diaChi);
    selectedCustomer.setEmail(email);
    selectedCustomer.setNgayDangKy(ngayDangKy);
    try { selectedCustomer.setDiemTichLuy(Integer.parseInt(txtDiem.getText().trim())); } catch(Exception ex){}
    // Không cho sửa loại KH ở đây (Giữ nguyên loại KH cũ: selectedCustomer.getThanhVien())
    if (Client.sendWithParams(CommandType.UPDATE_CUSTOMER, Map.of("khachHang", selectedCustomer)).getStatusCode() == 200) {
        tblKhachHang.refresh(); // Cập nhật hiển thị dòng đã sửa
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin khách hàng.");
        // Giữ nguyên dòng đang chọn
    } else {
    showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật thất bại.");
}
}
/** Xử lý nút Xóa */
private void handleXoa() {
    entity.KhachHang selectedCustomer = tblKhachHang.getSelectionModel().getSelectedItem();
    if (selectedCustomer == null) {
        showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn khách hàng cần xóa.");
        return;
    }
    // Xác nhận trước khi xóa
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
    "Bạn có chắc chắn muốn xóa khách hàng '" + selectedCustomer.getTenKH() + "' (Mã: " + selectedCustomer.getMaKH() + ") không?",
    ButtonType.YES, ButtonType.NO);
    confirm.setTitle("Xác nhận xóa");
    confirm.setHeaderText(null);
    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.YES) {
        Response res = Client.sendWithParams(CommandType.DELETE_CUSTOMER, Map.of("sdt", selectedCustomer.getSoDT()));
        if (res.getStatusCode() == 200) {
            masterCustomerList.remove(selectedCustomer); // Xóa khỏi danh sách gốc
            clearForm(); // Xóa form sau khi xóa thành công
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa khách hàng.");
        } else {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Xóa thất bại: " + res.getMessage());
    }
}
}
/** Xóa trắng các trường nhập liệu */
private void clearForm() {
    txtHoTen.clear(); txtSDT.clear(); txtDiaChi.clear(); txtEmail.clear(); txtDiem.clear();
    datePickerNgayDangKy.setValue(null);
    tblKhachHang.getSelectionModel().clearSelection(); // Bỏ chọn dòng
}
// === THAY ĐỔI ===: Cập nhật hàm kiểm tra dữ liệu đầu vào
/**
* Kiểm tra tính hợp lệ của dữ liệu đầu vào từ form.
* Hiển thị cảnh báo nếu có lỗi và focus vào trường bị lỗi.
*
* @param hoTen Họ tên khách hàng
* @param sdt Số điện thoại
* @param diaChi Địa chỉ // === THAY ĐỔI ===: Thêm tham số diaChi
* @param email Email
* @param ngayDangKy Ngày đăng ký
* @param customerBeingEdited Khách hàng đang được sửa (null nếu là thêm mới)
* @return true nếu hợp lệ, false nếu có lỗi.
*/
private boolean validateInput(String hoTen, String sdt, String diaChi, String email, LocalDate ngayDangKy, entity.KhachHang customerBeingEdited) {
    // 1. Kiểm tra Họ tên (tenKH ≠ null, ≠ "")
    if (hoTen.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Tên khách hàng không được để trống.");
        txtHoTen.requestFocus();
        return false;
    }
    // 2. Kiểm tra Số điện thoại (10 số, bắt đầu bằng 0)
    if (sdt.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số điện thoại không được để trống.");
        txtSDT.requestFocus();
        return false;
    }
    if (!PHONE_REGEX.matcher(sdt).matches()) {
        showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số điện thoại không hợp lệ. Phải là 10 chữ số và bắt đầu bằng 0.");
        txtSDT.requestFocus();
        return false;
    }
    // 2.5. Kiểm tra trùng lặp Số điện thoại
    final String currentMaKH = (customerBeingEdited != null) ? customerBeingEdited.getMaKH() : null;
    boolean isDuplicate = masterCustomerList.stream()
    .anyMatch(kh -> {
        String existingPhone = (kh.getSoDT() != null) ? kh.getSoDT().trim() : null;
        if (!sdt.equals(existingPhone)) {
            return false;
        }
        if (currentMaKH == null) {
            return true;
        }
        return !kh.getMaKH().equals(currentMaKH);
    });
    if (isDuplicate) {
        showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số điện thoại này đã tồn tại trong hệ thống.");
        txtSDT.requestFocus();
        if (customerBeingEdited == null) {
            Optional<entity.KhachHang> existingCustomer = masterCustomerList.stream()
            .filter(kh -> sdt.equals( (kh.getSoDT() != null) ? kh.getSoDT().trim() : null ))
            .findFirst();
            if (existingCustomer.isPresent()) {
                tblKhachHang.getSelectionModel().select(existingCustomer.get());
                tblKhachHang.scrollTo(existingCustomer.get());
            }
        }
        return false;
    }
    // === THAY ĐỔI: THÊM KIỂM TRA ĐỊA CHỈ ===
    // 3. Kiểm tra Địa chỉ (không rỗng)
    if (diaChi.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Địa chỉ không được để trống.");
        txtDiaChi.requestFocus();
        return false;
    }
    // === KẾT THÚC THAY ĐỔI ===
    // 4. Kiểm tra Email (đúng định dạng chuẩn, có ký tự "@") - (Đã đổi số thứ tự)
    // Email là không bắt buộc (optional), nhưng nếu nhập thì phải đúng định dạng
    if (!email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()) {
        showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Email không đúng định dạng (ví dụ: example@domain.com).");
        txtEmail.requestFocus();
        return false;
    }
    // 5. Kiểm tra Ngày đăng ký (ngayDangKy ≠ null) - (Đã đổi số thứ tự)
    if (ngayDangKy == null) {
        showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng chọn ngày đăng ký.");
        datePickerNgayDangKy.requestFocus();
        return false;
    }
    // Nếu tất cả đều hợp lệ
    return true;
}
// === KẾT THÚC THAY ĐỔI ===
/** Hiển thị Dialog lịch sử hóa đơn */
/**
* Hiển thị Dialog lịch sử hóa đơn CÓ THÊM SỰ KIỆN DOUBLE-CLICK
*/
private void showInvoiceHistoryDialog(entity.KhachHang customer) {
        String id = customer.getMaKH();
        List<HoaDon> ls = new ArrayList<>();
        Response resLS = Client.sendWithParams(CommandType.GET_INVOICES_BY_CUSTOMER, java.util.Map.of("maKH", id));
        if (resLS.getStatusCode() == 200) {
            ls = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resLS.getData()), HoaDon.class);
            ls = ls.stream()
                .filter(hd -> hd.getTrangThai() != null && hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN)
                .collect(java.util.stream.Collectors.toList());
        }

        Dialog<Void> d = new Dialog<>();
        d.setTitle("Khách hàng - " + id);
        VBox main = new VBox(15);
        main.setPadding(new Insets(15));
        main.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 10;");

        // Thông tin khách hàng
        Label t = new Label("THÔNG TIN KHÁCH HÀNG");
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
        t.setMaxWidth(Double.MAX_VALUE);
        t.setAlignment(Pos.CENTER);
        
        GridPane g = new GridPane();
        g.setHgap(15);
        g.setVgap(8);
        int row = 0;
        addInfoRow(g, row++, "Mã:", customer.getMaKH(), "Tên:", customer.getTenKH());
        addInfoRow(g, row++, "SĐT:", customer.getSoDT(), "Email:", customer.getEmail() != null ? customer.getEmail() : "");
        addInfoRow(g, row++, "Địa chỉ:", customer.getDiaChi() != null ? customer.getDiaChi() : "", "Loại:", customer.getThanhVien() != null ? customer.getThanhVien() : "");
        addInfoRow(g, row++, "Ngày ĐK:", customer.getNgayDangKy() != null ? customer.getNgayDangKy().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "", "", "");

        double tot = ls.stream().mapToDouble(HoaDon::getTongTienThanhToan).sum();
        HBox s = new HBox();
        s.setAlignment(Pos.CENTER_RIGHT);
        Label sl = new Label("Tổng chi tiêu: ");
        sl.setStyle("-fx-font-weight: bold;");
        Label sv = new Label(String.format("%,.0f VNĐ", tot));
        sv.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: green;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        s.getChildren().addAll(sl, sp, sv);
        
        main.getChildren().addAll(t, g, s);

        // Lịch sử hóa đơn
        Label ht = new Label("LỊCH SỬ (" + ls.size() + " hóa đơn)");
        ht.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
        
        TableView<HoaDon> tb = new TableView<>();
        tb.setPrefHeight(250);
        tb.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        
        TableColumn<HoaDon, String> c1 = new TableColumn<>("Mã");
        c1.setCellValueFactory(new PropertyValueFactory<>("maHD"));
        c1.setPrefWidth(100);
        
        TableColumn<HoaDon, String> c2 = new TableColumn<>("Ngày");
        c2.setCellValueFactory(cellData -> {
            HoaDon h = cellData.getValue();
            return new SimpleStringProperty(h.getNgayLap() != null ? h.getNgayLap().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        });
        c2.setPrefWidth(150);
        
        TableColumn<HoaDon, String> c3 = new TableColumn<>("PTTT");
        c3.setCellValueFactory(cellData -> {
            PTTThanhToan pt = cellData.getValue().getHinhThucTT();
            return new SimpleStringProperty(pt != null ? pt.getDisplayName() : "N/A");
        });
        
        TableColumn<HoaDon, Double> c4 = new TableColumn<>("Tổng");
        c4.setCellValueFactory(new PropertyValueFactory<>("tongTienThanhToan"));
        c4.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(Double i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? "" : String.format("%,.0f VNĐ", i));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        tb.getColumns().addAll(c1, c2, c3, c4);
        tb.setItems(FXCollections.observableArrayList(ls));
        
        tb.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                HoaDon selected = tb.getSelectionModel().getSelectedItem();
                if (selected != null) showInvoiceDetailDialog(selected.getMaHD());
            }
        });

        main.getChildren().addAll(ht, tb);
        d.getDialogPane().setContent(main);
        d.getDialogPane().setPrefWidth(600);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }
// --- HÀM MỚI ĐỂ HIỂN THỊ CHI TIẾT HÓA ĐƠN ---
/**
* Hiển thị Dialog chi tiết hóa đơn dựa vào mã hóa đơn.
* @param maHD Mã hóa đơn cần hiển thị chi tiết.
*/
/**
* Hiển thị Dialog chi tiết hóa đơn dựa vào mã hóa đơn.
* Đã sửa lỗi định dạng thời gian.
*/
private void showInvoiceDetailDialog(String maHD) {
        Response res = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, java.util.Map.of("maHD", maHD));
        if (res.getStatusCode() != 200) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy hóa đơn: " + maHD);
            return;
        }
        HoaDon hd = utils.JsonUtil.convertValue(res.getData(), HoaDon.class);
        Response resDetails = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, java.util.Map.of("maHD", maHD));
        List<ChiTietHoaDon> ct = new ArrayList<>();
        if (resDetails.getStatusCode() == 200) {
            ct = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resDetails.getData()), ChiTietHoaDon.class);
        }
        hd.setChiTietHoaDon(ct);
        if (hd.getTongCongMonAn() == 0 && !ct.isEmpty()) {
            double totalFood = ct.stream().mapToDouble(item -> item.getDonGia() * item.getSoLuong()).sum();
            hd.setTongCongMonAn(totalFood);
        }

        Dialog<Void> d = new Dialog<>();
        d.setTitle("Chi tiết hóa đơn - " + maHD);
        VBox main = new VBox(15);
        main.setPadding(new Insets(15));
        main.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 10;");

        Label title = new Label("CHI TIẾT HÓA ĐƠN " + maHD);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        GridPane info = new GridPane();
        info.setHgap(15);
        info.setVgap(8);
        int rowIdx = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        
        addInfoRow(info, rowIdx++, "Ngày:", hd.getNgayLap() != null ? hd.getNgayLap().format(fmt) : "",
                "SĐT:", hd.getKhachHang() != null ? hd.getKhachHang().getSoDT() : "");
        addInfoRow(info, rowIdx++, "Bàn:", hd.getMaBan() != null ? hd.getMaBan() : "",
                "Thu ngân:", hd.getTenNhanVien() != null ? hd.getTenNhanVien() : "");
        addInfoRow(info, rowIdx++, "Giờ vào:", hd.getGioVao() != null ? hd.getGioVao().toLocalTime().format(timeFmt) : "",
                "Giờ ra:", hd.getGioRa() != null ? hd.getGioRa().toLocalTime().format(timeFmt) : "");

        TableView<ChiTietHoaDon> tbl = new TableView<>();
        tbl.setPrefHeight(200);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        
        TableColumn<ChiTietHoaDon, Void> c0 = new TableColumn<>("STT");
        c0.setPrefWidth(40);
        c0.setCellFactory(col -> new TableCell<>() {
            @Override
            public void updateIndex(int i) {
                super.updateIndex(i);
                setText(isEmpty() || i < 0 ? null : String.valueOf(i + 1));
            }
        });
        TableColumn<ChiTietHoaDon, String> c1 = new TableColumn<>("Tên món");
        c1.setCellValueFactory(new PropertyValueFactory<>("tenMon"));
        TableColumn<ChiTietHoaDon, Integer> c2 = new TableColumn<>("SL");
        c2.setCellValueFactory(new PropertyValueFactory<>("soLuong"));
        c2.setPrefWidth(50);
        TableColumn<ChiTietHoaDon, Double> c3 = new TableColumn<>("Đơn giá");
        c3.setCellValueFactory(new PropertyValueFactory<>("donGia"));
        c3.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(Double i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? "" : String.format("%,.0f", i));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        TableColumn<ChiTietHoaDon, Double> c4 = new TableColumn<>("Thành tiền");
        c4.setCellValueFactory(new PropertyValueFactory<>("thanhTien"));
        c4.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(Double i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? "" : String.format("%,.0f", i));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        tbl.getColumns().addAll(c0, c1, c2, c3, c4);
        tbl.setItems(FXCollections.observableArrayList(ct));

        VBox sum = new VBox(5);
        sum.setAlignment(Pos.CENTER_RIGHT);
        Label row1 = new Label(String.format("Cộng món ăn: %,.0f VNĐ", hd.getTongCongMonAn()));
        Label row2 = new Label(String.format("Phí DV (5%%): %,.0f VNĐ", hd.getPhiDichVu()));
        Label row3 = new Label(String.format("VAT (8%%): %,.0f VNĐ", hd.getThueVAT()));
        Label row4 = new Label(String.format("Tiền cọc: %,.0f VNĐ", hd.getTienCoc()));
        Label row5 = new Label(String.format("Khuyến mãi: %,.0f VNĐ", hd.getKhuyenMai()));
        
        row1.setStyle("-fx-font-weight: bold;");
        row2.setStyle("-fx-font-weight: bold;");
        row3.setStyle("-fx-font-weight: bold;");
        row4.setStyle("-fx-font-weight: bold;");
        row5.setStyle("-fx-font-weight: bold; -fx-text-fill: #2f9e44;");
        
        sum.getChildren().addAll(row1, row2, row3, row4, row5);

        HBox tot = new HBox();
        tot.setAlignment(Pos.CENTER_RIGHT);
        Label lbl = new Label("TỔNG THANH TOÁN: ");
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label val = new Label(String.format("%,.0f VNĐ", hd.getTongTienThanhToan()));
        val.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e03131;");
        
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        tot.getChildren().addAll(lbl, sp, val);

        main.getChildren().addAll(title, info, tbl, sum, tot);
        d.getDialogPane().setContent(main);
        d.getDialogPane().setPrefWidth(600);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Thêm nút in hóa đơn
        ButtonType btnPrintType = new ButtonType("In hóa đơn", ButtonBar.ButtonData.LEFT);
        d.getDialogPane().getButtonTypes().add(0, btnPrintType);
        Button printButton = (Button) d.getDialogPane().lookupButton(btnPrintType);
        printButton.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold;");
        printButton.setOnAction(e -> {
            handleInHoaDon(hd);
        });

        d.showAndWait();
    }
// ==============================================================================
// 💥 CÁC HÀM XỬ LÝ IN HÓA ĐƠN (COPY TỪ HoaDonUI.java)
// ==============================================================================
// Helper Class cho vị trí Y
private static class YPosition {
    public float y;
    public YPosition(float initialY) { this.y = initialY; }
}
/**
* Xử lý sự kiện nhấn nút In Hóa Đơn
*/
    private void handleInHoaDon(HoaDon hd) {
        if (hd == null) return;
        try {
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
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở bản xem trước hóa đơn: " + e.getMessage());
        }
    }

private void addInfoRow(GridPane grid, int row, String l1, String v1, String l2, String v2) {
    Label lbl1 = new Label(l1); lbl1.getStyleClass().add("info-title");
    Label val1 = new Label(v1); val1.getStyleClass().add("info-value");
    Label lbl2 = new Label(l2); lbl2.getStyleClass().add("info-title");
    Label val2 = new Label(v2); val2.getStyleClass().add("info-value");
    grid.add(lbl1, 0, row); grid.add(val1, 1, row);
    grid.add(lbl2, 2, row); grid.add(val2, 3, row);
}
/** Hàm tiện ích tạo một dòng HBox cho phần tổng tiền */
private HBox createTotalRow(String labelText, String valueText) {
    HBox hbox = new HBox();
    Label label = new Label(labelText); label.getStyleClass().add("info-title");
    Label value = new Label(valueText); value.getStyleClass().add("info-value"); value.setAlignment(Pos.CENTER_RIGHT);
    Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
    hbox.getChildren().addAll(label, spacer, value);
    return hbox;
}
/** Hiển thị Alert đơn giản */
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
    alert.showAndWait();
}
}
