package network;
import db.EnvConfig;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.ObjectOutputStream;
public class Service {
    private static final int PORT = EnvConfig.serverPort();
    private static final int THREAD_POOL_SIZE = 20;
    private static volatile boolean running = true;
    private static ServerSocket serverSocket;
    private static ExecutorService executorService;
    
    // 🔥 Đăng ký danh sách các kênh stream để broadcast real-time (Desktop)
    private static final Set<ObjectOutputStream> notificationClients = ConcurrentHashMap.newKeySet();
    
    // 🔥 Tham chiếu tới WebSocket Server của Mobile
    private static MobileWebSocketServer mobileWS;

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(">>> SERVER ĐANG CHẠY TẠI PORT " + PORT + " <<<");
        System.out.println("==============================================");
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        running = true;
        
        // 🔥 Khởi chạy WebSocket Server cho Mobile trên port 8889
        mobileWS = new MobileWebSocketServer(8889);
        mobileWS.start();

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Đã sẵn sàng nhận kết nối.");
            
            // 🔥 Khởi chạy dịch vụ kiểm tra ngầm
            service.NotificationBackgroundService.start();

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    executorService.execute(new ClientHandler(socket));
                } catch (IOException e) {
                if (running) System.err.println("[SERVER ERROR] Lỗi chấp nhận kết nối: " + e.getMessage());
            }
        }
    } catch (IOException e) {
    System.err.println("[SERVER ERROR] Không thể khởi động server: " + e.getMessage());
} finally {
stop();
}
}
public static void stop() {
    running = false;
    try {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (mobileWS != null) {
            try {
                mobileWS.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        service.NotificationBackgroundService.stop();
        System.out.println("[SERVER] Đã dừng.");
    } catch (IOException e) {
    // Ignore
}
}

    /**
     * Đăng ký một stream để nhận thông báo real-time.
     */
    public static void registerNotificationClient(ObjectOutputStream out) {
        notificationClients.add(out);
        System.out.println("[BROADCAST] Đã đăng ký thêm 1 client. Tổng: " + notificationClients.size());
    }

    /**
     * Hủy đăng ký một stream.
     */
    public static void unregisterNotificationClient(ObjectOutputStream out) {
        if (notificationClients.remove(out)) {
            System.out.println("[BROADCAST] Đã hủy 1 client Real-time. Còn lại: " + notificationClients.size());
        }
    }

    /**
     * Phát loa thông báo tới tất cả các client đang kết nối.
     */
    public static void broadcast(Object event) {
        System.out.println("[BROADCAST] Đang gửi sự kiện: " + event + " tới " + notificationClients.size() + " clients.");
        
        // 1. Gửi cho Desktop Clients
        for (ObjectOutputStream out : notificationClients) {
            try {
                out.writeObject(event);
                out.flush();
                out.reset(); 
            } catch (IOException e) {
                notificationClients.remove(out);
            }
        }

        // 2. Gửi cho Mobile Clients (tự động broadcast nếu event là RealTimeEvent)
        if (event instanceof RealTimeEvent) {
            String json = utils.JsonUtil.toJson(event);
            
            // 🔥 GỬI QUA REDIS (MỚI - để Node.js backend nhận được)
            utils.CacheService.publishNotification(json);

            // Gửi qua WebSocket cũ (nếu còn dùng)
            if (mobileWS != null) {
                mobileWS.broadcast(json);
            }
        }
    }

    /**
     * Gửi thông báo tới một mục tiêu cụ thể (Targeted)
     * @param targetId SĐT khách hoặc "MANAGER"
     * @param event Sự kiện
     */
    public static void broadcastTargeted(String targetId, Object event) {
        // 1. Gửi cho Desktop (nếu là MANAGER)
        if ("MANAGER".equalsIgnoreCase(targetId)) {
            for (ObjectOutputStream out : notificationClients) {
                try {
                    out.writeObject(event);
                    out.flush();
                    out.reset(); 
                } catch (IOException e) {
                    notificationClients.remove(out);
                }
            }
        }

        // 2. Gửi cho Mobile (Dù là MANAGER hay Customer)
        String json = utils.JsonUtil.toJson(event);
        
        // 🔥 GỬI QUA REDIS (MỚI - để Node.js backend nhận được)
        utils.CacheService.publishNotification(json);

        if (mobileWS != null) {
            mobileWS.sendToTarget(targetId, json);
        }
    }
}