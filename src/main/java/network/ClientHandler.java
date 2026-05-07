package network;

import network.Request;
import network.Response;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private RequestDispatcher dispatcher;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dispatcher = new RequestDispatcher();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (!socket.isClosed()) {
                try {
                    // Đọc Request từ client
                    Object obj = in.readObject();
                    if (!(obj instanceof Request)) continue;
                    
                    Request request = (Request) obj;
                    System.out.println("[REQUEST RECEIVED] CommandType: " + request.getAction());
                    
                    // 🔥 Xử lý Đăng ký Real-time
                    if (request.getAction() == CommandType.SUBSCRIBE_REALTIME) {
                        Service.registerNotificationClient(out);
                        out.writeObject(new Response(200, "Đã đăng ký nhận tin Real-time thành công.", null));
                        out.flush();
                        continue; // Tiếp tục vòng lặp để giữ kết nối
                    }

                    // Xử lý và lấy Response thông thường
                    Response response = dispatcher.dispatch(request);
                    
                    // Gửi Response về client
                    out.writeObject(response);
                    out.flush();
                    out.reset(); // Quan trọng: Reset cache của ObjectOutputStream
                } catch (ClassNotFoundException e) {
                    System.err.println("Dữ liệu không hợp lệ: " + e.getMessage());
                    out.writeObject(Response.error("Dữ liệu không hợp lệ"));
                    out.flush();
                } catch (java.io.EOFException e) {
                    System.out.println("[CLIENT DISCONNECTED] " + socket.getInetAddress());
                    break;
                } catch (Exception e) {
                    System.err.println("Lỗi xử lý request: " + e.getMessage());
                    if (!socket.isClosed()) {
                        out.writeObject(Response.error("Lỗi Server: " + e.getMessage()));
                        out.flush();
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi kết nối client: " + e.getMessage());
        } finally {
            // 🔥 Hủy đăng ký khi ngắt kết nối
            if (out != null) {
                Service.unregisterNotificationClient(out);
            }
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}