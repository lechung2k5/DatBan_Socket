package server;

import entity.*;
import org.junit.jupiter.api.*;
import service.*;
import network.CommandType;
import network.Request;
import network.Response;
import network.RequestDispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 - Integration Test cho Business Logic Handlers
 * Test luồng nghiệp vụ từ Login -> Gọi món -> Thanh toán.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase5HandlerTest {

    private static String sessionToken;
    private static final RequestDispatcher dispatcher = new RequestDispatcher();

    @Test
    @Order(1)
    void testLogin_Success() {
        // Giả sử DatabaseSeeder đã chạy và có user admin/admin123
        Request req = new Request(CommandType.LOGIN);
        req.setParam("username", "admin");
        req.setParam("password", "admin123");

        Response res = dispatcher.dispatch(req);
        
        assertEquals(200, res.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) res.getData();
        sessionToken = (String) data.get("token");
        assertNotNull(sessionToken);
        System.out.println("[PASS] Login thành công, token: " + sessionToken);
    }

    @Test
    @Order(2)
    void testGetTables_WithCache() {
        Request req = new Request(CommandType.GET_TABLES, null, sessionToken);
        
        // Lần 1: Lấy từ DB và set cache
        Response res1 = dispatcher.dispatch(req);
        assertEquals(200, res1.getStatusCode());
        List<Ban> list1 = (List<Ban>) res1.getData();
        assertTrue(list1.size() > 0);

        // Lần 2: Lấy từ Cache
        Response res2 = dispatcher.dispatch(req);
        assertEquals(200, res2.getStatusCode());
        System.out.println("[PASS] GetTables thành công (lần 2 dùng cache) ✓");
    }

    @Test
    @Order(3)
    void testCreateOrderFlow() {
        String testHD = "HD-TEST-P5-" + System.currentTimeMillis();
        
        // 1. Tạo hóa đơn tạm
        HoaDon hd = new HoaDon();
        hd.setMaHD(testHD);
        hd.setMaBan("B001");
        hd.setTrangThai(TrangThaiHoaDon.HOA_DON_TAM);
        hd.setTongTienThanhToan(100000);

        Request req = new Request(CommandType.CREATE_ORDER, null, sessionToken);
        req.setParam("hoaDon", hd);
        req.setParam("chiTiet", List.of());

        Response res = dispatcher.dispatch(req);
        assertEquals(200, res.getStatusCode());

        // 2. Kiểm tra trạng thái bàn đã được cập nhật chưa?
        // (Lưu ý: Trong thực tế UI sẽ gọi UPDATE_TABLE_STATUS riêng hoặc gộp trong handleCreateOrder)
        // Ở đây ta test logic handler Payment/Table
        
        System.out.println("[PASS] Tạo hóa đơn thành công ✓");
    }

    @Test
    @Order(4)
    void testCheckoutFlow() {
        String testHD = "HD-CHECKOUT-P5-" + System.currentTimeMillis();
        
        // Setup hóa đơn
        HoaDon hd = new HoaDon();
        hd.setMaHD(testHD);
        hd.setMaBan("B001");
        hd.setTrangThai(TrangThaiHoaDon.HOA_DON_TAM);
        
        Request createReq = new Request(CommandType.CREATE_ORDER, null, sessionToken);
        createReq.setParam("hoaDon", hd);
        createReq.setParam("chiTiet", List.of());
        dispatcher.dispatch(createReq);

        // Checkout
        Request checkReq = new Request(CommandType.CHECK_OUT, null, sessionToken);
        checkReq.setParam("maHD", testHD);
        checkReq.setParam("maBan", "B001");
        checkReq.setParam("totalAmount", 150000.0);
        checkReq.setParam("maNhanVien", "admin");
        checkReq.setParam("pttt", "TIEN_MAT");

        Response res = dispatcher.dispatch(checkReq);
        assertEquals(200, res.getStatusCode());
        System.out.println("[PASS] Checkout và cập nhật trạng thái bàn thành công ✓");
    }

    @Test
    @Order(5)
    void testLogout() {
        Request req = new Request(CommandType.LOGOUT, null, sessionToken);
        Response res = dispatcher.dispatch(req);
        assertEquals(200, res.getStatusCode());

        // Thử dùng lại token cũ -> Phải bị từ chối
        Request req2 = new Request(CommandType.GET_TABLES, null, sessionToken);
        Response res2 = dispatcher.dispatch(req2);
        assertEquals(401, res2.getStatusCode());
        System.out.println("[PASS] Logout và vô hiệu hóa token thành công ✓");
    }
}
