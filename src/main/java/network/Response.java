package network;
import java.io.Serializable;
public class Response implements Serializable {
    private int statusCode; // 200: OK, 400: Error, etc.
    private String message;
    private Object data;
    private String requestId;
    public Response() {}
    public Response(int statusCode, String message, Object data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }
    public static Response ok(Object data) {
        return new Response(200, "Success", data);
    }
    public static Response error(String message) {
        return new Response(400, message, null);
    }
    public int getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}