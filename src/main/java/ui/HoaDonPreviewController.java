package ui;

import entity.ChiTietHoaDon;
import entity.HoaDon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class HoaDonPreviewController implements Initializable {

    @FXML private Label lblMaHD, lblNgayLap, lblMaBan, lblNhanVien, lblKhachHang;
    @FXML private Label lblTongTienMon, lblPhiDichVu, lblThueVAT, lblTienCoc, lblKhuyenMai, lblTongThanhToan;
    @FXML private VBox vboxItems, vboxInvoicePrint;
    @FXML private Button btnIn, btnHuy;

    private final DecimalFormat fmt = new DecimalFormat("###,###,### VNĐ");
    private final DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnHuy.setOnAction(e -> closePopup());
        btnIn.setOnAction(e -> handleInHoaDon());
    }

    public void setHoaDon(HoaDon hd) {
        if (hd == null) return;

        lblMaHD.setText(hd.getMaHD() != null ? hd.getMaHD() : "N/A");
        lblNgayLap.setText(hd.getNgayLap() != null ? hd.getNgayLap().format(dtFmt) : "N/A");
        lblMaBan.setText(hd.getMaBan() != null ? hd.getMaBan() : "N/A");
        lblNhanVien.setText(hd.getTenNhanVien() != null ? hd.getTenNhanVien() : "Admin");
        lblKhachHang.setText(hd.getKhachHang() != null ? hd.getKhachHang().getTenKH() : "Khách vãng lai");

        lblTongTienMon.setText(fmt.format(hd.getTongCongMonAn()));
        lblPhiDichVu.setText(fmt.format(hd.getPhiDichVu()));
        lblThueVAT.setText(fmt.format(hd.getThueVAT()));
        lblTienCoc.setText("-" + fmt.format(hd.getTienCoc()));
        lblKhuyenMai.setText("-" + fmt.format(hd.getKhuyenMai()));
        lblTongThanhToan.setText(fmt.format(hd.getTongTienThanhToan()));

        // Thêm chi tiết món
        vboxItems.getChildren().clear();
        // Header
        HBox header = new HBox();
        header.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 5 0;");
        Label l1 = new Label("Tên món"); l1.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l1, javafx.scene.layout.Priority.ALWAYS);
        Label l2 = new Label("SL"); l2.setPrefWidth(30); l2.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label l3 = new Label("Đ.Giá"); l3.setPrefWidth(80); l3.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label l4 = new Label("T.Tiền"); l4.setPrefWidth(90); l4.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        header.getChildren().addAll(l1, l2, l3, l4);
        vboxItems.getChildren().add(header);

        if (hd.getChiTietHoaDon() != null) {
            for (ChiTietHoaDon ct : hd.getChiTietHoaDon()) {
                HBox row = new HBox();
                Label n = new Label(ct.getTenMon()); n.setWrapText(true); n.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(n, javafx.scene.layout.Priority.ALWAYS);
                Label q = new Label(String.valueOf(ct.getSoLuong())); q.setPrefWidth(30); q.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                Label p = new Label(new DecimalFormat("###,###").format(ct.getDonGia())); p.setPrefWidth(80); p.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                Label t = new Label(new DecimalFormat("###,###").format(ct.getThanhTien())); t.setPrefWidth(90); t.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                row.getChildren().addAll(n, q, p, t);
                vboxItems.getChildren().add(row);
            }
        }
    }

    private void handleInHoaDon() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(btnIn.getScene().getWindow())) {
            Node node = vboxInvoicePrint;
            
            // Lấy máy in và cấu hình trang
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
            
            // Tính toán tỷ lệ để vừa trang (Nếu hóa đơn to hơn trang in)
            double printableWidth = pageLayout.getPrintableWidth();
            double nodeWidth = node.getBoundsInParent().getWidth();
            double scaleX = printableWidth / nodeWidth;
            
            if (scaleX < 1.0) {
                node.getTransforms().add(new Scale(scaleX, scaleX));
            }

            boolean success = job.printPage(node);
            if (success) {
                job.endJob();
                closePopup();
            }
            
            // Reset transform sau khi in
            node.getTransforms().clear();
        }
    }

    private void closePopup() {
        Stage stage = (Stage) btnHuy.getScene().getWindow();
        stage.close();
    }
}
