package ui;

import network.Client;
import network.CommandType;
import network.Response;
import entity.MonAn;
import entity.DanhMucMon;
import utils.CloudinaryUtil;
import utils.JsonUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * ThucDon - Quản lý thực đơn (Admin/Manager)
 * Hỗ trợ Cloudinary Image Upload
 */
public class ThucDon {
    @FXML private TableView<MenuItem> tableThucDon;
    @FXML private TableColumn<MenuItem, String> colTenMon;
    @FXML private TableColumn<MenuItem, Image> colHinhAnh;
    @FXML private TableColumn<MenuItem, Number> colDonGia;
    @FXML private TableColumn<MenuItem, String> colDanhMuc;

    @FXML private TextField txtTenMon, txtDonGia, txtSearch;
    @FXML private Label uploadLabel;
    @FXML private ImageView previewImg;
    @FXML private ComboBox<DanhMucMon> comboDanhMuc;
    
    @FXML private Button btnThem, btnXoa, btnSua, btnXoaTrang;
    @FXML private Button btnLuuForm;
    @FXML private Button btnTim;
    @FXML private HBox categoryButtonBox;
    @FXML private AnchorPane formInputArea;

    private ObservableList<MenuItem> dsMonAnUI;
    private List<DanhMucMon> dsDanhMuc = new ArrayList<>();
    private File selectedImageFile;
    private String currentHinhAnhUrl;

