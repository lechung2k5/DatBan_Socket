package service;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.VaiTro;
import utils.ServerSessionService;
import dao.EmployeeDAO;
import utils.JsonUtil;
import network.Request;
import network.Response;
import java.util.HashMap;
import java.util.Map;
/**
* AuthService - Quản lý đăng nhập và phiên làm việc (Session)
* Tích hợp với Redis để lưu trữ session token.
*/
public class AuthService {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    public Response handleLogin(Request request) {
        try {
            String username = (String) request.getParam("username");
            String password = (String) request.getParam("password");
            if (username == null || password == null) {
                return Response.error("Thiếu tên đăng nhập hoặc mật khẩu");
            }
            NhanVien emp = employeeDAO.findByUsername(username);
            if (emp != null && password.equals(emp.getMatKhau())) {
                // Tạo token trong Redis (Hết hạn sau 24h hoặc theo cấu hình)
                String token = ServerSessionService.createSession(emp.getMaNV(), emp.getChucVu());
                // Chuẩn bị dữ liệu trả về
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("employee", emp);
                // Entity TaiKhoan để tương thích với UI cũ
                TaiKhoan tk = new TaiKhoan();
                tk.setTenDangNhap(username);
                tk.setVaiTro(VaiTro.fromString(emp.getChucVu()));
                tk.setNhanVien(emp);
                data.put("account", tk);
                System.out.println("[AUTH] Đăng nhập thành công: " + username + " (Role: " + emp.getChucVu() + ")");
                return Response.ok(data);
            } else {
            System.out.println("[AUTH] Đăng nhập thất bại: " + username);
            return Response.error("Sai tài khoản hoặc mật khẩu");
        }
    } catch (Exception e) {
    System.err.println("[AUTH] Lỗi Server: " + e.getMessage());
    return Response.error("Lỗi Server: " + e.getMessage());
}
}
public Response handleLogout(Request request) {
    String token = request.getToken();
    if (token != null) {
        ServerSessionService.invalidate(token);
    }
    return Response.ok("Đã đăng xuất thành công");
}
public Response handleChangePassword(Request request) {
    try {
        String username = (String) request.getParam("username");
        String oldPassword = (String) request.getParam("oldPassword");
        String newPassword = (String) request.getParam("newPassword");
        NhanVien emp = employeeDAO.findByUsername(username);
        if (emp != null && oldPassword.equals(emp.getMatKhau())) {
            employeeDAO.updatePassword(emp.getMaNV(), newPassword);
            return Response.ok("Đổi mật khẩu thành công");
        } else {
        return Response.error("Mật khẩu cũ không chính xác");
    }
} catch (Exception e) {
return Response.error("Lỗi đổi mật khẩu: " + e.getMessage());
}
}
public Response handleForgotPasswordUpdate(Request request) {
    try {
        String maNV = (String) request.getParam("username");
        String newPassword = (String) request.getParam("newPassword");
        // Check if exists
        NhanVien emp = employeeDAO.findByUsername(maNV);
        if (emp == null) return Response.error("Không tìm thấy nhân viên");
        employeeDAO.updatePassword(maNV, newPassword);
        return Response.ok("Cập nhật mật khẩu thành công");
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
}