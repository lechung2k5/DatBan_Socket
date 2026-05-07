package network;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
public class Request implements Serializable {
    private CommandType CommandType;
    private Map<String, Object> params;
    private Object data; // Bổ sung data field cho entity
    private String token; // Bổ sung token cho session
    public Request() {
        this.params = new HashMap<>();
    }
    public Request(CommandType CommandType) {
        this.CommandType = CommandType;
        this.params = new HashMap<>();
    }
    public Request(CommandType CommandType, Object data, String token) {
        this.CommandType = CommandType;
        this.data = data;
        this.token = token;
        this.params = new HashMap<>();
    }
    public CommandType getAction() {
        return CommandType;
    }
    public void setAction(CommandType CommandType) {
        this.CommandType = CommandType;
    }
    public Map<String, Object> getParams() {
        return params;
    }
    public void setParam(String key, Object value) {
        this.params.put(key, value);
    }
    public Object getParam(String key) {
        return this.params.get(key);
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
}