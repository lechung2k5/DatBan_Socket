package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import utils.LocalDateAdapter;
import utils.LocalDateTimeAdapter;
import utils.LocalTimeAdapter;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import utils.ServerSessionService;
import utils.JsonUtil;

/**
 * MobileWebSocketServer - Cổng kết nối dành riêng cho Mobile App (React Native)
 * Sử dụng giao thức WebSocket và định dạng dữ liệu JSON.
 */
public class MobileWebSocketServer extends WebSocketServer {

    private final RequestDispatcher dispatcher = new RequestDispatcher();
    private final Gson gson;
    
    // 🔥 Quản lý các phiên kết nối theo Định danh (SĐT hoặc "MANAGER")
    private static final Map<String, WebSocket> authenticatedSessions = new ConcurrentHashMap<>();

    public MobileWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                .create();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[MOBILE-WS] Client mới kết nối: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[MOBILE-WS] Client ngắt kết nối: " + conn.getRemoteSocketAddress());
        // Dọn dẹp session cũ
        authenticatedSessions.values().removeIf(ws -> ws.equals(conn));
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            System.out.println("[MOBILE-WS] Nhận tin: " + message);
            
            // Parse JSON sang Request object
            Request request = gson.fromJson(message, Request.class);

            // 🔥 TỰ ĐỘNG NHẬN DIỆN CLIENT TỪ TOKEN
            String token = request.getToken();
            if (token != null && !token.isBlank()) {
                String sessionDataJson = ServerSessionService.getSessionData(token);
                if (sessionDataJson != null) {
                    Map<String, Object> sessionData = JsonUtil.fromJson(sessionDataJson, Map.class);
                    String identifier = (String) sessionData.get("employeeId"); // Hoặc maKH
                    String role = (String) sessionData.get("role");

                    if (identifier != null) {
                        if ("Admin".equalsIgnoreCase(role) || "QuanLy".equalsIgnoreCase(role) || "ThuNgan".equalsIgnoreCase(role)) {
                            authenticatedSessions.put("MANAGER", conn);
                        } else {
                            authenticatedSessions.put(identifier, conn);
                        }
                    }
                }
            }
            
            // Xử lý request qua dispatcher
            Response response = dispatcher.dispatch(request);
            
            // Gửi response về client dưới dạng JSON
            if (request.getRequestId() != null) {
                response.setRequestId(request.getRequestId());
            }
            String jsonResponse = gson.toJson(response);
            conn.send(jsonResponse);
            
        } catch (Exception e) {
            System.err.println("[MOBILE-WS] Lỗi xử lý tin nhắn: " + e.getMessage());
            conn.send(gson.toJson(Response.error("Lỗi Server: " + e.getMessage())));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[MOBILE-WS] Lỗi server: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[MOBILE-WS] WebSocket Server dành cho Mobile đã khởi chạy tại port: " + getAddress().getPort());
    }

    /**
     * Gửi tin nhắn tới một mục tiêu cụ thể
     */
    public void sendToTarget(String targetId, String jsonMessage) {
        WebSocket conn = authenticatedSessions.get(targetId);
        if (conn != null && conn.isOpen()) {
            conn.send(jsonMessage);
            System.out.println("[MOBILE-WS] Đã gửi thông báo tới: " + targetId);
        } else {
            System.out.println("[MOBILE-WS] Không tìm thấy kết nối hoạt động cho: " + targetId);
        }
    }
}
