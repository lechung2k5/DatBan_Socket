package service;
import entity.NhanVien;
import dao.EmployeeDAO;
import utils.JsonUtil;
import network.CommandType;
import network.Request;
import network.Response;
import java.util.List;
/**
* EmployeeService - Quản lý há» sơ nhân viên
*/
public class EmployeeService {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    public Response handleGetAll(Request request) {
        try {
            List<NhanVien> list = employeeDAO.findAll();
            return Response.ok(list);
        } catch (Exception e) {
        System.err.println("[EmployeeService] Lá»i handleGetAll: " + e.getMessage());
        return Response.error("Lá»i láº¥y danh sách nhân viên: " + e.getMessage());
    }
}
public Response handleUpdate(Request request) {
    try {
        NhanVien nv = JsonUtil.convertValue(request.getParam("employee"), NhanVien.class);
        if (nv == null) {
            return Response.error("Dữ liệu nhân viên không hợp lệ");
        }
        employeeDAO.upsert(nv);
        network.Service.broadcast(new network.RealTimeEvent(CommandType.UPDATE_EMPLOYEE, "[EMPLOYEE]:" + nv.getMaNV()));
        System.out.println("[EmployeeService] Đã cập nhật nhân viên: " + nv.getMaNV());
        return Response.ok("Thao tác nhân viên thành công");
    } catch (Exception e) {
    System.err.println("[EmployeeService] Lỗi handleUpdate: " + e.getMessage());
    return Response.error("Lỗi cập nhật nhân viên: " + e.getMessage());
}
}
}