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
    
    // 🔥 Đăng ký danh sách các kênh stream để broadcast real-time
    private static final Set<ObjectOutputStream> notificationClients = ConcurrentHashMap.newKeySet();
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(">>> SERVER ĐANG CHẠY TẠI PORT " + PORT + " <<<");
        System.out.println("==============================================");
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        running = true;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Đã sẵn sàng nhận kết nối.");
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
        notificationClients.remove(out);
        System.out.println("[BROADCAST] Đã hủy 1 client. Còn lại: " + notificationClients.size());
    }

    /**
     * Phát loa thông báo tới tất cả các client đang kết nối.
     */
    public static void broadcast(Object event) {
        System.out.println("[BROADCAST] Đang gửi sự kiện: " + event);
        for (ObjectOutputStream out : notificationClients) {
            try {
                out.writeObject(event);
                out.flush();
                out.reset(); // Quan trọng để tránh cache object
            } catch (IOException e) {
                // Nếu lỗi, có thể client đã ngắt kết nối
                System.err.println("[BROADCAST ERROR] Lỗi gửi tới 1 client, đang dọn dẹp...");
                notificationClients.remove(out);
            }
        }
    }
}