    public void initialize() {
        dsMonAnUI = FXCollections.observableArrayList();
        tableThucDon.setItems(dsMonAnUI);
        setupTableColumns();
        
        loadCategories();
        
        clearForm();
        formInputArea.setVisible(false);
        btnLuuForm.setText("Lưu");

        // 🔥 LẮNG NGHE REAL-TIME (Tải lại thực đơn khi có máy khác cập nhật)
        network.RealTimeClient.getInstance().addListener(event -> {
            if (event.getType() == CommandType.UPDATE_MENU) {
                Platform.runLater(() -> {
                    // Lấy lại danh mục đang chọn (nếu có) hoặc tải mặc định
                    DanhMucMon current = comboDanhMuc.getValue();
                    if (current != null) {
                        filterTableData(current.getMaDM());
                    } else if (!dsDanhMuc.isEmpty()) {
                        filterTableData(dsDanhMuc.get(0).getMaDM());
                    }
                });
            }
        });

        // Event handlers
        uploadLabel.setOnMouseClicked(e -> onUploadClicked());
        btnThem.setOnAction(e -> toggleForm(true));
        btnXoa.setOnAction(e -> handleXoaMonAn());
        btnSua.setOnAction(e -> {
            if (tableThucDon.getSelectionModel().getSelectedItem() != null) {
                toggleForm(true);
            } else {
                showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một món ăn để sửa.");
            }
        });
        btnXoaTrang.setOnAction(e -> clearForm());
        btnLuuForm.setOnAction(e -> handleLuuForm());
        btnTim.setOnAction(e -> handleSearch(txtSearch.getText()));

        // Shortcuts
        setupShortcuts();

        // Table Selection Listener
        tableThucDon.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillForm(newSelection);
            }
        });
    }

    private void loadCategories() {
        Response res = Client.send(CommandType.GET_MENU_CATEGORIES, null);
        if (res.getStatusCode() == 200) {
            dsDanhMuc = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), DanhMucMon.class);
            
            // Cập nhật ComboBox
            if (comboDanhMuc != null) {
                comboDanhMuc.setItems(FXCollections.observableArrayList(dsDanhMuc));
            }
            
            // Cập nhật sidebar buttons (nếu có)
            updateCategoryButtons();
            
            // Load mặc định category đầu tiên
            if (!dsDanhMuc.isEmpty()) {
                filterTableData(dsDanhMuc.get(0).getMaDM());
            }
        }
    }

    private void updateCategoryButtons() {
        if (categoryButtonBox == null) return;
        categoryButtonBox.getChildren().clear();
        
        for (DanhMucMon dm : dsDanhMuc) {
            Button btn = new Button(dm.getTenDM());
            btn.setUserData(dm.getMaDM());
            btn.getStyleClass().add("category-button");
            btn.setOnAction(e -> {
                deactivateAllButtons();
                btn.getStyleClass().add("active");
                filterTableData(dm.getMaDM());
                txtSearch.clear();
            });
            categoryButtonBox.getChildren().add(btn);
        }
    }

    private void deactivateAllButtons() {
        categoryButtonBox.getChildren().forEach(node -> node.getStyleClass().remove("active"));
    }

    private void toggleForm(boolean visible) {
        formInputArea.setVisible(visible);
        if (visible && tableThucDon.getSelectionModel().getSelectedItem() == null) {
            clearForm();
        }
    }

    private void fillForm(MenuItem item) {
        txtTenMon.setText(item.getName());
        txtDonGia.setText(String.format("%.0f", item.getPrice()));
        previewImg.setImage(item.getImage());
        currentHinhAnhUrl = item.getHinhAnhUrl();
        
        // Select category in combo
        for (DanhMucMon dm : dsDanhMuc) {
            if (dm.getMaDM().equals(item.getMaDM())) {
                comboDanhMuc.setValue(dm);
                break;
            }
        }
    }

    private void handleSearch(String searchTerm) {
        String term = searchTerm.trim();
        Response res;
        if (term.isEmpty()) {
            res = Client.send(CommandType.GET_MENU, null);
        } else {
            res = Client.sendWithParams(CommandType.GET_MENU, Map.of("query", term));
        }
        
        if (res.getStatusCode() == 200) {
            List<MonAn> results = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), MonAn.class);
            populateTable(new ArrayList<>(results));
            deactivateAllButtons();
        }
    }

    private void filterTableData(String maDM) {
        Response res = Client.sendWithParams(CommandType.GET_MENU, Map.of("maDM", maDM));
        if (res.getStatusCode() == 200) {
            List<MonAn> list = JsonUtil.fromJsonList(JsonUtil.toJson(res.getData()), MonAn.class);
            populateTable(new ArrayList<>(list));
        }
    }

    private void populateTable(ArrayList<MonAn> listFromDB) {
        dsMonAnUI.clear();
        for (MonAn monAn : listFromDB) {
            Image fxImage = null;
            if (monAn.getHinhAnhUrl() != null && !monAn.getHinhAnhUrl().isEmpty()) {
                // Ưu tiên tải từ URL (Cloudinary)
                String urlWithBuster = monAn.getHinhAnhUrl() + (monAn.getHinhAnhUrl().contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
                fxImage = new Image(urlWithBuster, true); // true = background loading
                
                // Thêm listener xử lý lỗi khi tải ảnh từ URL
                final Image finalImg = fxImage;
                fxImage.errorProperty().addListener((obs, oldV, newV) -> {
                    if (newV) {
                        System.err.println("[UI:ThucDon] Lỗi tải ảnh từ URL: " + monAn.getHinhAnhUrl());
                    }
                });
            } else if (monAn.getHinhAnh() != null && monAn.getHinhAnh().length > 0) {
                // Fallback sang dữ liệu nhị phân (legacy)
                try {
                    fxImage = new Image(new ByteArrayInputStream(monAn.getHinhAnh()));
                } catch (Exception e) {
                    System.err.println("[UI:ThucDon] Lỗi tạo ảnh từ byte array: " + e.getMessage());
                }
            }

            // Find category name
            String categoryName = monAn.getMaDM();
            for (DanhMucMon dm : dsDanhMuc) {
                if (dm.getMaDM().equals(monAn.getMaDM())) {
                    categoryName = dm.getTenDM();
                    break;
                }
            }

            MenuItem itemUI = new MenuItem(
                monAn.getMaMon(), monAn.getTenMon(), fxImage,
                monAn.getGiaBan(), monAn.getMaDM(), categoryName, monAn.getHinhAnhUrl()
            );
            dsMonAnUI.add(itemUI);
        }
    }

    private void handleLuuForm() {
        String tenMon = txtTenMon.getText().trim();
        String donGiaStr = txtDonGia.getText().replace(",", "").trim();
        DanhMucMon selectedDM = comboDanhMuc.getValue();

        if (tenMon.isEmpty() || donGiaStr.isEmpty() || selectedDM == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        double donGia;
        try {
            donGia = Double.parseDouble(donGiaStr);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Đơn giá không hợp lệ.");
            return;
        }

        // Handle Image Upload
        String finalImageUrl = currentHinhAnhUrl;
        if (selectedImageFile != null) {
            // Hiển thị loading hoặc chạy background nếu cần
            String uploadedUrl = CloudinaryUtil.uploadImage(selectedImageFile.getAbsolutePath(), "mon_" + System.currentTimeMillis());
            if (uploadedUrl != null) {
                finalImageUrl = uploadedUrl;
            } else {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không thể tải ảnh lên Cloudinary. Sẽ dùng ảnh cũ hoặc để trống.");
            }
        }

        MenuItem selectedItem = tableThucDon.getSelectionModel().getSelectedItem();
        MonAn monAn = new MonAn();
        monAn.setTenMon(tenMon);
        monAn.setGiaBan(donGia);
        monAn.setMaDM(selectedDM.getMaDM());
        monAn.setHinhAnhUrl(finalImageUrl);

        Map<String, Object> params = new HashMap<>();
        if (selectedItem == null) {
            monAn.setMaMon("MON" + System.currentTimeMillis());
            params.put("type", "ADD");
        } else {
            monAn.setMaMon(selectedItem.getMaMon());
            params.put("type", "UPDATE");
        }
        params.put("monAn", monAn);

        Response res = Client.sendWithParams(CommandType.UPDATE_MENU, params);
        if (res.getStatusCode() == 200) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu thông tin món ăn.");
            filterTableData(selectedDM.getMaDM());
            toggleForm(false);
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lưu thất bại: " + res.getMessage());
        }
    }

    private void handleXoaMonAn() {
        MenuItem selectedItem = tableThucDon.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa món " + selectedItem.getName() + "?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            Map<String, Object> params = new HashMap<>();
            params.put("type", "DELETE");
            params.put("maMon", selectedItem.getMaMon());
            params.put("maDM", selectedItem.getMaDM());
            
            if (Client.sendWithParams(CommandType.UPDATE_MENU, params).getStatusCode() == 200) {
                filterTableData(selectedItem.getMaDM());
                clearForm();
            }
        }
    }

    private void onUploadClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh món ăn");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedImageFile = fileChooser.showOpenDialog(uploadLabel.getScene().getWindow());
        if (selectedImageFile != null) {
            previewImg.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    private void clearForm() {
        txtTenMon.clear();
        txtDonGia.clear();
        previewImg.setImage(null);
        comboDanhMuc.setValue(null);
        selectedImageFile = null;
        currentHinhAnhUrl = null;
    }

    private void setupTableColumns() {
        colTenMon.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDanhMuc.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colDonGia.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDonGia.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%,.0f Đ", item.doubleValue()));
            }
        });
        
        colHinhAnh.setCellValueFactory(new PropertyValueFactory<>("image"));
        colHinhAnh.setCellFactory(c -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            { iv.setFitWidth(60); iv.setFitHeight(40); iv.setPreserveRatio(true); }
            @Override protected void updateItem(Image item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else { iv.setImage(item); setGraphic(iv); }
            }
        });
    }

    private void setupShortcuts() {
        Platform.runLater(() -> {
            if (txtSearch.getScene() == null) return;
            txtSearch.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> txtSearch.requestFocus());
            txtSearch.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> btnThem.fire());
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Inner ViewModel Class
    public static class MenuItem {
        private final SimpleStringProperty maMon;
        private final SimpleStringProperty name;
        private final SimpleObjectProperty<Image> image;
        private final SimpleDoubleProperty price;
        private final SimpleStringProperty maDM;
        private final SimpleStringProperty categoryName;
        private final String hinhAnhUrl;

        public MenuItem(String maMon, String name, Image image, double price, String maDM, String categoryName, String hinhAnhUrl) {
            this.maMon = new SimpleStringProperty(maMon);
            this.name = new SimpleStringProperty(name);
            this.image = new SimpleObjectProperty<>(image);
            this.price = new SimpleDoubleProperty(price);
            this.maDM = new SimpleStringProperty(maDM);
            this.categoryName = new SimpleStringProperty(categoryName);
            this.hinhAnhUrl = hinhAnhUrl;
        }

        public String getMaMon() { return maMon.get(); }
        public String getName() { return name.get(); }
        public Image getImage() { return image.get(); }
        public SimpleObjectProperty<Image> imageProperty() { return image; }
        public double getPrice() { return price.get(); }
        public String getMaDM() { return maDM.get(); }
        public String getCategoryName() { return categoryName.get(); }
        public String getHinhAnhUrl() { return hinhAnhUrl; }
    }
}