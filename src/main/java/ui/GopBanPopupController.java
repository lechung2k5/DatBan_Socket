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
    private Map<HoaDon, BooleanProperty> selectionMap = new HashMap<>(); // Map líÂ°u tráÂºÂ¡ng thíÂ¡i cháÂ»Ân
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listViewHDCanGop.setItems(hdCanGopList);
        // BáÂ»Â qua SelectionMode.MULTIPLE víÂ¬ ta díÂ¹ng Checkbox
        btnHuy.setOnAction(e -> closePopup());
        btnXacNhanGop.setOnAction(e -> handleXacNhanGop());
        btnXacNhanGop.setDisable(true); // Disable ban íâáÂºÂ§u
        // === FIX: CíÂ i íâáÂºÂ·t CellFactory váÂ»âºi Checkbox ===
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
                // CáÂºÂ­p nháÂºÂ­t Checkbox víÂ  Label
                checkBox.setSelected(selected.get());
                checkBox.setDisable(false); // LuíÂ´n cho phíÂ©p cháÂ»Ân/báÂ»Â cháÂ»Ân
                String ban = (hd.getBan() != null) ? hd.getBan().getMaBan() : "ChíÂ°a GíÂ¡n BíÂ n";
                String trangThai = hd.getTrangThai().getDisplayName();
                label.setText(String.format("%s (BíÂ n %s, %s)", hd.getMaHD(), ban, trangThai));
                setGraphic(hbox);
            }
        }
    });
    // ===========================================
}
/**
* NháÂºÂ­n HíÂ³a íâíÂ¡n Master (HíÂ íâíÂ°áÂ»Â£c cháÂ»Ân táÂ»Â« DatBan) víÂ  táÂºÂ£i danh síÂ¡ch HíÂ cíÂ³ tháÂ»Æ gáÂ»â¢p.
* ðÅ¸âÂ¥ íÂíÆ SáÂ»Â¬A: CháÂ»â° láÂ»Âc HíÂ³a íâíÂ¡n cíÂ³ tráÂºÂ¡ng thíÂ¡i "íÂang pháÂ»Â¥c váÂ»Â¥".
*/
public void setInitialData(HoaDon masterHD, DatBan parentCtrl) {
    this.hoaDonMaster = masterHD;
    this.mainController = parentCtrl;
    lblHDMaster.setText(masterHD.getMaHD() + " (BíÂ n " + masterHD.getBan().getMaBan() + ")");
    // 1. TáÂºÂ£i TáÂºÂ¤T CáÂºÂ¢ HíÂ íâang hoáÂºÂ¡t íâáÂ»â¢ng qua API
    Response res = Client.send(CommandType.GET_INVOICES_PENDING, null);
    if (res.getStatusCode() == 200) {
        List<HoaDon> allActiveHDs = utils.JsonUtil.fromJsonList(utils.JsonUtil.toJson(res.getData()), HoaDon.class);
        List<HoaDon> canGop = allActiveHDs.stream()
        .filter(hd -> !hd.getMaHD().equals(masterHD.getMaHD()))
        .filter(hd -> hd.getTrangThai() == TrangThaiHoaDon.DANG_SU_DUNG || hd.getTrangThai() == TrangThaiHoaDon.HOA_DON_TAM)
        .collect(Collectors.toList());
        hdCanGopList.setAll(canGop);
    }
    // KháÂ»Å¸i táÂºÂ¡o selectionMap cho danh síÂ¡ch máÂ»âºi
    selectionMap.clear();
    for (HoaDon hd : hdCanGopList) {
        selectionMap.put(hd, new SimpleBooleanProperty(false));
    }
    updateButtonState();
}
private void handleXacNhanGop() {
    // LáÂºÂ¥y danh síÂ¡ch HíÂ³a íâíÂ¡n íâíÂ°áÂ»Â£c cháÂ»Ân táÂ»Â« Map
    List<HoaDon> selectedHDs = selectionMap.entrySet().stream()
    .filter(entry -> entry.getValue().get())
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
    if (selectedHDs.isEmpty()) return;
    Optional<ButtonType> result = showAlertConfirm("XíÂ¡c nháÂºÂ­n GáÂ»â¢p",
    String.format("BáÂºÂ¡n cíÂ³ cháÂºÂ¯c cháÂºÂ¯n muáÂ»ân gáÂ»â¢p %d HíÂ³a íâíÂ¡n íâíÂ£ cháÂ»Ân víÂ o HíÂ³a íâíÂ¡n %s khíÂ´ng?", selectedHDs.size(), hoaDonMaster.getMaHD()));
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            // DuyáÂ»â¡t qua táÂ»Â«ng HíÂ nguáÂ»ân víÂ  tháÂ»Â±c hiáÂ»â¡n giao dáÂ»â¹ch gáÂ»â¢p
            for (HoaDon sourceHD : selectedHDs) {
                // 1. ChuyáÂ»Æn míÂ³n táÂ»Â« HíÂ nguáÂ»ân sang HíÂ íâíÂ­ch qua API
                Client.sendWithParams(CommandType.MERGE_INVOICES, Map.of(
                "targetId", hoaDonMaster.getMaHD(),
                "sourceId", sourceHD.getMaHD()
                ));
                // 2. XíÂ³a HíÂ nguáÂ»ân víÂ  giáÂºÂ£i phíÂ³ng bíÂ n nguáÂ»ân qua API
                Client.sendWithParams(CommandType.CLEANUP_MERGED, Map.of("maHD", sourceHD.getMaHD()));
            }
            showAlert(AlertType.INFORMATION, "ThíÂ nh cíÂ´ng", "íÂã gộp thành công " + selectedHDs.size() + " Hóa đơn vào HD " + hoaDonMaster.getMaHD());
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