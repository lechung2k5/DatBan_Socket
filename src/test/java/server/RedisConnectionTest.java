package server;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import utils.ServerSessionService;
import db.RedisConfig;

import static org.junit.jupiter.api.Assertions.*;

public class RedisConnectionTest {

    @Test
    public void testRedisConnection() {
        System.out.println("--- Test: Kết nối Redis ---");
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            String pong = jedis.ping();
            System.out.println("[REDIS PING] => " + pong);
            assertEquals("PONG", pong, "Redis phải trả về PONG khi ping");
        } catch (Exception e) {
            fail("Không kết nối được Redis: " + e.getMessage());
        }
    }

    @Test
    public void testSessionLifecycle() {
        System.out.println("--- Test: Session lifecycle (Login -> Validate -> Logout) ---");

        // 1. Tạo session (mô phỏng login)
        String token = ServerSessionService.createSession("emp001", "Thu ngân");
        assertNotNull(token);
        System.out.println("[SESSION] Token created: " + token);

        // 2. Kiểm tra token hợp lệ
        assertTrue(ServerSessionService.isValid(token), "Token vừa tạo phải hợp lệ");
        System.out.println("[SESSION] isValid => true ✓");

        // 3. Kiểm tra token giả
        assertFalse(ServerSessionService.isValid("fake-token-xyz"), "Token giả không được hợp lệ");
        System.out.println("[SESSION] isValid(fake) => false ✓");

        // 4. Xóa session (mô phỏng logout)
        ServerSessionService.invalidate(token);
        assertFalse(ServerSessionService.isValid(token), "Token sau logout phải không còn hợp lệ");
        System.out.println("[SESSION] Sau logout => false ✓");
    }

    @Test
    public void testCacheService() {
        System.out.println("--- Test: Redis Cache Set/Get/Invalidate ---");
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            // Set
            jedis.setex("test:cache:key", 60, "hello-redis-cloud");

            // Get
            String value = jedis.get("test:cache:key");
            assertEquals("hello-redis-cloud", value);
            System.out.println("[CACHE] Get: " + value + " ✓");

            // Cleanup
            jedis.del("test:cache:key");
            assertNull(jedis.get("test:cache:key"));
            System.out.println("[CACHE] Sau del: null ✓");
        }
    }
}
