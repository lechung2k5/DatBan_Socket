package service;

import entity.NhanVien;
import entity.TaiKhoan;
import entity.VaiTro;
import utils.ServerSessionService;
import dao.EmployeeDAO;
import network.Request;
import network.Response;
import entity.KhachHang;
import dao.CustomerDAO;
import utils.EmailUtil;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * AuthService - Quản lý đăng nhập và phiên làm việc (Session)
 * Tích hợp với Redis để lưu trữ session token.
 */
public class AuthService {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public Response handleLogin(Request request) {
        try {
            String username = (String) request.getParam("username");
            String password = (String) request.getParam("password");
            if (username == null || password == null) {
                return Response.error("Thiếu tên đăng nhập hoặc mật khẩu");
            }
            NhanVien emp = employeeDAO.findByUsername(username);
            if (emp != null && password.equals(emp.getMatKhau())) {
                // Tạo token trong Redis
                String token = ServerSessionService.createSession(emp.getMaNV(), emp.getChucVu());
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("employee", emp);
                
                TaiKhoan tk = new TaiKhoan();
                tk.setTenDangNhap(username);
                tk.setVaiTro(VaiTro.fromString(emp.getChucVu()));
                tk.setNhanVien(emp);
                data.put("account", tk);
                
                System.out.println("[AUTH] Đăng nhập thành công: " + username);
                return Response.ok(data);
            } else {
                return Response.error("Sai tài khoản hoặc mật khẩu");
            }
        } catch (Exception e) {
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
            NhanVien emp = employeeDAO.findByUsername(maNV);
            if (emp == null) return Response.error("Không tìm thấy nhân viên");
            employeeDAO.updatePassword(maNV, newPassword);
            return Response.ok("Cập nhật mật khẩu thành công");
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Gửi mã OTP qua email cho khách hàng
     */
    public Response handleSendOTP(Request request) {
        try {
            String email = (String) request.getParam("email");
            if (email == null || email.isEmpty()) return Response.error("Email không hợp lệ");

            String otp = String.format("%06d", new Random().nextInt(1000000));
            boolean success = EmailUtil.sendOTP(email, otp);
            
            if (success) {
                Map<String, Object> data = new HashMap<>();
                data.put("otp", otp);
                return Response.ok(data);
            } else {
                return Response.error("Không thể gửi email. Vui lòng kiểm tra lại cấu hình server.");
            }
        } catch (Exception e) {
            return Response.error("Lỗi gửi OTP: " + e.getMessage());
        }
    }

    /**
     * Đăng ký khách hàng mới từ mobile app
     */
    public Response handleRegisterCustomer(Request request) {
        try {
            String name = (String) request.getParam("name");
            String phone = (String) request.getParam("phone"); // Số điện thoại làm ID
            String email = (String) request.getParam("email");
            String diaChi = (String) request.getParam("diaChi");
            String password = (String) request.getParam("password"); // Pass đã hash từ mobile

            if (phone == null || name == null || email == null || password == null) {
                return Response.error("Thiếu thông tin đăng ký bắt buộc");
            }

            // Kiểm tra trùng
            if (customerDAO.findByPhone(phone) != null) return Response.error("Số điện thoại này đã được đăng ký");
            if (customerDAO.findByEmail(email) != null) return Response.error("Email này đã được sử dụng");

            KhachHang kh = new KhachHang();
            kh.setMaKH(phone);
            kh.setSoDT(phone);
            kh.setTenKH(name);
            kh.setEmail(email);
            kh.setDiaChi(diaChi);
            kh.setMatKhau(password);
            kh.setNgayDangKy(LocalDate.now());
            kh.setThanhVien("Member");
            kh.setDiemTichLuy(0);
            
            customerDAO.insert(kh);
            System.out.println("[AUTH] Đăng ký khách hàng thành công: " + phone);
            return Response.ok("Đăng ký tài khoản thành công!");
        } catch (Exception e) {
            return Response.error("Lỗi đăng ký: " + e.getMessage());
        }
    }

    /**
     * Đăng nhập khách hàng (Dùng Email)
     */
    public Response handleCustomerLogin(Request request) {
        try {
            String email = (String) request.getParam("email");
            String password = (String) request.getParam("password");

            KhachHang kh = customerDAO.findByEmail(email);
            if (kh == null) return Response.error("Email không tồn tại");

            if (kh.getMatKhau().equals(password)) {
                // Tạo token cho khách hàng (Role: Customer)
                String token = ServerSessionService.createSession(kh.getMaKH(), "Customer");
                
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("customer", kh);
                
                System.out.println("[AUTH] KH đăng nhập thành công: " + email + " (Token: " + token + ")");
                return Response.ok(data);
            } else {
                return Response.error("Mật khẩu không chính xác");
            }
        } catch (Exception e) {
            return Response.error("Lỗi đăng nhập: " + e.getMessage());
        }
    }
}