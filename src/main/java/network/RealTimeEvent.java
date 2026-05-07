package network;

import java.io.Serializable;

/**
 * RealTimeEvent - Đối tượng đại diện cho một sự kiện cập nhật hệ thống.
 * Được gửi từ Server tới Client để kích hoạt refresh dữ liệu.
 */
public class RealTimeEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private CommandType type;
    private String message;
    private String affectedId; // 🔥 MỚI: ID của bàn hoặc hóa đơn bị ảnh hưởng
    private Object data;

    public RealTimeEvent(CommandType type) {
        this.type = type;
    }

    public RealTimeEvent(CommandType type, String message) {
        this.type = type;
        this.message = message;
    }

    public RealTimeEvent(CommandType type, String message, String affectedId) {
        this.type = type;
        this.message = message;
        this.affectedId = affectedId;
    }

    public RealTimeEvent(CommandType type, String message, String affectedId, Object data) {
        this.type = type;
        this.message = message;
        this.affectedId = affectedId;
        this.data = data;
    }

    // Getters and Setters
    public CommandType getType() { return type; }
    public void setType(CommandType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAffectedId() { return affectedId; }
    public void setAffectedId(String affectedId) { this.affectedId = affectedId; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() {
        return "RealTimeEvent{" +
                "type=" + type +
                ", message='" + message + '\'' +
                '}';
    }
}
