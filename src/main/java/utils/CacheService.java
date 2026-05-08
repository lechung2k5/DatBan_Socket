package utils;
import db.RedisConfig;
import redis.clients.jedis.Jedis;
/**
* CacheService Ã¢Â€Â” Quáº£n lÃ½ toÃƒÂ n bÃ¡Â»Â™ cache Redis cho Server.
*
* Key patterns & TTL (theo REBUILD.md):
*   cache:menu    Ã¢Â†Â’ 5 phÃºt  (300s)  Ã¢Â€Â” danh sÃ¡ch mÃ³n Ã„Âƒn
*   cache:tables  Ã¢Â†Â’ 30 giÃ¢y (30s)   Ã¢Â€Â” tráº¡ng thÃ¡i bÃƒÂ n Ã„Âƒn
*   cache:promos  Ã¢Â†Â’ 10 phÃºt (600s)  Ã¢Â€Â” danh sÃ¡ch Æ°u Ã„Â‘Ã£i (ÃƒÂ­t thay Ã„Â‘Ã¡Â»Â•i)
*
* MÃ¡Â»Âi method Ã„Â‘Ã¡Â»Âu cÃ³ try-catch Ã„Â‘Ã¡Â»Âƒ khÃ´ng lÃƒÂ m crash Server khi Redis giÃ¡n Ã„Â‘oáº¡n.
* Náº¿u Redis lÃ¡Â»Â—i: get Ã¢Â†Â’ tráº£ null (Server sÃ¡ÂºÂ½ fallback vÃ¡Â»Â DynamoDB), set/invalidate Ã¢Â†Â’ bÃ¡Â»Â qua.
*/
public class CacheService {
    // Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€ Menu cache (TTL: 5 phÃºt) Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€
    private static final String KEY_MENU   = "cache:menu";
    private static final int    TTL_MENU   = 300;
    public static void setMenu(String json) {
        try (Jedis j = RedisConfig.getPool().getResource()) {
            j.setex(KEY_MENU, TTL_MENU, json);
            System.out.println("[CACHE] SET cache:menu (TTL " + TTL_MENU + "s)");
        } catch (Exception e) {
        System.err.println("[CACHE] LÃ¡Â»Â—i setMenu: " + e.getMessage());
    }
}
public static String getMenu() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        String val = j.get(KEY_MENU);
        System.out.println("[CACHE] GET cache:menu Ã¢Â†Â’ " + (val != null ? "HIT" : "MISS"));
        return val;
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i getMenu: " + e.getMessage());
    return null;
}
}
public static void invalidateMenu() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.del(KEY_MENU);
        System.out.println("[CACHE] INVALIDATE cache:menu");
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i invalidateMenu: " + e.getMessage());
}
}
// Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€ Tables cache (TTL: 30 giÃ¢y) Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€
private static final String KEY_TABLES = "cache:tables";
private static final int    TTL_TABLES = 30;
public static void setTables(String json) {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.setex(KEY_TABLES, TTL_TABLES, json);
        System.out.println("[CACHE] SET cache:tables (TTL " + TTL_TABLES + "s)");
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i setTables: " + e.getMessage());
}
}
public static String getTables() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        String val = j.get(KEY_TABLES);
        System.out.println("[CACHE] GET cache:tables Ã¢Â†Â’ " + (val != null ? "HIT" : "MISS"));
        return val;
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i getTables: " + e.getMessage());
    return null;
}
}
public static void invalidateTables() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.del(KEY_TABLES);
        System.out.println("[CACHE] INVALIDATE cache:tables");
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i invalidateTables: " + e.getMessage());
}
}
// Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€ Promos cache (TTL: 10 phÃºt) Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€
private static final String KEY_PROMOS = "cache:promos";
private static final int    TTL_PROMOS = 600;
public static void setPromos(String json) {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.setex(KEY_PROMOS, TTL_PROMOS, json);
        System.out.println("[CACHE] SET cache:promos (TTL " + TTL_PROMOS + "s)");
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i setPromos: " + e.getMessage());
}
}
public static String getPromos() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        String val = j.get(KEY_PROMOS);
        System.out.println("[CACHE] GET cache:promos Ã¢Â†Â’ " + (val != null ? "HIT" : "MISS"));
        return val;
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i getPromos: " + e.getMessage());
    return null;
}
}
public static void invalidatePromos() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.del(KEY_PROMOS);
        System.out.println("[CACHE] INVALIDATE cache:promos");
    } catch (Exception e) {
    System.err.println("[CACHE] LÃ¡Â»Â—i invalidatePromos: " + e.getMessage());
}
}
// Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€ Invalidate ALL cache (dÃ¹ng khi cÃ¡ÂºÂ§n reset toÃƒÂ n bÃ¡Â»Â™) Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€Ã¢Â”Â€
public static void invalidateAll() {
    invalidateMenu();
    invalidateTables();
    invalidatePromos();
    System.out.println("[CACHE] Ã„ÂÃ£ xÃ³a toÃ n bá»™ cache.");
}

    public static void publishNotification(String json) {
        try (redis.clients.jedis.Jedis j = db.RedisConfig.getPool().getResource()) {
            j.publish("notifications", json);
            System.out.println("[CACHE] PUBLISH notifications to Redis");
        } catch (Exception e) {
            System.err.println("[CACHE] Lỗi publishNotification: " + e.getMessage());
        }
    }
}
