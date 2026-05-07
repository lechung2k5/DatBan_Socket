package server;

import network.Client;
import network.Service;
import utils.ClientSessionManager;
import entity.NhanVien;
import org.junit.jupiter.api.*;
import network.CommandType;
import network.Response;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6 - Integration Test: Client-Server Communication via Socket
 * Kiểm tra kết nối Socket và luồng Login qua Client.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase6IntegrationTest {

    private static Thread serverThread;

    @BeforeAll
    static void startServer() {
        serverThread = new Thread(() -> {
            try {
                Service.main(new String[]{});
            } catch (Exception e) {
                // Ignore if server already running or port in use
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        
        // Wait for server to start
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
    }

    @AfterAll
    static void tearDown() {
        Service.stop();
    }

    @Test
    @Order(1)
    void testApiClient_Login_Success() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "admin123");

        Response res = Client.sendWithParams(CommandType.LOGIN, params);
        
        assertEquals(200, res.getStatusCode());
        assertNotNull(res.getData());
        
        // Luu session de test tiep
        Map<String, Object> data = (Map<String, Object>) res.getData();
        ClientSessionManager.getInstance().login((String) data.get("token"), (NhanVien) data.get("employee"));
        
        System.out.println("[PASS] Phase 6: Client connected and logged in successfully ✓");
    }

    @Test
    @Order(2)
    void testApiClient_InvalidLogin() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "wrong");
        params.put("password", "wrong");

        Response res = Client.sendWithParams(CommandType.LOGIN, params);
        
        assertNotEquals(200, res.getStatusCode());
        System.out.println("[PASS] Phase 6: Client correctly handled invalid login ✓");
    }
}
