package utils;
import db.RedisConfig;
import redis.clients.jedis.Jedis;
/**
* CacheService â Quản lý toÃ n bá» cache Redis cho Server.
*
* Key patterns & TTL (theo REBUILD.md):
*   cache:menu    â 5 phút  (300s)  â danh sách món Än
*   cache:tables  â 30 giây (30s)   â trạng thái bÃ n Än
*   cache:promos  â 10 phút (600s)  â danh sách ưu Äãi (Ã­t thay Äá»i)
*
* Má»i method Äá»u có try-catch Äá» không lÃ m crash Server khi Redis gián Äoạn.
* Nếu Redis lá»i: get â trả null (Server sáº½ fallback vá» DynamoDB), set/invalidate â bá» qua.
*/
public class CacheService {
    // âââ Menu cache (TTL: 5 phút) ââââââââââââââââââââââââââââââââââââââââââââ
    private static final String KEY_MENU   = "cache:menu";
    private static final int    TTL_MENU   = 300;
    public static void setMenu(String json) {
        try (Jedis j = RedisConfig.getPool().getResource()) {
            j.setex(KEY_MENU, TTL_MENU, json);
            System.out.println("[CACHE] SET cache:menu (TTL " + TTL_MENU + "s)");
        } catch (Exception e) {
        System.err.println("[CACHE] Lá»i setMenu: " + e.getMessage());
    }
}
public static String getMenu() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        String val = j.get(KEY_MENU);
        System.out.println("[CACHE] GET cache:menu â " + (val != null ? "HIT" : "MISS"));
        return val;
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i getMenu: " + e.getMessage());
    return null;
}
}
public static void invalidateMenu() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.del(KEY_MENU);
        System.out.println("[CACHE] INVALIDATE cache:menu");
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i invalidateMenu: " + e.getMessage());
}
}
// âââ Tables cache (TTL: 30 giây) ââââââââââââââââââââââââââââââââââââââââ
private static final String KEY_TABLES = "cache:tables";
private static final int    TTL_TABLES = 30;
public static void setTables(String json) {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.setex(KEY_TABLES, TTL_TABLES, json);
        System.out.println("[CACHE] SET cache:tables (TTL " + TTL_TABLES + "s)");
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i setTables: " + e.getMessage());
}
}
public static String getTables() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        String val = j.get(KEY_TABLES);
        System.out.println("[CACHE] GET cache:tables â " + (val != null ? "HIT" : "MISS"));
        return val;
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i getTables: " + e.getMessage());
    return null;
}
}
public static void invalidateTables() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.del(KEY_TABLES);
        System.out.println("[CACHE] INVALIDATE cache:tables");
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i invalidateTables: " + e.getMessage());
}
}
// âââ Promos cache (TTL: 10 phút) âââââââââââââââââââââââââââââââââââââââââ
private static final String KEY_PROMOS = "cache:promos";
private static final int    TTL_PROMOS = 600;
public static void setPromos(String json) {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.setex(KEY_PROMOS, TTL_PROMOS, json);
        System.out.println("[CACHE] SET cache:promos (TTL " + TTL_PROMOS + "s)");
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i setPromos: " + e.getMessage());
}
}
public static String getPromos() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        String val = j.get(KEY_PROMOS);
        System.out.println("[CACHE] GET cache:promos â " + (val != null ? "HIT" : "MISS"));
        return val;
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i getPromos: " + e.getMessage());
    return null;
}
}
public static void invalidatePromos() {
    try (Jedis j = RedisConfig.getPool().getResource()) {
        j.del(KEY_PROMOS);
        System.out.println("[CACHE] INVALIDATE cache:promos");
    } catch (Exception e) {
    System.err.println("[CACHE] Lá»i invalidatePromos: " + e.getMessage());
}
}
// âââ Invalidate ALL cache (dùng khi cáº§n reset toÃ n bá») ââââââââââââââââââ
public static void invalidateAll() {
    invalidateMenu();
    invalidateTables();
    invalidatePromos();
    System.out.println("[CACHE] Äã xóa toàn bộ cache.");
}
}