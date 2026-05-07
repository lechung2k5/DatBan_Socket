package network;
import db.EnvConfig;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Service {
    private static final int PORT = EnvConfig.serverPort();
    private static final int THREAD_POOL_SIZE = 20;
    private static volatile boolean running = true;
    private static ServerSocket serverSocket;
    private static ExecutorService executorService;
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
}