package ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.application.Platform;
import service.NotificationService;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Map;
import network.RealTimeEvent;
import network.RealTimeSubscriber;
import network.CommandType;
import java.util.function.Consumer;

public class NotificationController implements Initializable, RealTimeSubscriber {

    @FXML
    private VBox notificationList;

    private ManHinhChinh mainController;
    private final String targetId = "MANAGER";

    private final Consumer<RealTimeEvent> rtListener = event -> {
        if (event.getType() == CommandType.NEW_NOTIFICATION) {
            Platform.runLater(this::refresh);
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refresh();
        network.RealTimeClient.getInstance().addListener(rtListener);
    }

    public void setMainController(ManHinhChinh mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void refresh() {
        notificationList.getChildren().clear();
        List<Map<String, Object>> notifications = NotificationService.getNotifications(targetId);

        if (notifications.isEmpty()) {
            Label emptyLabel = new Label("Không có thông báo nào");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            notificationList.getChildren().add(emptyLabel);
            return;
        }

        for (Map<String, Object> notif : notifications) {
            notificationList.getChildren().add(createNotificationCard(notif));
        }

        // Cập nhật badge ở màn hình chính nếu có
        if (mainController != null) {
            mainController.updateNotificationBadge();
        }
    }

    private VBox createNotificationCard(Map<String, Object> notif) {
        String id = (String) notif.get("notificationId");
        String title = (String) notif.get("title");
        String message = (String) notif.get("message");
        String type = (String) notif.get("type");
        String time = (String) notif.get("createdAt");
        boolean isRead = (boolean) notif.getOrDefault("isRead", false);

        VBox card = new VBox(5);
        card.getStyleClass().add("notification-card");
        if (!isRead) card.getStyleClass().add("notification-card-unread");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("notification-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("✕");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            NotificationService.deleteNotification(targetId, id);
            refresh();
            e.consume();
        });

        header.getChildren().addAll(titleLabel, spacer, deleteBtn);

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("notification-message");
        msgLabel.setWrapText(true);

        Label timeLabel = new Label(formatTime(time));
        timeLabel.getStyleClass().add("notification-time");

        card.getChildren().addAll(header, msgLabel, timeLabel);

        card.setOnMouseClicked(e -> {
            if (!isRead) {
                NotificationService.markAsRead(targetId, id);
                card.getStyleClass().remove("notification-card-unread");
            }
            handleNavigation(type);
        });

        return card;
    }

    private String formatTime(String timeStr) {
        try {
            // Đơn giản hóa hiển thị thời gian
            return timeStr.replace("T", " ").substring(0, 16);
        } catch (Exception e) {
            return timeStr;
        }
    }

    private void handleNavigation(String type) {
        if (mainController == null) return;
        
        try {
            switch (type) {
                case "BOOKING":
                    mainController.handleQuanLyDatBan();
                    break;
                case "SYSTEM":
                    mainController.handleManHinhChinh();
                    break;
                case "PROMO":
                    // Có thể điều hướng sang trang khác nếu cần
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleMarkAllAsRead() {
        List<Map<String, Object>> notifications = NotificationService.getNotifications(targetId);
        for (Map<String, Object> notif : notifications) {
            if (!(boolean)notif.getOrDefault("isRead", false)) {
                NotificationService.markAsRead(targetId, (String)notif.get("notificationId"));
            }
        }
        refresh();
    }

    @FXML
    public void handleClearAll() {
        List<Map<String, Object>> notifications = NotificationService.getNotifications(targetId);
        for (Map<String, Object> notif : notifications) {
            NotificationService.deleteNotification(targetId, (String)notif.get("notificationId"));
        }
        refresh();
    }

    @Override
    public Consumer<RealTimeEvent> getRealTimeListener() {
        return rtListener;
    }

    @Override
    public void cleanupRealTime() {
        network.RealTimeClient.getInstance().removeListener(rtListener);
    }
}
