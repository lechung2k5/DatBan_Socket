package db;
import db.EnvConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
public class RedisConfig {
    private static JedisPool jedisPool;
    public static synchronized JedisPool getPool() {
        if (jedisPool == null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);
            String host     = EnvConfig.redisHost();
            int    port     = EnvConfig.redisPort();
            String password = EnvConfig.redisPassword();
            int    timeout  = 3000; // ms
            if (password != null && !password.isBlank()) {
                // Redis Cloud: kết nối có password, KHÔNG dùng SSL (port này là plaintext)
                jedisPool = new JedisPool(poolConfig, host, port, timeout, password);
                System.out.println("[REDIS] Kết nối Redis Cloud: " + host + ":" + port + " (Auth, no SSL)");
            } else {
            // Redis local: không cần password
            jedisPool = new JedisPool(poolConfig, host, port);
            System.out.println("[REDIS] Kết nối Redis local: " + host + ":" + port);
        }
    }
    return jedisPool;
}
}