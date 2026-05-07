package ui;
import network.Client;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.shape.Arc; // 👈 THÊM DÒNG NÀY
import javafx.application.Platform;
public class PreloaderController {
    @FXML
    private Arc progressArc; // 👈 ĐỔI THÀNH Arc
    private MainApp mainApp;
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
    @FXML
    void initialize() {
        Task<Void> startupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Giả lập khởi tạo (0% - 30%)
                updateMessage("Đang khởi tạo...");
                updateProgress(10, 100);
                Thread.sleep(1000);
                // 2. Kết nối CSDL (30% - 80%)
                updateMessage("Đang kết nối CSDL...");
                updateProgress(30, 100);
                try {
                    // Check API connectivity
                    // For now, we just assume if we can instantiate it, we are good,
                    // or we can ping a simple endpoint.
                } catch (Exception e) {
                updateMessage("Không thể kết nối đến Server API!");
                Thread.sleep(2000);
                Platform.exit();
            }
            Thread.sleep(1500);
            updateProgress(80, 100);
            // 3. Tải tài nguyên (80% - 100%)
            updateMessage("Đang tải tài nguyên...");
            Thread.sleep(1000);
            updateProgress(100, 100);
            updateMessage("Hoàn tất!");
            Thread.sleep(500);
            return null;
        }
    };
    // 🔥 CÁCH KẾT NỐI PROGRESS VỚI ARC:
    // Cập nhật thuộc tính 'length' của Arc dựa trên progress của Task
    // Tiến trình 0-100 sẽ được map sang 0-360 độ.
    startupTask.progressProperty().addListener((obs, oldVal, newVal) -> {
        Platform.runLater(() -> {
            double progress = newVal.doubleValue();
            // 🔥 LOGIC MỚI:
            // Chỉ hiện và cập nhật Arc KHI progress > 1%
            if (progress > 0.01) {
                // 1. Bật hiển thị Arc lên
                progressArc.setVisible(true);
                // 2. Cập nhật độ dài
                progressArc.setLength(progress * -360);
            } else {
            // Nếu dưới 1%, bắt buộc phải ẨN ĐI
            progressArc.setVisible(false);
        }
    });
});
// Xử lý khi Task hoàn thành (vẫn giữ)
startupTask.setOnSucceeded(e -> {
    if (mainApp != null) {
        mainApp.gotoLogin();
    }
});
// Bắt đầu chạy Task (vẫn giữ)
new Thread(startupTask).start();
}
}