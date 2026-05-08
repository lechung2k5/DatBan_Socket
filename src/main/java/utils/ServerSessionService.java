package utils;
import db.EnvConfig;
import db.RedisConfig;
import utils.JsonUtil;
import redis.clients.jedis.Jedis;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
public class ServerSessionService {
    private static final int TOKEN_TTL = EnvConfig.sessionTtlSeconds();
    private static final String PREFIX  = "session:";
    // 🔥 FALLBACK: In-memory session store if Redis is unavailable
    private static final Map<String, String> inMemorySessions = new ConcurrentHashMap<>();
    public static String createSession(String employeeId, String role) {
        String token = UUID.randomUUID().toString();
        String data  = JsonUtil.toJson(Map.of(
        "employeeId", employeeId,
        "role",       role,
        "loginAt",    LocalDateTime.now().toString()
        ));
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.setex(PREFIX + token, TOKEN_TTL, data);
            System.out.println("[SESSION] Created in Redis: " + token);
        } catch (Exception e) {
        inMemorySessions.put(token, data);
        System.err.println("[SESSION] Redis unavailable, using in-memory: " + token);
    }
    return token;
}
public static boolean isValid(String token) {
    if (token == null || token.isBlank()) return false;
    try (Jedis jedis = RedisConfig.getPool().getResource()) {
        return jedis.exists(PREFIX + token);
    } catch (Exception e) {
    return inMemorySessions.containsKey(token);
}
}
    public static void invalidate(String token) {
        if (token == null || token.isBlank()) return;
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            jedis.del(PREFIX + token);
        } catch (Exception e) {
            inMemorySessions.remove(token);
        }
    }

    public static String getSessionData(String token) {
        if (token == null || token.isBlank()) return null;
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            return jedis.get(PREFIX + token);
        } catch (Exception e) {
            return inMemorySessions.get(token);
        }
    }
}