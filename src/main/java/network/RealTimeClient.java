package network;

import db.EnvConfig;
import javafx.application.Platform;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * RealTimeClient - Bộ lắng nghe sự kiện Real-time từ Server.
 * Chạy trên một luồng riêng biệt để không làm treo giao diện.
 */
public class RealTimeClient {
    private static final String HOST = EnvConfig.serverHost();
    private static final int PORT = EnvConfig.serverPort();
    
    private static RealTimeClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean running = false;
    
    // Danh sách các callback để thông báo cho UI
    private final List<Consumer<RealTimeEvent>> listeners = new ArrayList<>();

    private RealTimeClient() {}

    public static synchronized RealTimeClient getInstance() {
        if (instance == null) {
            instance = new RealTimeClient();
        }
        return instance;
    }

    /**
     * Bắt đầu kết nối và lắng nghe Server.
     */
    public void start() {
        if (running) return;
        running = true;
        
        new Thread(() -> {
            while (running) {
                try {
                    System.out.println("[REALTIME] Đang kết nối tới Server để nhận thông báo...");
                    socket = new Socket(HOST, PORT);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());

                    // Gửi yêu cầu đăng ký nhận tin Real-time
                    out.writeObject(new Request(CommandType.SUBSCRIBE_REALTIME));
                    out.flush();

                    while (running && !socket.isClosed()) {
                        Object obj = in.readObject();
                        if (obj instanceof RealTimeEvent) {
                            RealTimeEvent event = (RealTimeEvent) obj;
                            System.out.println("[REALTIME] Nhận sự kiện: " + event.getType());
                            
                            // Thông báo cho tất cả các listeners trên luồng JavaFX
                            Platform.runLater(() -> {
                                for (Consumer<RealTimeEvent> listener : listeners) {
                                    listener.accept(event);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[REALTIME] Lỗi kết nối hoặc mất kết nối: " + e.getMessage());
                    // Đợi 5 giây trước khi thử kết nối lại
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                } finally {
                    closeResources();
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        closeResources();
    }

    /**
     * Đăng ký một bộ lắng nghe sự kiện (Controller sẽ dùng hàm này).
     */
    public void addListener(Consumer<RealTimeEvent> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<RealTimeEvent> listener) {
        listeners.remove(listener);
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
