package ui;
import network.Client;
import network.CommandType;
import network.Response;
import utils.JsonUtil;
import entity.*;
import entity.TrangThaiHoaDon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
public class GopBanPopupController implements Initializable {
    @FXML private Label lblHDMaster;
    @FXML private ListView<HoaDon> listViewHDCanGop;
    @FXML private Button btnHuy;
    @FXML private Button btnXacNhanGop;
    private HoaDon hoaDonMaster;
    private DatBan mainController;
    private ObservableList<HoaDon> hdCanGopList = FXCollections.observableArrayList();
    private Map<HoaDon, BooleanProperty> selectionMap = new HashMap<>(); // Map lÃÂ°u tráÂºÂ¡ng thÃÂ¡i cháÂ»Ân
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listViewHDCanGop.setItems(hdCanGopList);
        // BáÂ»Â qua SelectionMode.MULTIPLE vÃÂ¬ ta dÃÂ¹ng Checkbox
        btnHuy.setOnAction(e -> closePopup());
        btnXacNhanGop.setOnAction(e -> handleXacNhanGop());
        btnXacNhanGop.setDisable(true); // Disable ban ÃâáÂºÂ§u
        // === FIX: CÃÂ i ÃâáÂºÂ·t CellFactory váÂ»âºi Checkbox ===
        listViewHDCanGop.setCellFactory(lv -> new ListCell<HoaDon>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label label = new Label();
            private final HBox hbox = new HBox(5, checkBox, label);
            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                // Listener cho Checkbox
                checkBox.setOnAction(event -> {
                    if (getItem() != null) {
                        selectionMap.get(getItem()).set(checkBox.isSelected());
                        updateButtonState();
                    }
                });
            }
            @Override
            protected void updateItem(HoaDon hd, boolean empty) {
                super.updateItem(hd, empty);
                if (empty || hd == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                // LáÂºÂ¥y/TáÂºÂ¡o BooleanProperty
                BooleanProperty selected = selectionMap.computeIfAbsent(hd, k -> new SimpleBooleanProperty(false));
                // CáÂºÂ­p nháÂºÂ­t Checkbox vÃÂ  Label
                checkBox.setSelected(selected.get());
                checkBox.setDisable(false); // LuÃÂ´n cho phÃÂ©p cháÂ»Ân/báÂ»Â cháÂ»Ân
                String ban = (hd.getBan() != null) ? hd.getBan().getMaBan() : "ChÃÂ°a GÃÂ¡n BÃÂ n";
                String trangThai = hd.getTrangThai().getDisplayName();
                label.setText(String.format("%s (BÃÂ n %s, %s)", hd.getMaHD(), ban, trangThai));
                setGraphic(hbox);
            }
        }
    });
    // ===========================================
}
/**
* NháÂºÂ­n HÃÂ³a ÃâÃÂ¡n Master (HÃÂ ÃâÃÂ°áÂ»Â£c cháÂ»Ân táÂ»Â« DatBan) vÃÂ  táÂºÂ£i danh sÃÂ¡ch HÃÂ cÃÂ³ tháÂ»Æ gáÂ»â¢p.
* Ã°Å¸âÂ¥ ÃÂÃÆ SáÂ»Â¬A: CháÂ»â° láÂ»Âc HÃÂ³a ÃâÃÂ¡n cÃÂ³ tráÂºÂ¡ng thÃÂ¡i "ÃÂang pháÂ»Â¥c váÂ»Â¥".
*/
public void setInitialData(HoaDon masterHD, DatBan parentCtrl) {
    this.hoaDonMaster = masterHD;
    this.mainController = parentCtrl;
    lblHDMaster.setText(masterHD.getMaHD() + " (BÃÂ n " + masterHD.getBan().getMaBan() + ")");
    // 1. TáÂºÂ£i TáÂºÂ¤T CáÂºÂ¢ HÃÂ Ãâang hoáÂºÂ¡t ÃâáÂ»â¢ng qua API
    Response res = Client.send(CommandType.GET_INVOICES_PENDING, null);
    if (res.getStatusCode() == 200) {
        List<HoaDon> allActiveHDs = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), HoaDon.class);
        List<HoaDon> canGop = allActiveHDs.stream()
        .filter(hd -> !hd.getMaHD().equals(masterHD.getMaHD()))
        .filter(hd -> hd.getTrangThai() == TrangThaiHoaDon.DANG_SU_DUNG || hd.getTrangThai() == TrangThaiHoaDon.HOA_DON_TAM)
        .collect(Collectors.toList());
        hdCanGopList.setAll(canGop);
    }
    // KháÂ»Å¸i táÂºÂ¡o selectionMap cho danh sÃÂ¡ch máÂ»âºi
    selectionMap.clear();
    for (HoaDon hd : hdCanGopList) {
        selectionMap.put(hd, new SimpleBooleanProperty(false));
    }
    updateButtonState();
}
private void handleXacNhanGop() {
    // LáÂºÂ¥y danh sÃÂ¡ch HÃÂ³a ÃâÃÂ¡n ÃâÃÂ°áÂ»Â£c cháÂ»Ân táÂ»Â« Map
    List<HoaDon> selectedHDs = selectionMap.entrySet().stream()
    .filter(entry -> entry.getValue().get())
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
    if (selectedHDs.isEmpty()) return;
    Optional<ButtonType> result = showAlertConfirm("XÃÂ¡c nháÂºÂ­n GáÂ»â¢p",
    String.format("BáÂºÂ¡n cÃÂ³ cháÂºÂ¯c cháÂºÂ¯n muáÂ»ân gáÂ»â¢p %d HÃÂ³a ÃâÃÂ¡n ÃâÃÂ£ cháÂ»Ân vÃÂ o HÃÂ³a ÃâÃÂ¡n %s khÃÂ´ng?", selectedHDs.size(), hoaDonMaster.getMaHD()));
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            // DuyáÂ»â¡t qua táÂ»Â«ng HÃÂ nguáÂ»ân vÃÂ  tháÂ»Â±c hiáÂ»â¡n giao dáÂ»â¹ch gáÂ»â¢p
            for (HoaDon sourceHD : selectedHDs) {
                // 1. ChuyáÂ»Æn mÃÂ³n táÂ»Â« HÃÂ nguáÂ»ân sang HÃÂ ÃâÃÂ­ch qua API
                Client.sendWithParams(CommandType.MERGE_INVOICES, Map.of(
                "targetId", hoaDonMaster.getMaHD(),
                "sourceId", sourceHD.getMaHD()
                ));
                // 2. XÃÂ³a HÃÂ nguáÂ»ân vÃÂ  giáÂºÂ£i phÃÂ³ng bÃÂ n nguáÂ»ân qua API
                Client.sendWithParams(CommandType.CLEANUP_MERGED, Map.of("maHD", sourceHD.getMaHD()));
            }
            showAlert(AlertType.INFORMATION, "ThÃÂ nh cÃÂ´ng", "ÃÂã gộp thành công " + selectedHDs.size() + " Hóa đơn vào HD " + hoaDonMaster.getMaHD());
            // 3. Refresh UI chính và đóng popup
            mainController.loadBookingCards();
            mainController.loadTableGrids();
            closePopup();
        } catch (Exception e) {
        showAlert(AlertType.ERROR, "Lỗi", "Không thể hoàn tất giao dịch gộp: " + e.getMessage());
        e.printStackTrace();
    }
}
}
/**
* Cập nhật trạng thái nút Xác nhận Gộp.
*/
private void updateButtonState() {
    long countSelected = selectionMap.values().stream()
    .filter(BooleanProperty::get)
    .count();
    btnXacNhanGop.setDisable(countSelected == 0);
}
private void closePopup() {
    Stage stage = (Stage) btnHuy.getScene().getWindow();
    stage.close();
}
private void showAlert(AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
private Optional<ButtonType> showAlertConfirm(String title, String content) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    return alert.showAndWait();
}
}