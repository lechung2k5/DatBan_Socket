package service;

import db.DynamoDBConfig;
import network.RealTimeEvent;
import network.Service;
import network.CommandType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import network.Response;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NotificationService - Quản lý gửi và lưu trữ thông báo
 */
public class NotificationService {
    private static final DynamoDbClient db = DynamoDBConfig.getClient();
    private static final String TBL = "AppNotifications";

    /**
     * Gửi thông báo và lưu vào Database
     * 
     * @param targetId SĐT khách hàng hoặc "MANAGER"
     * @param title    Tiêu đề
     * @param message  Nội dung
     * @param type     Loại thông báo (BOOKING, PROMO, SYSTEM)
     */
    public static void sendNotification(String targetId, String title, String message, String type) {
        try {
            String notificationId = UUID.randomUUID().toString();
            String createdAt = LocalDateTime.now().toString();

            // 1. Lưu vào DynamoDB
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("targetId", AttributeValue.builder().s(targetId).build());
            item.put("notificationId", AttributeValue.builder().s(notificationId).build());
            item.put("title", AttributeValue.builder().s(title).build());
            item.put("message", AttributeValue.builder().s(message).build());
            item.put("type", AttributeValue.builder().s(type).build());
            item.put("createdAt", AttributeValue.builder().s(createdAt).build());
            item.put("isRead", AttributeValue.builder().bool(false).build());

            db.putItem(PutItemRequest.builder().tableName(TBL).item(item).build());

            // 2. Gửi Real-time qua WebSocket
            RealTimeEvent event = new RealTimeEvent(
                    CommandType.NEW_NOTIFICATION,
                    message, // message
                    title, // affectedId (UI uses this for Title)
                    Map.of(
                            "notificationId", notificationId,
                            "title", title,
                            "message", message,
                            "type", type,
                            "createdAt", createdAt));

            // Gửi đích danh cho targetId
            Service.broadcastTargeted(targetId, event);

            // 🔥 3. Publish lên Redis cho Mobile Backend
            utils.CacheService.publishNotification(utils.JsonUtil.toJson(event));

            System.out.println("[Notification] Sent to " + targetId + ": " + title);
        } catch (Exception e) {
            System.err.println("[Notification] Error: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách thông báo cho một đối tượng
     */
    public static java.util.List<java.util.Map<String, Object>> getNotifications(String targetId) {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        try {
            software.amazon.awssdk.services.dynamodb.model.QueryResponse response = db
                    .query(software.amazon.awssdk.services.dynamodb.model.QueryRequest.builder()
                            .tableName(TBL)
                            .keyConditionExpression("targetId = :tid")
                            .expressionAttributeValues(
                                    java.util.Map.of(":tid", AttributeValue.builder().s(targetId).build()))
                            .scanIndexForward(false) // Mới nhất lên đầu
                            .build());

            for (java.util.Map<String, AttributeValue> item : response.items()) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                item.forEach((k, v) -> {
                    if (v.s() != null)
                        map.put(k, v.s());
                    else if (v.bool() != null)
                        map.put(k, v.bool());
                });
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Đánh dấu đã đọc
     */
    public static void markAsRead(String targetId, String notificationId) {
        try {
            db.updateItem(software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.builder()
                    .tableName(TBL)
                    .key(java.util.Map.of(
                            "targetId", AttributeValue.builder().s(targetId).build(),
                            "notificationId", AttributeValue.builder().s(notificationId).build()))
                    .updateExpression("SET isRead = :r")
                    .expressionAttributeValues(java.util.Map.of(":r", AttributeValue.builder().bool(true).build()))
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Xóa thông báo
     */
    public static void deleteNotification(String targetId, String notificationId) {
        try {
            db.deleteItem(software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest.builder()
                    .tableName(TBL)
                    .key(java.util.Map.of(
                            "targetId", AttributeValue.builder().s(targetId).build(),
                            "notificationId", AttributeValue.builder().s(notificationId).build()))
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Response handleGetNotifications(network.Request request) {
        try {
            String targetId = (String) request.getParam("targetId");
            if (targetId == null || targetId.isEmpty()) return Response.error("Thiếu targetId");
            return Response.ok(getNotifications(targetId));
        } catch (Exception e) {
            return Response.error("Lỗi lấy thông báo: " + e.getMessage());
        }
    }

    public static Response handleMarkAsRead(network.Request request) {
        try {
            String targetId = (String) request.getParam("targetId");
            String notificationId = (String) request.getParam("notificationId");
            markAsRead(targetId, notificationId);
            return Response.ok("Đã đánh dấu đã đọc");
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    public static Response handleDeleteNotification(network.Request request) {
        try {
            String targetId = (String) request.getParam("targetId");
            String notificationId = (String) request.getParam("notificationId");
            deleteNotification(targetId, notificationId);
            return Response.ok("Đã xóa thông báo");
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }
}
