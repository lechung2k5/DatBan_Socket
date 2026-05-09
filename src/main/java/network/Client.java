package network;

import db.EnvConfig;

import utils.ClientSessionManager;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Client - Lớp trung gian gửi yêu cầu từ Client tới Server qua Socket.
 * Mỗi yêu cầu mở một kết nối mới (Short-lived connection) để đảm bảo tính ổn
 * định.
 */
public class Client {
    private static final String HOST = EnvConfig.serverHost();
    private static final int PORT = EnvConfig.serverPort();

    /**
     * Gửi yêu cầu với CommandType và Payload (data).
     */
    public static Response send(CommandType CommandType, Object data) {
        // Tự động lấy token từ ClientSessionManager
        String token = ClientSessionManager.getInstance().getToken();
        Request req = new Request(CommandType, data, token);
        return sendRequest(req);
    }

    /**
     * Gửi yêu cầu với CommandType và Params (Map).
     */
    public static Response sendWithParams(CommandType CommandType, java.util.Map<String, Object> params) {
        Request req = new Request(CommandType);
        req.setToken(ClientSessionManager.getInstance().getToken());
        if (params != null) {
            params.forEach(req::setParam);
        }
        return sendRequest(req);
    }

    private static Response sendRequest(Request req) {
        System.out
                .println("[API] Sending request: " + req.getAction() + (req.getToken() != null ? " (with token)" : ""));
        try (Socket socket = new Socket(HOST, PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(req);
            out.flush();
            Response res = (Response) in.readObject();
            return res;
        } catch (ConnectException e) {
            System.err.println("[API] Không thể kết nối tới Server tại " + HOST + ":" + PORT);
            return new Response(503,
                    "Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại mạng hoặc trạng thái Server.", null);
        } catch (Exception e) {
            System.err.println("[API] Lỗi: " + e.getMessage());
            return Response.error("Lỗi hệ thống: " + e.getMessage());
        }
    }
}