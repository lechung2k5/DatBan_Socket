package server;

import network.Client;
import network.Service;
import utils.ClientSessionManager;
import entity.Ban;
import entity.HoaDon;
import entity.NhanVien;
import entity.TrangThaiHoaDon;
import org.junit.jupiter.api.*;
import network.CommandType;
import network.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7 - End-to-End Integration Test
 * Kiểm tra toàn bộ luồng nghiệp vụ qua Socket: Login -> Get Tables -> Create Order -> Checkout.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase7FullFlowTest {

    private static String testHD;

    @BeforeAll
    static void startServer() {
        new Thread(() -> {
            try {
                Service.main(new String[]{});
            } catch (Exception e) {}
        }).start();
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
    }

    @AfterAll
    static void tearDown() {
        Service.stop();
    }

    @Test
    @Order(1)
    @DisplayName("1. Đăng nhập hệ thống")
    void step1_Login() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "admin123");

        Response res = Client.sendWithParams(CommandType.LOGIN, params);
        assertEquals(200, res.getStatusCode());
        
        Map<String, Object> data = (Map<String, Object>) res.getData();
        ClientSessionManager.getInstance().login((String) data.get("token"), (NhanVien) data.get("employee"));
        assertNotNull(ClientSessionManager.getInstance().getToken());
    }

    @Test
    @Order(2)
    @DisplayName("2. Lấy danh sách bàn")
    void step2_GetTables() {
        Response res = Client.send(CommandType.GET_TABLES, null);
        assertEquals(200, res.getStatusCode());
        List<Ban> tables = (List<Ban>) res.getData();
        assertNotNull(tables);
        assertTrue(tables.size() > 0);
        System.out.println("[PASS] Lấy danh sách bàn thành công: " + tables.size() + " bàn");
    }

    @Test
    @Order(3)
    @DisplayName("3. Tạo hóa đơn mới")
    void step3_CreateOrder() {
        testHD = "HD-E2E-" + System.currentTimeMillis();
        HoaDon hd = new HoaDon();
        hd.setMaHD(testHD);
        hd.setMaBan("B01");
        hd.setTrangThai(TrangThaiHoaDon.HOA_DON_TAM);
        hd.setTongTienThanhToan(50000);

        Map<String, Object> params = new HashMap<>();
        params.put("hoaDon", hd);
        params.put("chiTiet", List.of());

        Response res = Client.sendWithParams(CommandType.CREATE_ORDER, params);
        assertEquals(200, res.getStatusCode());
        System.out.println("[PASS] Tạo hóa đơn " + testHD + " thành công ✓");
    }

    @Test
    @Order(4)
    @DisplayName("4. Thanh toán")
    void step4_Checkout() {
        Map<String, Object> params = new HashMap<>();
        params.put("maHD", testHD);
        params.put("maBan", "B01");
        params.put("totalAmount", 75000.0);
        params.put("maNhanVien", "admin");
        params.put("pttt", "TIEN_MAT");

        Response res = Client.sendWithParams(CommandType.CHECK_OUT, params);
        assertEquals(200, res.getStatusCode());
        System.out.println("[PASS] Thanh toán và giải phóng bàn thành công ✓");
    }

    @Test
    @Order(5)
    @DisplayName("5. Đăng xuất")
    void step5_Logout() {
        Response res = Client.send(CommandType.LOGOUT, null);
        assertEquals(200, res.getStatusCode());
        
        // Thử gọi lại lệnh cần token -> Phải bị 401
        Response res2 = Client.send(CommandType.GET_TABLES, null);
        assertEquals(401, res2.getStatusCode());
        System.out.println("[PASS] Đăng xuất và vô hiệu hóa token thành công ✓");
    }
}
