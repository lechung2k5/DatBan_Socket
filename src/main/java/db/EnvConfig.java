package db;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * EnvConfig - Quản lý cấu hình từ file .env
 */
public class EnvConfig {
    private static Dotenv dotenv;

    private static Dotenv getInstance() {
        if (dotenv == null) {
            try {
                dotenv = Dotenv.configure()
                        .directory("./")
                        .ignoreIfMissing()
                        .load();
                System.out.println("[EnvConfig] .env loaded from " + System.getProperty("user.dir"));
            } catch (Exception e) {
                System.err.println("[EnvConfig] Warning: Could not load .env: " + e.getMessage());
            }
        }
        return dotenv;
    }

    public static String get(String key) {
        return getInstance().get(key);
    }

    public static String get(String key, String defaultValue) {
        return getInstance().get(key, defaultValue);
    }

    public static int getInt(String key) {
        String val = getInstance().get(key);
        return val != null ? Integer.parseInt(val) : 0;
    }

    public static int getInt(String key, int defaultValue) {
        String val = getInstance().get(key);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    // --- Shortcuts cho từng nhóm config ---

    // AWS
    public static String awsAccessKey()  { return get("AWS_ACCESS_KEY_ID"); }
    public static String awsSecretKey()  { return get("AWS_SECRET_ACCESS_KEY"); }
    public static String awsRegion()     { return get("AWS_REGION", "ap-southeast-1"); }

    // Redis
    public static String redisHost()     { return get("REDIS_HOST", "localhost"); }
    public static int    redisPort()     { return getInt("REDIS_PORT", 6379); }
    public static String redisPassword() { return get("REDIS_PASSWORD", null); }

    // Server
    public static String serverHost()    { return get("SERVER_HOST", "localhost"); }
    public static int    serverPort()    { return getInt("SERVER_PORT", 8888); }

    // Session
    public static int sessionTtlSeconds() {
        return getInt("SESSION_TTL_HOURS", 8) * 3600;
    }

    // Cloudinary
    public static String cloudinaryCloudName() { return get("CLOUDINARY_CLOUD_NAME"); }
    public static String cloudinaryApiKey()    { return get("CLOUDINARY_API_KEY"); }
    public static String cloudinaryApiSecret() { return get("CLOUDINARY_API_SECRET"); }

    // Mail
    public static String mailUsername() { return get("MAIL_USERNAME"); }
    public static String mailPassword() { return get("MAIL_PASSWORD"); }
}