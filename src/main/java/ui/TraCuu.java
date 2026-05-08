package ui;
import network.Client;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import network.CommandType;
import network.RealTimeClient;
import network.RealTimeEvent;
import network.Response;
import utils.JsonUtil;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import entity.KhachHang;
import entity.TaiKhoan;
import entity.TrangThaiHoaDon;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TraCuu implements network.RealTimeSubscriber {
    private final java.util.function.Consumer<network.RealTimeEvent> rtListener = e -> {
        Platform.runLater(this::loadData);
    };

    @Override
    public java.util.function.Consumer<network.RealTimeEvent> getRealTimeListener() {
        return rtListener;
    }

    @FXML private ComboBox<String> cboSapXepHD, cboSapXepKH;
    @FXML private TextField txtTimKiemHD, txtTimKiemKH;
    @FXML private TableView<HoaDonDisplay> tblHoaDon;
    @FXML private TableColumn<HoaDonDisplay, String> colMaHD, colNgayHD, colHinhThuc, colSDTKH, colTongTien;
    @FXML private TableColumn<HoaDonDisplay, Void> colXemChiTiet;
    @FXML private TableView<KhachHangDisplay> tblKhachHang;
    @FXML private TableColumn<KhachHangDisplay, String> colMaKH, colHoTen, colSDT, colDiaChi, colEmail, colNgayDK, colLoaiKH, colTongTienHD;
    @FXML private TableColumn<KhachHangDisplay, Void> colLichSu;
    private ObservableList<HoaDonDisplay> danhSachHoaDon, tatCaHoaDon;
    private ObservableList<KhachHangDisplay> danhSachKhachHang, tatCaKhachHang;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    @FXML
    public void initialize() {
        tatCaHoaDon = FXCollections.observableArrayList();
        danhSachHoaDon = FXCollections.observableArrayList();
        tatCaKhachHang = FXCollections.observableArrayList();
        danhSachKhachHang = FXCollections.observableArrayList();
        
        // Đăng ký Real-time
        network.RealTimeClient.getInstance().addListener(rtListener);

        // Load data initial
        loadData();
        setupTables();

        Platform.runLater(() -> {
            Scene scene = tblHoaDon.getScene();
            if (scene == null) return;
            scene.setOnKeyPressed(e -> {
                if (e.isControlDown() && !e.isShiftDown() && e.getCode() == KeyCode.H) {
                    tblHoaDon.requestFocus();
                }
                if (e.isControlDown() && !e.isShiftDown() && e.getCode() == KeyCode.K) {
                    tblKhachHang.requestFocus();
                }
                if (e.isControlDown() && !e.isAltDown() && e.getCode() == KeyCode.F) {
                    txtTimKiemHD.requestFocus();
                    txtTimKiemHD.selectAll();
                }
                if (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.F) {
                    txtTimKiemKH.requestFocus();
                    txtTimKiemKH.selectAll();
                }
                if (e.isControlDown() && e.isShiftDown() && e.getCode() == KeyCode.E) {
                    if (tblHoaDon.isFocused()) handleXuatExcelHoaDon();
                    if (tblKhachHang.isFocused()) handleXuatExcelKhachHang();
                }
                if (e.isControlDown() && e.getCode() == KeyCode.P && tblHoaDon.isFocused()) {
                    HoaDonDisplay hd = tblHoaDon.getSelectionModel().getSelectedItem();
                    if (hd != null) showFullDetailHD(hd.getMaHD());
                }
                if (e.getCode() == KeyCode.F5) {
                    loadData();
                }
            });

            // Gán dữ liệu cho ComboBox và listener (Phải nằm trong initialize)
            cboSapXepHD.setItems(FXCollections.observableArrayList("Theo ngày", "Theo tháng", "Theo năm"));
            cboSapXepKH.setItems(FXCollections.observableArrayList("Theo ngày", "Theo tháng", "Theo năm"));
            cboSapXepKH.valueProperty().addListener((o, old, n) -> { if (n != null) sapXepKH(n); });
            cboSapXepHD.valueProperty().addListener((o, old, n) -> { if (n != null) sapXepHD(n); });
            txtTimKiemHD.textProperty().addListener((o, old, n) -> timHD(n));
            txtTimKiemKH.textProperty().addListener((o, old, n) -> timKH(n));

            // 🔥 ĐĂNG KÝ REAL-TIME LISTENER ĐỂ TỰ ĐỘNG REFRESH
            RealTimeClient.getInstance().addListener(this::handleRealTimeEvent);
        });
    }

    private void handleRealTimeEvent(RealTimeEvent event) {
        // Làm mới dữ liệu khi có bất kỳ sự kiện thay đổi hóa đơn, đặt bàn hoặc khách hàng
        if (event.getType() == CommandType.CREATE_ORDER || 
            event.getType() == CommandType.CHECK_OUT ||
            event.getType() == CommandType.UPDATE_INVOICE ||
            event.getType() == CommandType.UPDATE_TABLE_STATUS ||
            event.getType() == CommandType.MERGE_INVOICES ||
            event.getType() == CommandType.SPLIT_INVOICE ||
            event.getType() == CommandType.UPDATE_CUSTOMER) {
            
            System.out.println("[TraCuu] Nhận tín hiệu Real-time, đang làm mới dữ liệu...");
            Platform.runLater(() -> loadData());
        }
    }

    private void setupTables() {
        // Setup bảng Hóa đơn
        colMaHD.setCellValueFactory(c -> c.getValue().maHDProperty());
        colNgayHD.setCellValueFactory(c -> c.getValue().ngayProperty());
        colHinhThuc.setCellValueFactory(c -> c.getValue().hinhThucProperty());
        colSDTKH.setCellValueFactory(c -> c.getValue().sdtKHProperty());
        colTongTien.setCellValueFactory(c -> c.getValue().tongTienProperty());
        colTongTien.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? "" : i);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        colXemChiTiet.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Xem");
            {
                btn.setOnAction(e -> showFullDetailHD(getTableView().getItems().get(getIndex()).getMaHD()));
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
            }
            protected void updateItem(Void i, boolean e) {
                super.updateItem(i, e);
                setGraphic(e ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });
        // Setup bảng Khách hàng
        colMaKH.setCellValueFactory(c -> c.getValue().maKHProperty());
        colHoTen.setCellValueFactory(c -> c.getValue().hoTenProperty());
        colSDT.setCellValueFactory(c -> c.getValue().sdtProperty());
        colDiaChi.setCellValueFactory(c -> c.getValue().diaChiProperty());
        colEmail.setCellValueFactory(c -> c.getValue().emailProperty());
        colNgayDK.setCellValueFactory(c -> c.getValue().ngayDKProperty());
        colLoaiKH.setCellValueFactory(c -> c.getValue().loaiKHProperty());
        colTongTienHD.setCellValueFactory(c -> c.getValue().tongTienHDProperty());
        colTongTienHD.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? "" : i);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        colLichSu.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Xem");
            {
                btn.setOnAction(e -> showFullHistoryKH(getTableView().getItems().get(getIndex()).getMaKH()));
                btn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
            }
            protected void updateItem(Void i, boolean e) {
                super.updateItem(i, e);
                setGraphic(e ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });
        tblHoaDon.setItems(danhSachHoaDon);
        tblKhachHang.setItems(danhSachKhachHang);
    }

    private void loadData() {
        tatCaHoaDon.clear();
        tatCaKhachHang.clear();
        
        List<HoaDon> listHD = new ArrayList<>();
        // 1. Load Hóa Đơn qua API
        Response resHD = Client.send(CommandType.GET_INVOICES_ALL, null);
        if (resHD.getStatusCode() == 200) {
            listHD = JsonUtil.fromJsonList(JsonUtil.toJson(resHD.getData()), HoaDon.class);
            // Sắp xếp mặc định: Mới nhất lên đầu
            listHD.sort((a, b) -> {
                if (a.getNgayLap() == null) return 1;
                if (b.getNgayLap() == null) return -1;
                return b.getNgayLap().compareTo(a.getNgayLap());
            });
            for (HoaDon hd : listHD) {
                if (hd.getTongTienThanhToan() <= 0) {
                    if (hd.getChiTietHoaDon() != null && !hd.getChiTietHoaDon().isEmpty()) {
                        double totalFood = hd.getChiTietHoaDon().stream()
                                .mapToDouble(item -> item.getDonGia() * item.getSoLuong())
                                .sum();
                        hd.setTongCongMonAn(totalFood);
                    }
                    hd.calculateTotals();
                }
                if (hd.getTrangThai() != TrangThaiHoaDon.DA_THANH_TOAN) {
                    continue;
                }
                String sdt = (hd.getKhachHang() != null) ? hd.getKhachHang().getSoDT() : "N/A";
                double tongTien = hd.getTongTienThanhToan();
                tatCaHoaDon.add(new HoaDonDisplay(
                    hd.getMaHD(),
                    hd.getNgayLap() != null ? hd.getNgayLap().format(fmt) : "",
                    hd.getHinhThucTT() != null ? hd.getHinhThucTT().getDisplayName() : "",
                    sdt,
                    String.format("%,.0f VNĐ ", tongTien)
                ));
            }
        }
        danhSachHoaDon.setAll(tatCaHoaDon);

        // 2. Load Khách hàng qua API
        Response resKH = Client.send(CommandType.GET_CUSTOMERS, null);
        if (resKH.getStatusCode() == 200) {
            List<KhachHang> listKH = JsonUtil.fromJsonList(JsonUtil.toJson(resKH.getData()), KhachHang.class);
            listKH.sort((a, b) -> {
                if (a.getNgayDangKy() == null) return 1;
                if (b.getNgayDangKy() == null) return -1;
                return b.getNgayDangKy().compareTo(a.getNgayDangKy());
            });
            
            // 🔥 TỐI ƯU: Tạo map để tính tổng tiền của mỗi khách hàng từ danh sách hóa đơn đã load
            java.util.Map<String, Double> customerTotalMap = new java.util.HashMap<>();
            java.util.Set<String> customersWithPaidInvoices = new java.util.HashSet<>();
            
            for (HoaDon hd : listHD) {
                if (hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN && hd.getKhachHang() != null) {
                    String maKH = hd.getKhachHang().getMaKH();
                    if (maKH != null) {
                        customerTotalMap.put(maKH, customerTotalMap.getOrDefault(maKH, 0.0) + hd.getTongTienThanhToan());
                        customersWithPaidInvoices.add(maKH);
                    }
                }
            }

            for (KhachHang kh : listKH) {
                double total = customerTotalMap.getOrDefault(kh.getMaKH(), 0.0);
                tatCaKhachHang.add(new KhachHangDisplay(
                    kh.getMaKH(),
                    kh.getTenKH(),
                    kh.getSoDT(),
                    kh.getDiaChi() != null ? kh.getDiaChi() : "",
                    kh.getEmail() != null ? kh.getEmail() : "",
                    kh.getNgayDangKy() != null ? kh.getNgayDangKy().format(fmt) : "",
                    kh.getThanhVien() != null ? kh.getThanhVien() : "",
                    String.format("%,.0f VNĐ ", total)
                ));
            }
        }
        danhSachKhachHang.setAll(tatCaKhachHang);
    }

    private void sapXepHD(String k) {
        List<HoaDonDisplay> l = new ArrayList<>(danhSachHoaDon);
        l.sort((a, b) -> {
            String d1 = a.getNgay();
            String d2 = b.getNgay();
            if (d1 == null || d1.isEmpty() || d1.length() < 10) return 1;
            if (d2 == null || d2.isEmpty() || d2.length() < 10) return -1;
            try {
                String[] parts1 = d1.split("/");
                String[] parts2 = d2.split("/");
                if (parts1.length != 3 || parts2.length != 3) {
                    return d1.compareTo(d2);
                }
                int ngay1 = Integer.parseInt(parts1[0]);
                int thang1 = Integer.parseInt(parts1[1]);
                int nam1 = Integer.parseInt(parts1[2]);
                int ngay2 = Integer.parseInt(parts2[0]);
                int thang2 = Integer.parseInt(parts2[1]);
                int nam2 = Integer.parseInt(parts2[2]);
                if (k.equals("Theo năm")) {
                    if (nam1 != nam2) return nam1 - nam2;
                    if (thang1 != thang2) return thang1 - thang2;
                    return ngay1 - ngay2;
                } else if (k.equals("Theo tháng")) {
                    if (nam1 != nam2) return nam1 - nam2;
                    if (thang1 != thang2) return thang1 - thang2;
                    return ngay1 - ngay2;
                } else {
                    if (nam1 != nam2) return nam1 - nam2;
                    if (thang1 != thang2) return thang1 - thang2;
                    return ngay1 - ngay2;
                }
            } catch (Exception e) {
                return d1.compareTo(d2);
            }
        });
        danhSachHoaDon.setAll(l);
    }

    private void sapXepKH(String k) {
        List<KhachHangDisplay> l = new ArrayList<>(danhSachKhachHang);
        l.sort((a, b) -> {
            String d1 = a.getNgayDK();
            String d2 = b.getNgayDK();
            if (d1 == null || d1.isEmpty() || d1.length() < 10) return 1;
            if (d2 == null || d2.isEmpty() || d2.length() < 10) return -1;
            try {
                String[] parts1 = d1.split("/");
                String[] parts2 = d2.split("/");
                if (parts1.length != 3 || parts2.length != 3) {
                    return d1.compareTo(d2);
                }
                int ngay1 = Integer.parseInt(parts1[0]);
                int thang1 = Integer.parseInt(parts1[1]);
                int nam1 = Integer.parseInt(parts1[2]);
                int ngay2 = Integer.parseInt(parts2[0]);
                int thang2 = Integer.parseInt(parts2[1]);
                int nam2 = Integer.parseInt(parts2[2]);
                if (k.equals("Theo năm")) {
                    if (nam1 != nam2) return nam1 - nam2;
                    if (thang1 != thang2) return thang1 - thang2;
                    return ngay1 - ngay2;
                } else if (k.equals("Theo tháng")) {
                    if (nam1 != nam2) return nam1 - nam2;
                    if (thang1 != thang2) return thang1 - thang2;
                    return ngay1 - ngay2;
                } else {
                    if (nam1 != nam2) return nam1 - nam2;
                    if (thang1 != thang2) return thang1 - thang2;
                    return ngay1 - ngay2;
                }
            } catch (Exception e) {
                return d1.compareTo(d2);
            }
        });
        danhSachKhachHang.setAll(l);
    }

    private void timHD(String t) {
        if (t == null || t.trim().isEmpty()) {
            danhSachHoaDon.setAll(tatCaHoaDon);
            return;
        }
        String s = t.toLowerCase().trim();
        danhSachHoaDon.setAll(tatCaHoaDon.stream()
        .filter(h -> {
            boolean matchSDT = h.getSdtKH() != null &&
            !h.getSdtKH().equals("N/A") &&
            h.getSdtKH().toLowerCase().contains(s);
            boolean matchMaHD = h.getMaHD() != null &&
            h.getMaHD().toLowerCase().contains(s);
            return matchSDT || matchMaHD;
        })
        .collect(Collectors.toList()));
    }

    private void timKH(String t) {
        if (t == null || t.trim().isEmpty()) {
            danhSachKhachHang.setAll(tatCaKhachHang);
            return;
        }
        String s = t.toLowerCase().trim();
        danhSachKhachHang.setAll(tatCaKhachHang.stream()
        .filter(k -> {
            boolean matchSDT = k.getSdt() != null &&
            k.getSdt().toLowerCase().contains(s);
            boolean matchHoTen = k.getHoTen() != null &&
            k.getHoTen().toLowerCase().contains(s);
            return matchSDT || matchHoTen;
        })
        .collect(Collectors.toList()));
    }

    private void showFullDetailHD(String id) {
        Response res = Client.sendWithParams(CommandType.GET_INVOICE_BY_ID, java.util.Map.of("maHD", id));
        if (res.getStatusCode() != 200) {
            alert("Lỗi", "Không tìm thấy hóa đơn: " + id);
            return;
        }
        HoaDon hd = JsonUtil.convertValue(res.getData(), HoaDon.class);
        Response resDetails = Client.sendWithParams(CommandType.GET_INVOICE_DETAILS, java.util.Map.of("maHD", id));
        List<ChiTietHoaDon> ct = new ArrayList<>();
        if (resDetails.getStatusCode() == 200) {
            ct = JsonUtil.fromJsonList(JsonUtil.toJson(resDetails.getData()), ChiTietHoaDon.class);
        }
        
        hd.setChiTietHoaDon(ct);

        if (hd.getTongCongMonAn() == 0 && !ct.isEmpty()) {
            double totalFood = ct.stream().mapToDouble(item -> item.getDonGia() * item.getSoLuong()).sum();
            hd.setTongCongMonAn(totalFood);
        }

        Dialog<Void> d = new Dialog<>();
        d.setTitle("Chi tiết hóa đơn - " + id);
        VBox main = new VBox(15);
        main.setPadding(new Insets(15));
        main.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 10;");

        Label title = new Label("CHI TIẾT HÓA ĐƠN " + id);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        GridPane info = new GridPane();
        info.setHgap(15);
        info.setVgap(8);
        int rowIdx = 0;
        addInfo(info, rowIdx++, "Ngày:", hd.getNgayLap() != null ? hd.getNgayLap().format(fmt) : "",
                "SĐT:", hd.getKhachHang() != null ? hd.getKhachHang().getSoDT() : "");
        addInfo(info, rowIdx++, "Bàn:", hd.getMaBan() != null ? hd.getMaBan() : "",
                "Thu ngân:", hd.getTenNhanVien() != null ? hd.getTenNhanVien() : "");
        addInfo(info, rowIdx++, "Giờ vào:", hd.getGioVao() != null ? hd.getGioVao().toLocalTime().format(timeFmt) : "",
                "Giờ ra:", hd.getGioRa() != null ? hd.getGioRa().toLocalTime().format(timeFmt) : "");

        TableView<ChiTietHoaDon> tbl = new TableView<>();
        tbl.setPrefHeight(200);
        
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
        c1.setPrefWidth(180);

        TableColumn<ChiTietHoaDon, Integer> c2 = new TableColumn<>("SL");
        c2.setCellValueFactory(new PropertyValueFactory<>("soLuong"));
        c2.setPrefWidth(50);

        TableColumn<ChiTietHoaDon, Double> c3 = new TableColumn<>("Đơn giá");
        c3.setCellValueFactory(new PropertyValueFactory<>("donGia"));
        c3.setPrefWidth(100);
        c3.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : String.format("%,.0f", i));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        TableColumn<ChiTietHoaDon, Double> c4 = new TableColumn<>("Thành tiền");
        c4.setCellValueFactory(new PropertyValueFactory<>("thanhTien"));
        c4.setPrefWidth(110);
        c4.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : String.format("%,.0f", i));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        tbl.getColumns().addAll(c0, c1, c2, c3, c4);
        tbl.setItems(FXCollections.observableArrayList(ct));

        VBox sum = new VBox(5);
        sum.getChildren().addAll(
                row("Tổng món ăn:", String.format("%,.0f VNĐ", hd.getTongCongMonAn())),
                row("Phí DV (5%):", String.format("%,.0f VNĐ", hd.getPhiDichVu())),
                row("VAT (8%):", String.format("%,.0f VNĐ", hd.getThueVAT())),
                row("Tiền cọc:", String.format("%,.0f VNĐ", hd.getTienCoc())),
                row("Khuyến mãi:", String.format("%,.0f VNĐ", hd.getKhuyenMai()))
        );

        HBox tot = new HBox();
        tot.setAlignment(Pos.CENTER_RIGHT);
        Label lbl = new Label("Tổng thanh toán: ");
        lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label val = new Label(String.format("%,.0f VNĐ", hd.getTongTienThanhToan()));
        val.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e03131;");
        
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        tot.getChildren().addAll(lbl, sp, val);

        main.getChildren().addAll(title, info, tbl, sum, tot);
        d.getDialogPane().setContent(main);
        d.getDialogPane().setPrefWidth(600);

        ButtonType btnPrint = new ButtonType("In hóa đơn", ButtonBar.ButtonData.LEFT);
        d.getDialogPane().getButtonTypes().addAll(btnPrint, ButtonType.CLOSE);

        Button printButton = (Button) d.getDialogPane().lookupButton(btnPrint);
        printButton.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold;");
        printButton.setOnAction(e -> {
            e.consume();
            handleInHoaDon(hd);
        });

        d.show();
    }

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
            alert("Lỗi", "Không thể mở bản xem trước hóa đơn: " + e.getMessage());
        }
    }

    private void showFullHistoryKH(String id) {
        List<HoaDon> ls = new ArrayList<>();
        Response resLS = Client.sendWithParams(CommandType.GET_INVOICES_BY_CUSTOMER, java.util.Map.of("maKH", id));
        if (resLS.getStatusCode() == 200) {
            ls = JsonUtil.fromJsonList(JsonUtil.toJson(resLS.getData()), HoaDon.class);
        }
        // Lấy thông tin KH
        Response resKH = Client.send(CommandType.GET_CUSTOMERS, null);
        KhachHang kh = null;
        if (resKH.getStatusCode() == 200) {
            List<KhachHang> listKH = JsonUtil.fromJsonList(JsonUtil.toJson(resKH.getData()), KhachHang.class);
            kh = listKH.stream().filter(k -> id.equals(k.getMaKH())).findFirst().orElse(null);
        }
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Khách hàng - " + id);
        VBox main = new VBox(15);
        main.setPadding(new Insets(15));
        main.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 10;");
        if (kh != null) {
            Label t = new Label("THÔNG TIN KHÁCH HÀNG");
            t.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
            t.setMaxWidth(Double.MAX_VALUE);
            t.setAlignment(Pos.CENTER);
            GridPane g = new GridPane();
            g.setHgap(15);
            g.setVgap(8);
            int row = 0;
            addInfo(g, row++, "Mã:", kh.getMaKH(), "Tên:", kh.getTenKH());
            addInfo(g, row++, "SĐT:", kh.getSoDT(), "Email:", kh.getEmail() != null ? kh.getEmail() : "");
            addInfo(g, row++, "Địa chỉ:", kh.getDiaChi() != null ? kh.getDiaChi() : "", "Loại:", kh.getThanhVien() != null ? kh.getThanhVien() : "");
            addInfo(g, row++, "Ngày ĐK:", kh.getNgayDangKy() != null ? kh.getNgayDangKy().format(fmt) : "", "", "");
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
        }
        Label ht = new Label("Lịch sử (" + ls.size() + " hóa đơn)");
        ht.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
        TableView<HoaDon> tb = new TableView<>();
        tb.setPrefHeight(250);
        tb.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<HoaDon, String> c1 = new TableColumn<>("Mã");
        c1.setCellValueFactory(new PropertyValueFactory<>("maHD"));
        c1.setPrefWidth(100);
        TableColumn<HoaDon, String> c2 = new TableColumn<>("Ngày");
        c2.setCellValueFactory(cellData -> {
            HoaDon h = cellData.getValue();
            return new SimpleStringProperty(h.getNgayLap() != null ? h.getNgayLap().format(dateTimeFormatter) : "N/A");
        });
        c2.setPrefWidth(150);
        TableColumn<HoaDon, String> c3 = new TableColumn<>("PTTT");
        c3.setCellValueFactory(cellData -> {
            HoaDon h = cellData.getValue();
            return new SimpleStringProperty(h.getHinhThucTT() != null ? h.getHinhThucTT().getDisplayName() : "Chưa TT");
        });
        c3.setPrefWidth(120);
        TableColumn<HoaDon, Double> c4 = new TableColumn<>("Tổng cộng");
        c4.setCellValueFactory(new PropertyValueFactory<>("tongTienThanhToan"));
        c4.setPrefWidth(130);
        c4.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(Double i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : String.format("%,.0f VNĐ", i));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        tb.getColumns().addAll(c1, c2, c3, c4);
        tb.setItems(FXCollections.observableArrayList(ls));
        tb.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                HoaDon selectedHD = tb.getSelectionModel().getSelectedItem();
                if (selectedHD != null && selectedHD.getMaHD() != null) {
                    showFullDetailHD(selectedHD.getMaHD());
                }
            }
        });
        if (ls.isEmpty()) {
            tb.setPlaceholder(new Label("Khách hàng chưa có hóa đơn."));
        }
        main.getChildren().addAll(ht, tb);
        d.getDialogPane().setContent(main);
        d.getDialogPane().setPrefSize(600, 650);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.getDialogPane().setStyle("-fx-background-color: transparent;");
        d.show();
    }

    private void addInfo(GridPane g, int r, String l1, String v1, String l2, String v2) {
        Label lb1 = new Label(l1);
        lb1.setStyle("-fx-font-weight: bold;");
        g.add(lb1, 0, r);
        g.add(new Label(v1), 1, r);
        if (!l2.isEmpty()) {
            Label lb2 = new Label(l2);
            lb2.setStyle("-fx-font-weight: bold;");
            g.add(lb2, 2, r);
            g.add(new Label(v2), 3, r);
        }
    }

    private HBox row(String l, String v) {
        HBox h = new HBox();
        Label lb = new Label(l);
        lb.setStyle("-fx-font-weight: bold;");
        Label vl = new Label(v);
        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);
        h.getChildren().addAll(lb, s, vl);
        return h;
    }

    @FXML
    private void handleXuatExcelHoaDon() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showSaveDialog(tblHoaDon.getScene().getWindow());
        if (f != null) {
            try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(f)) {
                Sheet s = wb.createSheet("Hóa đơn");
                Row h = s.createRow(0);
                String[] cols = {"Mã", "Ngày", "PTTT", "SĐT", "Tổng"};
                for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
                int r = 1;
                for (HoaDonDisplay hd : danhSachHoaDon) {
                    Row rw = s.createRow(r++);
                    rw.createCell(0).setCellValue(hd.getMaHD());
                    rw.createCell(1).setCellValue(hd.getNgay());
                    rw.createCell(2).setCellValue(hd.getHinhThuc());
                    rw.createCell(3).setCellValue(hd.getSdtKH());
                    rw.createCell(4).setCellValue(hd.getTongTien());
                }
                wb.write(out);
                alert("Thành công", "Đã xuất Excel");
            } catch (Exception e) {
                alert("Lỗi", e.getMessage());
            }
        }
    }

    @FXML
    private void handleXuatExcelKhachHang() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showSaveDialog(tblKhachHang.getScene().getWindow());
        if (f != null) {
            try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(f)) {
                Sheet s = wb.createSheet("Khách hàng");
                Row h = s.createRow(0);
                String[] cols = {"Mã", "Tên", "SĐT", "Địa chỉ", "Email", "Ngày", "Loại", "Tổng"};
                for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
                int r = 1;
                for (KhachHangDisplay k : danhSachKhachHang) {
                    Row rw = s.createRow(r++);
                    rw.createCell(0).setCellValue(k.getMaKH());
                    rw.createCell(1).setCellValue(k.getHoTen());
                    rw.createCell(2).setCellValue(k.getSdt());
                    rw.createCell(3).setCellValue(k.getDiaChi());
                    rw.createCell(4).setCellValue(k.getEmail());
                    rw.createCell(5).setCellValue(k.getNgayDK());
                    rw.createCell(6).setCellValue(k.getLoaiKH());
                    rw.createCell(7).setCellValue(k.getTongTienHD());
                }
                wb.write(out);
                alert("Thành công", "Đã xuất Excel");
            } catch (Exception e) {
                alert("Lỗi", e.getMessage());
            }
        }
    }

    private void alert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(c);
        a.showAndWait();
    }

    public static class HoaDonDisplay {
        private final SimpleStringProperty maHD, ngay, hinhThuc, sdtKH, tongTien;
        public HoaDonDisplay(String m, String n, String h, String s, String t) {
            maHD = new SimpleStringProperty(m);
            ngay = new SimpleStringProperty(n);
            hinhThuc = new SimpleStringProperty(h);
            sdtKH = new SimpleStringProperty(s);
            tongTien = new SimpleStringProperty(t);
        }
        public String getMaHD() { return maHD.get(); }
        public SimpleStringProperty maHDProperty() { return maHD; }
        public String getNgay() { return ngay.get(); }
        public SimpleStringProperty ngayProperty() { return ngay; }
        public String getHinhThuc() { return hinhThuc.get(); }
        public SimpleStringProperty hinhThucProperty() { return hinhThuc; }
        public String getSdtKH() { return sdtKH.get(); }
        public SimpleStringProperty sdtKHProperty() { return sdtKH; }
        public String getTongTien() { return tongTien.get(); }
        public SimpleStringProperty tongTienProperty() { return tongTien; }
    }

    public static class KhachHangDisplay {
        private final SimpleStringProperty maKH, hoTen, sdt, diaChi, email, ngayDK, loaiKH, tongTienHD;
        public KhachHangDisplay(String m, String h, String s, String d, String e, String n, String l, String t) {
            maKH = new SimpleStringProperty(m);
            hoTen = new SimpleStringProperty(h);
            sdt = new SimpleStringProperty(s);
            diaChi = new SimpleStringProperty(d);
            email = new SimpleStringProperty(e);
            ngayDK = new SimpleStringProperty(n);
            loaiKH = new SimpleStringProperty(l);
            tongTienHD = new SimpleStringProperty(t);
        }
        public String getMaKH() { return maKH.get(); }
        public SimpleStringProperty maKHProperty() { return maKH; }
        public String getHoTen() { return hoTen.get(); }
        public SimpleStringProperty hoTenProperty() { return hoTen; }
        public String getSdt() { return sdt.get(); }
        public SimpleStringProperty sdtProperty() { return sdt; }
        public String getDiaChi() { return diaChi.get(); }
        public SimpleStringProperty diaChiProperty() { return diaChi; }
        public String getEmail() { return email.get(); }
        public SimpleStringProperty emailProperty() { return email; }
        public String getNgayDK() { return ngayDK.get(); }
        public SimpleStringProperty ngayDKProperty() { return ngayDK; }
        public String getLoaiKH() { return loaiKH.get(); }
        public SimpleStringProperty loaiKHProperty() { return loaiKH; }
        public String getTongTienHD() { return tongTienHD.get(); }
        public SimpleStringProperty tongTienHDProperty() { return tongTienHD; }
    }
}
