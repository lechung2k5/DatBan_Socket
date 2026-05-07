package utils;
import entity.NhanVien;
public class ClientSessionManager {
    private static ClientSessionManager instance;
    private String token;
    private NhanVien currentEmployee;
    private ClientSessionManager() {}
    public static ClientSessionManager getInstance() {
        if (instance == null) {
            instance = new ClientSessionManager();
        }
        return instance;
    }
    public void login(String token, NhanVien employee) {
        this.token = token;
        this.currentEmployee = employee;
    }
    public void logout() {
        this.token = null;
        this.currentEmployee = null;
    }
    public String getToken() {
        return token;
    }
    public NhanVien getCurrentEmployee() {
        return currentEmployee;
    }
    public boolean isLoggedIn() {
        return token != null;
    }
}