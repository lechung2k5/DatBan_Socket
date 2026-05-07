package service;
import entity.KhachHang;
import dao.CustomerDAO;
import utils.JsonUtil;
import network.Request;
import network.Response;
/**
* CustomerService - Quản lý há» sơ khách hÃ ng
*/
public class CustomerService {
    private final CustomerDAO customerDAO = new CustomerDAO();
    public Response handleFindByPhone(Request request) {
        try {
            String sdt = (String) request.getParam("phone");
            if (sdt == null || sdt.isEmpty()) sdt = (String) request.getParam("sdt");
            if (sdt == null || sdt.isEmpty()) return Response.error("Số điện thoại không hợp lệ");
            KhachHang kh = customerDAO.findByPhone(sdt);
            if (kh != null) return Response.ok(kh);
            return Response.error("Không tìm thấy khách hàng");
        } catch (Exception e) {
        System.err.println("[CustomerService] Lá»i handleFindByPhone: " + e.getMessage());
        return Response.error("Lá»i: " + e.getMessage());
    }
}
public Response handleCreate(Request request) {
    try {
        KhachHang kh = JsonUtil.convertValue(request.getParam("khachHang"), KhachHang.class);
        if (kh == null || kh.getSoDT() == null) {
            return Response.error("Dữ liá»u khách hÃ ng không há»£p lá»");
        }
        KhachHang existing = customerDAO.findByPhone(kh.getSoDT());
        if (existing != null) return Response.ok(existing);
        customerDAO.insert(kh);
            network.Service.broadcast(new network.RealTimeEvent(network.CommandType.UPDATE_CUSTOMER, "Thêm khách hàng", kh.getSoDT()));
            return Response.ok(kh);
    } catch (Exception e) {
    System.err.println("[CustomerService] Lá»i handleCreate: " + e.getMessage());
    return Response.error("Lá»i thêm khách: " + e.getMessage());
}
}
public Response handleUpdate(Request request) {
    try {
        KhachHang kh = JsonUtil.convertValue(request.getParam("khachHang"), KhachHang.class);
        customerDAO.update(kh);
            network.Service.broadcast(new network.RealTimeEvent(network.CommandType.UPDATE_CUSTOMER, "Cập nhật khách hàng", kh.getSoDT()));
            return Response.ok("Cập nhật thông tin khách hàng thành công");
    } catch (Exception e) {
    return Response.error("Lá»i cáº­p nháº­t khách hÃ ng: " + e.getMessage());
}
}
public Response handleGetAll(Request request) {
    try {
        return Response.ok(customerDAO.findAll());
    } catch (Exception e) {
    return Response.error("Lá»i láº¥y danh sách khách hÃ ng: " + e.getMessage());
}
}
public Response handleDelete(Request request) {
    try {
        String sdt = (String) request.getParam("sdt");
        customerDAO.delete(sdt);
            network.Service.broadcast(new network.RealTimeEvent(network.CommandType.UPDATE_CUSTOMER, "Xóa khách hàng", sdt));
            return Response.ok("Xóa khách hàng thành công");
    } catch (Exception e) {
    return Response.error("Lá»i xóa khách hàng: " + e.getMessage());
}
}
}
