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
import java.util.regex.Pattern; // === THAY ÃÂáÂ»âI ===: ThÃÂªm import ÃâáÂ»Æ kiáÂ»Æm tra regex
import entity.TaiKhoan; // ÃÂáÂ»Æ láÂºÂ¥y tÃÂªn NV
import ui.MainApp;
//=== CÃÂC IMPORT CHO IN áÂºÂ¤N VÃâ¬ PDFBOX ===











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
import javafx.beans.property.SimpleObjectProperty; // THÃÅ M IMPORT SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets; // THÃÅ M IMPORT Insets
import javafx.geometry.Pos;
import javafx.scene.control.*; // Import táÂºÂ¥t cáÂºÂ£ control
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority; // THÃÅ M IMPORT Priority
import javafx.scene.layout.VBox; // THÃÅ M IMPORT VBox
public class KhachHang {
    // === Khai bÃÂ¡o FXML ===
    @FXML private TableView<entity.KhachHang> tblKhachHang;
    @FXML private TableColumn<entity.KhachHang, String> colMaKH;
    @FXML private TableColumn<entity.KhachHang, String> colHoTen;
    @FXML private TableColumn<entity.KhachHang, String> colSDT;
    @FXML private TableColumn<entity.KhachHang, String> colDiaChi;
    @FXML private TableColumn<entity.KhachHang, String> colEmail;
    @FXML private TableColumn<entity.KhachHang, String> colNgayDangKy;
    @FXML private TableColumn<entity.KhachHang, String> colLoaiKH;
    @FXML private TableColumn<entity.KhachHang, String> colTongTien; // KiáÂ»Æu String vÃÂ¬ sáÂºÂ½ hiáÂ»Æn tháÂ»â¹ text
    @FXML private TableColumn<entity.KhachHang, Void> colXemLichSu;
    @FXML private TextField txtHoTen, txtSDT, txtDiaChi, txtEmail, txtSearch;
    @FXML private DatePicker datePickerNgayDangKy;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button btnThem, btnXoa, btnSua, btnXoaTrang;
    @FXML private Button btnTim; // NÃÂºt tÃÂ¬m kiáÂºÂ¿m
    // === ThuáÂ»â¢c tÃÂ­nh khÃÂ¡c ===
    // âÅâ¦ ÃÂáÂºÂ£m báÂºÂ£o cÃÂ¡c formatter nÃÂ y ÃâÃÂ°áÂ»Â£c kháÂ»Å¸i táÂºÂ¡o ÃâÃÂºng vÃÂ  lÃÂ  final
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private ObservableList<entity.KhachHang> masterCustomerList; // Danh sÃÂ¡ch gáÂ»âc
    private FilteredList<entity.KhachHang> filteredCustomerList; // Danh sÃÂ¡ch hiáÂ»Æn tháÂ»â¹ sau láÂ»Âc
    // === THAY ÃÂáÂ»âI ===: ThÃÂªm cÃÂ¡c biáÂºÂ¿n Regex ÃâáÂ»Æ kiáÂ»Æm tra
    private static final Pattern PHONE_REGEX = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    // ==================================
    // KHáÂ»Å¾I TáÂºÂ O (INITIALIZE)
    // ==================================
    public void initialize() {
        setupTableColumns();        // CÃÂ i ÃâáÂºÂ·t cÃÂ¡c cáÂ»â¢t báÂºÂ£ng
        loadDatabaseData();         // TáÂºÂ£i dáÂ»Â¯ liáÂ»â¡u ban ÃâáÂºÂ§u
        setupFiltersAndSearch();    // CÃÂ i ÃâáÂºÂ·t báÂ»â¢ láÂ»Âc vÃÂ  tÃÂ¬m kiáÂºÂ¿m
        setupActionButtons();       // GÃÂ¡n sáÂ»Â± kiáÂ»â¡n cho cÃÂ¡c nÃÂºt ThÃÂªm/SáÂ»Â­a/XÃÂ³a
        setupSelectionListener();   // LáÂºÂ¯ng nghe sáÂ»Â± kiáÂ»â¡n cháÂ»Ân dÃÂ²ng
        clearForm();                // XÃÂ³a tráÂºÂ¯ng form ban ÃâáÂºÂ§u
    }
    // ==================================
    // CÃâ¬I ÃÂáÂºÂ¶T GIAO DIáÂ»â N
    // ==================================
    /** CÃÂ i ÃâáÂºÂ·t báÂ»â¢ láÂ»Âc ComboBox vÃÂ  sáÂ»Â± kiáÂ»â¡n nÃÂºt TÃÂ¬m */
    private void setupFiltersAndSearch() {
        // === THAY ÃÂáÂ»âI: ÃÂÃÂ£ xÃÂ³a "Guest" kháÂ»Âi danh sÃÂ¡ch ===
        filterComboBox.setItems(FXCollections.observableArrayList(
        "TáÂºÂ¥t cáÂºÂ£", "Member", "Gold", "Diamond"
        ));
        // === KáÂºÂ¾T THÃÅ¡C THAY ÃÂáÂ»âI ===
        filterComboBox.setValue("TáÂºÂ¥t cáÂºÂ£"); // GiÃÂ¡ tráÂ»â¹ máÂºÂ·c ÃâáÂ»â¹nh
        // LáÂºÂ¯ng nghe thay ÃâáÂ»â¢i ComboBox vÃÂ  ÃÂ´ tÃÂ¬m kiáÂºÂ¿m, gáÂ»Âi updateFilter
        filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        //txtSearch.textProperty().addListener((obs, oldVal, newVal) -> updateFilter()); // LáÂ»Âc ngay khi gÃÂµ
        // NÃÂºt TÃÂ¬m cÃÂ³ tháÂ»Æ khÃÂ´ng cáÂºÂ§n thiáÂºÂ¿t náÂºÂ¿u láÂ»Âc live, nhÃÂ°ng váÂºÂ«n giáÂ»Â¯ láÂºÂ¡i
        btnTim.setOnAction(e -> updateFilter());
    }
    /** GÃÂ¡n sáÂ»Â± kiáÂ»â¡n cho cÃÂ¡c nÃÂºt ThÃÂªm, SáÂ»Â­a, XÃÂ³a, XÃÂ³a tráÂºÂ¯ng */
    private void setupActionButtons() {
        btnThem.setOnAction(e -> handleThem());
        btnSua.setOnAction(e -> handleSua());
        btnXoa.setOnAction(e -> handleXoa());
        btnXoaTrang.setOnAction(e -> {
            clearForm();
            txtHoTen.requestFocus(); // CháÂ»â° focus khi ngÃÂ°áÂ»Âi dÃÂ¹ng cáÂ»â tÃÂ¬nh báÂºÂ¥m nÃÂºt XÃÂ³a tráÂºÂ¯ng
        });
    }
    /** LáÂºÂ¯ng nghe sáÂ»Â± kiáÂ»â¡n cháÂ»Ân dÃÂ²ng trÃÂªn TableView */
    private void setupSelectionListener() {
        tblKhachHang.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // HiáÂ»Æn tháÂ»â¹ thÃÂ´ng tin lÃÂªn form
                txtHoTen.setText(newSelection.getTenKH());
                txtSDT.setText(newSelection.getSoDT());
                txtDiaChi.setText(newSelection.getDiaChi());
                txtEmail.setText(newSelection.getEmail());
                datePickerNgayDangKy.setValue(newSelection.getNgayDangKy());
            } else {
            clearForm(); // XÃÂ³a form náÂºÂ¿u khÃÂ´ng cÃÂ³ dÃÂ²ng nÃÂ o ÃâÃÂ°áÂ»Â£c cháÂ»Ân
        }
    });
}
/** CáÂºÂ¥u hÃÂ¬nh cÃÂ¡c cáÂ»â¢t cho TableView KhÃÂ¡ch HÃÂ ng */
private void setupTableColumns() {
    tblKhachHang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    // âÅâ¦ SáÂ»Â¬A LáÂ»âI: ÃÂáÂºÂ£m báÂºÂ£o tÃÂªn thuáÂ»â¢c tÃÂ­nh kháÂ»âºp váÂ»âºi getter trong entity.KhachHang
    colMaKH.setCellValueFactory(new PropertyValueFactory<>("maKH")); // CáÂºÂ§n getMaKH()
    colHoTen.setCellValueFactory(new PropertyValueFactory<>("tenKH")); // CáÂºÂ§n getTenKH()
    colSDT.setCellValueFactory(new PropertyValueFactory<>("soDT")); // CáÂºÂ§n getSoDT()
    colDiaChi.setCellValueFactory(new PropertyValueFactory<>("diaChi")); // CáÂºÂ§n getDiaChi()
    colEmail.setCellValueFactory(new PropertyValueFactory<>("email")); // CáÂºÂ§n getEmail()
    colLoaiKH.setCellValueFactory(new PropertyValueFactory<>("thanhVien")); // CáÂºÂ§n getThanhVien()
    // CáÂ»â¢t NgÃÂ y ÃâÃÆng kÃÂ½ (ÃÂáÂ»â¹nh dáÂºÂ¡ng Date) - CáÂºÂ§n getNgayDangKy() tráÂºÂ£ váÂ»Â LocalDate
    colNgayDangKy.setCellValueFactory(cellData -> {
        LocalDate date = cellData.getValue().getNgayDangKy();
        String formattedDate = (date != null) ? date.format(dateFormatter) : "";
        return new SimpleStringProperty(formattedDate);
    });
    // CáÂ»â¢t TáÂ»â¢ng tiáÂ»Ân HÃÂ (HiáÂ»â¡n Ãâang áÂºÂ©n vÃÂ  hiáÂ»Æn tháÂ»â¹ "N/A")
    // CáÂºÂ§n getTongTienHoaDon() hoáÂºÂ·c tÃÂ­nh toÃÂ¡n riÃÂªng náÂºÂ¿u muáÂ»ân hiáÂ»Æn tháÂ»â¹ giÃÂ¡ tráÂ»â¹
    colTongTien.setCellValueFactory(cellData -> new SimpleStringProperty("N/A")); // GiÃÂ¡ tráÂ»â¹ táÂºÂ¡m
    colTongTien.setVisible(false); // áÂºÂ¨n cáÂ»â¢t nÃÂ y Ãâi
    // CáÂ»â¢t nÃÂºt Xem LáÂ»â¹ch SáÂ»Â­
    colXemLichSu.setCellFactory(param -> new TableCell<entity.KhachHang, Void>() {
        private final Button viewButton = new Button("Xem");
        private final HBox pane = new HBox(viewButton);
        {
            viewButton.getStyleClass().add("view-button");
            pane.setAlignment(Pos.CENTER);
            viewButton.setOnAction(event -> {
                entity.KhachHang customer = getTableView().getItems().get(getIndex());
                if (customer != null && customer.getMaKH() != null) {
                    showInvoiceHistoryDialog(customer); // GáÂ»Âi hÃÂ m hiáÂ»Æn tháÂ»â¹ láÂ»â¹ch sáÂ»Â­
                } else {
                showAlert(Alert.AlertType.WARNING, "LáÂ»âi", "KhÃÂ´ng tháÂ»Æ láÂºÂ¥y thÃÂ´ng tin khÃÂ¡ch hÃÂ ng.");
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
// XáÂ»Â¬ LÃÂ DáÂ»Â® LIáÂ»â U VÃâ¬ SáÂ»Â° KIáÂ»â N
// ==================================
/** TáÂºÂ£i dáÂ»Â¯ liáÂ»â¡u táÂ»Â« CSDL vÃÂ o danh sÃÂ¡ch gáÂ»âc vÃÂ  ÃÂ¡p dáÂ»Â¥ng báÂ»â¢ láÂ»Âc */
private void loadDatabaseData() {
    try {
        Response res = Client.send(CommandType.GET_CUSTOMERS, null);
        if (res.getStatusCode() == 200) {
            List<entity.KhachHang> list = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), entity.KhachHang.class);
            masterCustomerList = FXCollections.observableArrayList(list);
        } else {
        masterCustomerList = FXCollections.observableArrayList();
        showAlert(Alert.AlertType.ERROR, "LáÂ»âi dáÂ»Â¯ liáÂ»â¡u", "KhÃÂ´ng tháÂ»Æ táÂºÂ£i danh sÃÂ¡ch khÃÂ¡ch hÃÂ ng: " + res.getMessage());
    }
} catch (Exception e) {
masterCustomerList = FXCollections.observableArrayList();
showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu", "Đã xảy ra lỗi: " + e.getMessage());
}
// Luôn khởi tạo FilteredList dù có lỗi hay không
filteredCustomerList = new FilteredList<>(masterCustomerList, p -> true);
tblKhachHang.setItems(filteredCustomerList);
updateFilter(); // Áp dụng bộ lọc ban đầu (hiển thị tất cả)
    System.out.println("LOG: Đã tìm thấy " + masterCustomerList.size() + " khách hàng.");
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
    entity.KhachHang newCustomer = new entity.KhachHang("KH" + System.currentTimeMillis(), hoTen, sdt, email, ngayDangKy, diaChi, loaiKH);
    Response res = Client.sendWithParams(CommandType.TIM_HOAC_TAO_KH, Map.of("sdt", sdt, "tenKH", hoTen));
    if (res.getStatusCode() == 200) {
        // Cập nhật lại thông tin đầy đủ nếu cần
        Client.sendWithParams(CommandType.UPDATE_CUSTOMER, Map.of("khachHang", newCustomer));
        masterCustomerList.add(newCustomer);
        tblKhachHang.getSelectionModel().select(newCustomer);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm khách hàng mới.");
    }
}
/** Xử lý nút Sửa */
private void handleSua() {
    entity.KhachHang selectedCustomer = tblKhachHang.getSelectionModel().getSelectedItem();
    if (selectedCustomer == null) {
        showAlert(Alert.AlertType.WARNING, "ChÃÂ°a cháÂ»Ân", "Vui lÃÂ²ng cháÂ»Ân khÃÂ¡ch hÃÂ ng cáÂºÂ§n sáÂ»Â­a.");
        return;
    }
    String hoTen = txtHoTen.getText().trim();
    String sdt = txtSDT.getText().trim();
    String diaChi = txtDiaChi.getText().trim();
    String email = txtEmail.getText().trim();
    LocalDate ngayDangKy = datePickerNgayDangKy.getValue();

    if (!validateInput(hoTen, sdt, diaChi, email, ngayDangKy, selectedCustomer)) {
        return; // DáÂ»Â«ng náÂºÂ¿u dáÂ»Â¯ liáÂ»â€¡u khÃƒÂ´ng háÂ»Â£p láÂ»â€¡
    }
    // === KáÂºÂ¾T THÃƒÅ¡C THAY Ã„Â áÂ»â€ I ===
    // Cập nhật đối tượng trong bộ nhớ
    selectedCustomer.setTenKH(hoTen);
    selectedCustomer.setSoDT(sdt);
    selectedCustomer.setDiaChi(diaChi);
    selectedCustomer.setEmail(email);
    selectedCustomer.setNgayDangKy(ngayDangKy);
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
    txtHoTen.clear(); txtSDT.clear(); txtDiaChi.clear(); txtEmail.clear();
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
    Response res = Client.sendWithParams(CommandType.GET_INVOICES_BY_CUSTOMER, Map.of("maKH", customer.getMaKH()));
    List<HoaDon> hoaDonList;
    if (res.getStatusCode() == 200) {
        hoaDonList = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), HoaDon.class);
        hoaDonList = hoaDonList.stream()
        .filter(hd -> hd.getTrangThai() != null && hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN)
        .collect(java.util.stream.Collectors.toList());
    } else {
    hoaDonList = new ArrayList<>();
}
Dialog<Void> dialog = new Dialog<>();
dialog.setTitle("Lịch sử hóa đơn - " + customer.getTenKH());
dialog.setHeaderText("Danh sách hóa đơn của: " + customer.getTenKH() + " (SĐT: " + customer.getSoDT() + ")");
TableView<HoaDon> historyTable = new TableView<>();
historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
TableColumn<HoaDon, String> colHistMaHD = new TableColumn<>("Mã HĐ");
colHistMaHD.setCellValueFactory(new PropertyValueFactory<>("maHD")); colHistMaHD.setPrefWidth(80);
TableColumn<HoaDon, String> colHistNgayLap = new TableColumn<>("Ngày Lập");
colHistNgayLap.setCellValueFactory(cellData -> { LocalDateTime dt = cellData.getValue().getNgayLap(); return new SimpleStringProperty(dt != null ? dt.format(dateTimeFormatter) : "N/A"); });
colHistNgayLap.setPrefWidth(150);
TableColumn<HoaDon, String> colHistPTTT = new TableColumn<>("PTTT");
colHistPTTT.setCellValueFactory(cellData -> { PTTThanhToan pt = cellData.getValue().getHinhThucTT(); return new SimpleStringProperty(pt != null ? pt.getDisplayName() : "Chưa TT"); });
colHistPTTT.setPrefWidth(100);
TableColumn<HoaDon, Double> colHistTongTien = new TableColumn<>("Tổng Tiền");
colHistTongTien.setCellValueFactory(new PropertyValueFactory<>("tongTienThanhToan"));
colHistTongTien.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : String.format("%,.0f Đ", item)); setAlignment(Pos.CENTER_RIGHT); } });
colHistTongTien.setPrefWidth(120);
historyTable.getColumns().setAll(colHistMaHD, colHistNgayLap, colHistPTTT, colHistTongTien);
if (hoaDonList.isEmpty()) { historyTable.setPlaceholder(new Label("Khách hàng chưa có hóa đơn.")); }
else { historyTable.setItems(FXCollections.observableArrayList(hoaDonList)); }
// --- THÊM SỰ KIỆN DOUBLE-CLICK ---
historyTable.setOnMouseClicked(event -> {
    if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
        HoaDon selectedHoaDon = historyTable.getSelectionModel().getSelectedItem();
        if (selectedHoaDon != null && selectedHoaDon.getMaHD() != null) {
            System.out.println("Double clicked on HD: " + selectedHoaDon.getMaHD());
            // Gọi hàm hiển thị chi tiết hóa đơn
            showInvoiceDetailDialog(selectedHoaDon.getMaHD());
        }
    }
});
// --- KẾT THÚC THÊM SỰ KIỆN ---
VBox dialogLayout = new VBox(10, historyTable);
dialogLayout.setPadding(new Insets(10)); VBox.setVgrow(historyTable, Priority.ALWAYS);
dialog.getDialogPane().setContent(dialogLayout);
dialog.getDialogPane().setPrefSize(550, 400);
dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
dialog.showAndWait();
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
    Response resHD = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, Map.of("invoiceId", maHD));
    if (resHD.getStatusCode() != 200) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy hóa đơn " + maHD); return; }
    HoaDon hoaDon = utils.JsonUtil.convertValue(resHD.getData(), HoaDon.class);
    Response resDT = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, Map.of("maHD", maHD));
    List<ChiTietHoaDon> chiTietList;
    if (resDT.getStatusCode() == 200) {
        chiTietList = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(resDT.getData()), ChiTietHoaDon.class);
    } else {
    chiTietList = new ArrayList<>();
}
Dialog<Void> detailDialog = new Dialog<>();
detailDialog.setTitle("Chi tiết hóa đơn - " + maHD);
detailDialog.setHeaderText(null);
// --- Layout chính ---
VBox mainLayout = new VBox(15);
mainLayout.setPadding(new Insets(15));
// Load CSS nếu có
String cssPath = getClass().getResource("/css/HoaDon.css") != null ? getClass().getResource("/css/HoaDon.css").toExternalForm() : null;
if (cssPath != null) {
    detailDialog.getDialogPane().getStylesheets().add(cssPath);
    mainLayout.getStyleClass().add("right-panel");
} else {
mainLayout.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 10;");
}
// --- Tiêu đề ---
Label title = new Label("CHI TIẾT HÓA ĐƠN " + maHD);
title.getStyleClass().add("screen-title");
title.setMaxWidth(Double.MAX_VALUE);
title.setAlignment(Pos.CENTER);
// --- Thông tin chung ---
GridPane infoGrid = new GridPane(); infoGrid.setHgap(10); infoGrid.setVgap(8);
ColumnConstraints col1=new ColumnConstraints(); col1.setHgrow(Priority.NEVER);
ColumnConstraints col2=new ColumnConstraints(); col2.setHgrow(Priority.ALWAYS);
ColumnConstraints col3=new ColumnConstraints(); col3.setHgrow(Priority.NEVER);
ColumnConstraints col4=new ColumnConstraints(); col4.setHgrow(Priority.ALWAYS);
infoGrid.getColumnConstraints().addAll(col1, col2, col3, col4);
int rowIndex = 0;
// Ngày - SĐT
addInfoRow(infoGrid, rowIndex++, "Ngày:", (hoaDon.getNgayLap() != null ? hoaDon.getNgayLap().format(dateFormatter) : "N/A"),
"SĐT Khách:", (hoaDon.getSoDienThoaiKH() != null ? hoaDon.getSoDienThoaiKH() : "N/A"));
// Bàn - Thu ngân
addInfoRow(infoGrid, rowIndex++, "Bàn:", (hoaDon.getMaBan() != null ? hoaDon.getMaBan() : "N/A"),
"Thu ngân:", (hoaDon.getTenNhanVien() != null ? hoaDon.getTenNhanVien() : "N/A"));
// Giờ vào - Giờ ra
addInfoRow(infoGrid, rowIndex++, "Giờ vào:", (hoaDon.getGioVao() != null ? hoaDon.getGioVao().toLocalTime().format(timeFormatter) : "N/A"),
"Giờ ra:", (hoaDon.getGioRa() != null ? hoaDon.getGioRa().toLocalTime().format(timeFormatter) : "N/A"));
// --- Bảng chi tiết món ---
TableView<ChiTietHoaDon> detailTable = new TableView<>();
detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS); detailTable.setPrefHeight(180);
TableColumn<ChiTietHoaDon, Void> colSTT = new TableColumn<>("STT"); colSTT.setPrefWidth(40);
colSTT.setCellFactory(col -> new TableCell<>() { @Override public void updateIndex(int index) { super.updateIndex(index); setText(isEmpty() || index < 0 ? null : Integer.toString(index + 1)); } });
TableColumn<ChiTietHoaDon, String> colTenMon = new TableColumn<>("Tên món"); colTenMon.setCellValueFactory(new PropertyValueFactory<>("tenMon"));
TableColumn<ChiTietHoaDon, Integer> colSoLuong = new TableColumn<>("SL"); colSoLuong.setCellValueFactory(new PropertyValueFactory<>("soLuong")); colSoLuong.setPrefWidth(50);
TableColumn<ChiTietHoaDon, Double> colDonGia = new TableColumn<>("Đơn giá"); colDonGia.setCellValueFactory(new PropertyValueFactory<>("donGia"));
colDonGia.setCellFactory(c -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : String.format("%,.0f", item)); setAlignment(Pos.CENTER_RIGHT); }});
TableColumn<ChiTietHoaDon, Double> colThanhTien = new TableColumn<>("Thành tiền"); colThanhTien.setCellValueFactory(new PropertyValueFactory<>("thanhTien"));
colThanhTien.setCellFactory(c -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : String.format("%,.0f", item)); setAlignment(Pos.CENTER_RIGHT); }});
detailTable.getColumns().setAll(colSTT, colTenMon, colSoLuong, colDonGia, colThanhTien);
detailTable.setItems(FXCollections.observableArrayList(chiTietList));
// --- Tổng tiền ---
VBox totalBox = new VBox(5); totalBox.setPadding(new Insets(5, 0, 5, 0));
double tongMonAn = hoaDon.getTongCongMonAn();
totalBox.getChildren().addAll(
createTotalRow("TáÂ»â¢ng cáÂ»â¢ng mÃÂ³n ÃÆn:", String.format("%,.0f VNÃÂ", tongMonAn)),
createTotalRow("PhÃÂ­ dáÂ»â¹ch váÂ»Â¥ (5%):", String.format("%,.0f VNÃÂ", tongMonAn * 0.05)),
createTotalRow("ThuáÂºÂ¿ VAT (8%):", String.format("%,.0f VNÃÂ", tongMonAn * 0.08)),
createTotalRow("TiáÂ»Ân ÃâáÂºÂ·t cáÂ»Âc bÃÂ n:", String.format("%,.0f VNÃÂ", hoaDon.getTienCoc()))
);
// --- Ã°Å¸âÂ¥ PHáÂºÂ¦N CUáÂ»ÂI: NÃÅ¡T IN + TáÂ»âNG TIáÂ»â¬N ---
HBox finalTotalBox = new HBox(10);
finalTotalBox.setAlignment(Pos.CENTER_RIGHT);
// âÅâ¦ NÃÂºt In HÃÂ³a ÃÂÃÂ¡n (ThÃÂªm máÂ»âºi)
Button btnPrint = new Button("In hÃÂ³a ÃâÃÂ¡n");
btnPrint.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
btnPrint.setOnAction(e -> handleInHoaDon(hoaDon)); // GáÂ»Âi hÃÂ m in
Label finalTotalLabel = new Label("TáÂ»â¢ng tiáÂ»Ân thanh toÃÂ¡n:"); finalTotalLabel.getStyleClass().add("total-title");
Label finalTotalValue = new Label(String.format("%,.0f VNÃÂ", hoaDon.getTongTienThanhToan())); finalTotalValue.getStyleClass().add("total-value");
Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
finalTotalBox.getChildren().addAll(btnPrint, spacer, finalTotalLabel, finalTotalValue);
// --- Add all to layout ---
mainLayout.getChildren().addAll(title, infoGrid, detailTable, totalBox, finalTotalBox);
detailDialog.getDialogPane().setContent(mainLayout);
detailDialog.getDialogPane().setPrefWidth(600);
detailDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
detailDialog.getDialogPane().setStyle("-fx-background-color: transparent;");
detailDialog.showAndWait();
}
// ==============================================================================
// Ã°Å¸âÂ¥ CÃÂC HÃâ¬M XáÂ»Â¬ LÃÂ IN HÃâA ÃÂÃÂ N (COPY TáÂ»Âª HoaDonUI.java)
// ==============================================================================
// Helper Class cho váÂ»â¹ trÃÂ­ Y
private static class YPosition {
    public float y;
    public YPosition(float initialY) { this.y = initialY; }
}
/**
* XáÂ»Â­ lÃÂ½ sáÂ»Â± kiáÂ»â¡n nháÂºÂ¥n nÃÂºt In HÃÂ³a ÃÂÃÂ¡n
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
/** HÃÂ m tiáÂ»â¡n ÃÂ­ch táÂºÂ¡o máÂ»â¢t dÃÂ²ng HBox cho pháÂºÂ§n táÂ»â¢ng tiáÂ»Ân */
private HBox createTotalRow(String labelText, String valueText) {
    HBox hbox = new HBox();
    Label label = new Label(labelText); label.getStyleClass().add("info-title");
    Label value = new Label(valueText); value.getStyleClass().add("info-value"); value.setAlignment(Pos.CENTER_RIGHT);
    Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
    hbox.getChildren().addAll(label, spacer, value);
    return hbox;
}
/** HiáÂ»Æn tháÂ»â¹ Alert ÃâÃ¡n giản */
private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
    alert.showAndWait();
}
}