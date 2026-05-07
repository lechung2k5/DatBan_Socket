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
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dispatcher = new RequestDispatcher();
    }
    @Override
    public void run() {
        try (
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            while (!socket.isClosed()) {
                try {
                    // Đọc Request từ client
                    Request request = (Request) in.readObject();
                    System.out.println("[REQUEST RECEIVED] CommandType: " + request.getAction());
                    // Xử lý và lấy Response
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
        e.printStackTrace();
        out.writeObject(Response.error("Lỗi Server: " + e.getMessage()));
        out.flush();
    }
}
} catch (IOException e) {
System.err.println("Lỗi kết nối client: " + e.getMessage());
} finally {
try {
    socket.close();
} catch (IOException e) {
e.printStackTrace();
}
}
}
}