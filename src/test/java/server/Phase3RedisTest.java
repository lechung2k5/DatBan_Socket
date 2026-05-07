package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CacheService;
import utils.ServerSessionService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3  Redis Session & Cache Test
 *
 * Checklist theo REBUILD.md:
 *  ✅ ServerSessionService: createSession, isValid, invalidate
 *  ✅ CacheService: set/get/invalidate cho menu (300s) và tables (30s)
 *  ✅ CacheService: promos (600s)
 *  ✅ invalidateAll()
 *  ✅ Cache strategy: invalidate khi data thay đổi
 */
public class Phase3RedisTest {

    @BeforeEach
    void cleanup() {
        // Dọn sạch cache trước mỗi test
        CacheService.invalidateAll();
    }

    // ─── SESSION ─────────────────────────────────────────────────────────────

    @Test
    void testSessionCreateAndValidate() {
        String token = ServerSessionService.createSession("NV001", "Thu ngân");
        assertNotNull(token, "Token không được null");
        assertTrue(ServerSessionService.isValid(token), "Token vừa tạo phải hợp lệ");
        System.out.println("[PASS] Session created & valid: " + token);

        // Cleanup
        ServerSessionService.invalidate(token);
    }

    @Test
    void testSessionInvalidateOnLogout() {
        String token = ServerSessionService.createSession("NV002", "Quản lý");
        assertTrue(ServerSessionService.isValid(token));

        ServerSessionService.invalidate(token);
        assertFalse(ServerSessionService.isValid(token), "Token sau logout phải không hợp lệ");
        System.out.println("[PASS] Session invalidated after logout");
    }

    @Test
    void testFakeTokenIsInvalid() {
        assertFalse(ServerSessionService.isValid(null), "null không hợp lệ");
        assertFalse(ServerSessionService.isValid(""), "chuỗi rỗng không hợp lệ");
        assertFalse(ServerSessionService.isValid("fake-token-xyz-999"), "token giả không hợp lệ");
        System.out.println("[PASS] Fake/null tokens rejected correctly");
    }

    // ─── CACHE: MENU ─────────────────────────────────────────────────────────

    @Test
    void testMenuCacheHitAndMiss() {
        // MISS khi chưa có cache
        assertNull(CacheService.getMenu(), "Lần đầu phải MISS");
        System.out.println("[PASS] Menu MISS trước khi set");

        // Set cache
        String fakeMenu = "[{\"itemId\":\"FOOD001\",\"name\":\"Phở Bò\",\"price\":55000}]";
        CacheService.setMenu(fakeMenu);

        // HIT
        String cached = CacheService.getMenu();
        assertNotNull(cached, "Sau khi set phải HIT");
        assertEquals(fakeMenu, cached);
        System.out.println("[PASS] Menu HIT sau khi set: " + cached);
    }

    @Test
    void testMenuCacheInvalidate() {
        CacheService.setMenu("[{\"itemId\":\"FOOD002\",\"name\":\"Cơm tấm\"}]");
        assertNotNull(CacheService.getMenu());

        // Mô phỏng: thêm/sửa món ăn → invalidate cache cũ
        CacheService.invalidateMenu();
        assertNull(CacheService.getMenu(), "Sau invalidate phải MISS");
        System.out.println("[PASS] Menu cache invalidated correctly");
    }

    // ─── CACHE: TABLES ───────────────────────────────────────────────────────

    @Test
    void testTablesCacheHitAndMiss() {
        assertNull(CacheService.getTables(), "Lần đầu phải MISS");

        String fakeTables = "[{\"tableId\":\"B001\",\"status\":\"Trống\"},{\"tableId\":\"B002\",\"status\":\"Đang dùng\"}]";
        CacheService.setTables(fakeTables);

        String cached = CacheService.getTables();
        assertNotNull(cached);
        assertEquals(fakeTables, cached);
        System.out.println("[PASS] Tables HIT: " + cached);
    }

    @Test
    void testTablesCacheInvalidateOnCheckout() {
        CacheService.setTables("[{\"tableId\":\"B001\",\"status\":\"Đang dùng\"}]");
        assertNotNull(CacheService.getTables());

        // Mô phỏng: checkout xong → bàn trống → invalidate cache
        CacheService.invalidateTables();
        assertNull(CacheService.getTables(), "Sau checkout phải MISS tables cache");
        System.out.println("[PASS] Tables cache invalidated on checkout ✓");
    }

    // ─── CACHE: PROMOS ───────────────────────────────────────────────────────

    @Test
    void testPromosCacheHitAndMiss() {
        assertNull(CacheService.getPromos(), "Lần đầu phải MISS");

        String fakePromos = "[{\"maUuDai\":\"UD001\",\"giamGia\":10}]";
        CacheService.setPromos(fakePromos);

        String cached = CacheService.getPromos();
        assertNotNull(cached);
        assertEquals(fakePromos, cached);
        System.out.println("[PASS] Promos HIT: " + cached);
    }

    // ─── CACHE STRATEGY ──────────────────────────────────────────────────────

    @Test
    void testInvalidateAllClearsEverything() {
        CacheService.setMenu("[\"menu\"]");
        CacheService.setTables("[\"tables\"]");
        CacheService.setPromos("[\"promos\"]");

        CacheService.invalidateAll();

        assertNull(CacheService.getMenu(), "Menu phải null sau invalidateAll");
        assertNull(CacheService.getTables(), "Tables phải null sau invalidateAll");
        assertNull(CacheService.getPromos(), "Promos phải null sau invalidateAll");
        System.out.println("[PASS] invalidateAll() xóa sạch toàn bộ cache ✓");
    }
}
