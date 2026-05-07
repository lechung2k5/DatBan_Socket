# 🔄 KẾ HOẠCH REBUILD PROJECT — Distributed Programming With Java
> Course: 2101558 | Nhóm 08 | IUH — FIT
> Stack: Java TCP Socket · AWS DynamoDB · Redis

---

## ⚠️ PHÂN TÍCH GAP: Project Hiện Tại vs Yêu Cầu Môn Học

| Tiêu chí | Project Hiện Tại | Yêu Cầu Môn Học | Trạng thái |
|---|---|---|---|
| Kiến trúc | Desktop App (offline, 1 máy) | Distributed App (nhiều máy qua mạng) | ❌ THIẾU |
| Database | SQL Server (JDBC) | **NoSQL database** | ❌ SAI |
| Giao tiếp mạng | Không có | **Java TCP Socket** | ❌ THIẾU |
| Session/Cache | Không có | Redis (token + cache) | ❌ THIẾU |
| Nhiều client | Không | Multi-client đồng thời | ❌ THIẾU |
| UI JavaFX | ✅ Có | Không bắt buộc framework | ✅ Giữ được |
| Nghiệp vụ nhà hàng | ✅ Rất đầy đủ | Đủ theo topic | ✅ Giữ được |
| Báo cáo | Có Word doc | Cần đúng cấu trúc 5 chương | ⚠️ Cần cập nhật |

---

## 🏗️ KIẾN TRÚC TỔNG THỂ

```
┌──────────────────────────────────────────────────────────────────┐
│                        LAN / localhost                           │
│                                                                  │
│  ┌─────────────┐                      ┌────────────────────────┐ │
│  │  Client 1   │                      │        SERVER          │ │
│  │ (Thu ngân)  │◄──── TCP Socket ────►│                        │ │
│  └─────────────┘     Request/Response │  RequestDispatcher     │ │
│                                       │  ├── AuthHandler       │ │
│  ┌─────────────┐                      │  ├── TableHandler      │ │
│  │  Client 2   │◄──── TCP Socket ────►│  ├── OrderHandler      │ │
│  │  (Quản lý)  │                      │  ├── PaymentHandler    │ │
│  └─────────────┘                      │  └── StatsHandler      │ │
│                                       └────────┬───────┬───────┘ │
│  ┌─────────────┐                               │       │         │
│  │  Client 3   │◄──── TCP Socket ────►         │       │         │
│  │   (Admin)   │                               ▼       ▼         │
│  └─────────────┘                    ┌────────────┐ ┌──────────┐  │
│                                     │    AWS     │ │  Redis   │  │
│                                     │  DynamoDB  │ │  Cache   │  │
│                                     │  (NoSQL)   │ │ +Session │  │
│                                     └────────────┘ └──────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### Vai trò từng thành phần

| Component | Công nghệ | Mục đích |
|---|---|---|
| Giao tiếp mạng | **Java TCP Socket** | Client ↔ Server qua LAN |
| Database chính | **AWS DynamoDB** | Lưu bàn, hóa đơn, menu, khách hàng |
| Session & Cache | **Redis** | Lưu token login, cache menu/bàn |
| UI | **JavaFX** (giữ nguyên) | Giao diện desktop client |
| Serialization | **Gson (JSON)** | Đóng gói Request/Response qua Socket |

---

## 📦 DEPENDENCIES MỚI — `pom.xml`

```xml
<!-- AWS DynamoDB SDK v2 -->
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>dynamodb</artifactId>
  <version>2.25.0</version>
</dependency>
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>dynamodb-enhanced</artifactId>
  <version>2.25.0</version>
</dependency>

<!-- Redis client (Jedis) -->
<dependency>
  <groupId>redis.clients</groupId>
  <artifactId>jedis</artifactId>
  <version>5.1.0</version>
</dependency>

<!-- Gson — JSON serialization cho Socket protocol -->
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.10.1</version>
</dependency>

<!-- dotenv — đọc biến môi trường từ file .env -->
<dependency>
  <groupId>io.github.cdimascio</groupId>
  <artifactId>dotenv-java</artifactId>
  <version>3.0.0</version>
</dependency>
```

---

## 📋 KẾ HOẠCH REBUILD CHI TIẾT

---

### PHASE 0 — Chuẩn bị & Thiết lập (Tuần 1)

**Mục tiêu:** Setup môi trường, tài khoản cloud, dependencies

#### AWS DynamoDB Setup
- [ ] Tạo tài khoản AWS Free Tier (nếu chưa có) tại https://aws.amazon.com/free
- [ ] Vào AWS Console → DynamoDB → Tạo các Tables:

| Table Name | Partition Key | Sort Key | Ghi chú |
|---|---|---|---|
| `Tables` | `tableId` (S) | — | Trạng thái bàn |
| `Invoices` | `invoiceId` (S) | `createdAt` (S) | Hóa đơn + embedded items |
| `MenuItems` | `categoryId` (S) | `itemId` (S) | Menu theo danh mục |
| `Customers` | `customerId` (S) | — | Khách hàng thành viên |
| `Employees` | `employeeId` (S) | — | Nhân viên + tài khoản |
| `Reservations` | `reservationId` (S) | `tableId` (S) | Đặt bàn trước |

- [ ] Tạo IAM User có quyền `AmazonDynamoDBFullAccess`, lưu `Access Key ID` + `Secret Access Key`
- [ ] Tạo file `.env` ở **root project** (KHÔNG commit lên Git):
  ```env
  # AWS DynamoDB
  AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY
  AWS_SECRET_ACCESS_KEY=YOUR_SECRET_KEY
  AWS_REGION=ap-southeast-1

  # Redis
  REDIS_HOST=localhost
  REDIS_PORT=6379

  # Server
  SERVER_HOST=localhost
  SERVER_PORT=9999

  # Session
  SESSION_TTL_HOURS=8
  ```
- [ ] Tạo file `.env.example` (ĐƯỢC commit — làm template cho cả nhóm):
  ```env
  AWS_ACCESS_KEY_ID=
  AWS_SECRET_ACCESS_KEY=
  AWS_REGION=ap-southeast-1
  REDIS_HOST=localhost
  REDIS_PORT=6379
  SERVER_HOST=localhost
  SERVER_PORT=9999
  SESSION_TTL_HOURS=8
  ```

#### Redis Setup
- [ ] Cài **Redis Stack** local: https://redis.io/docs/getting-started/install-redis/
  - Windows: dùng WSL2 hoặc Redis Windows port
  - macOS: `brew install redis`
  - Linux: `sudo apt install redis-server`
- [ ] Verify: `redis-cli ping` → trả về `PONG`
- [ ] Mặc định chạy ở `localhost:6379`

#### Project Setup
- [ ] Tạo nhánh Git mới: `feature/distributed-rebuild`
- [ ] Thêm dependencies vào `pom.xml`
- [ ] Thêm `.env` vào `.gitignore` ngay lập tức — commit `.env.example` thay thế
- [ ] Tạo `EnvConfig.java` — singleton đọc `.env` dùng cho toàn bộ project

**Thành viên phụ trách:** Trưởng nhóm

---


### EnvConfig.java — Singleton đọc `.env` toàn project

> Tạo ngay sau Phase 0. Tất cả class khác đọc config qua `EnvConfig` thay vì hardcode.

```
src/main/java/config/
└── EnvConfig.java
```

```java
public class EnvConfig {
    private static Dotenv dotenv;

    private static Dotenv getInstance() {
        if (dotenv == null) {
            dotenv = Dotenv.configure()
                .directory("./")        // Tìm .env ở root project
                .ignoreIfMissing()      // Không crash nếu chạy production với biến hệ thống
                .load();
        }
        return dotenv;
    }

    public static String get(String key) {
        return getInstance().get(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(getInstance().get(key));
    }

    // --- Shortcuts cho từng nhóm config ---

    // AWS
    public static String awsAccessKey()  { return get("AWS_ACCESS_KEY_ID"); }
    public static String awsSecretKey()  { return get("AWS_SECRET_ACCESS_KEY"); }
    public static String awsRegion()     { return get("AWS_REGION"); }

    // Redis
    public static String redisHost()     { return get("REDIS_HOST"); }
    public static int    redisPort()     { return getInt("REDIS_PORT"); }

    // Server
    public static String serverHost()    { return get("SERVER_HOST"); }
    public static int    serverPort()    { return getInt("SERVER_PORT"); }

    // Session
    public static int sessionTtlSeconds() {
        return getInt("SESSION_TTL_HOURS") * 3600;
    }
}
```

Mọi class trong project **chỉ dùng `EnvConfig`** để lấy config, không hardcode bất kỳ giá trị nào:

```java
// ❌ SAI — hardcode
new JedisPool(config, "localhost", 6379);

// ✅ ĐÚNG — đọc từ .env qua EnvConfig
new JedisPool(config, EnvConfig.redisHost(), EnvConfig.redisPort());
```

---

### PHASE 1 — Tầng Network Protocol (Tuần 2)

**Mục tiêu:** Thiết kế giao thức Request/Response qua TCP Socket

#### Cấu trúc package mới

```
src/main/java/
├── network/
│   ├── protocol/
│   │   ├── Request.java        ← Gói tin gửi từ Client → Server
│   │   ├── Response.java       ← Gói tin trả từ Server → Client
│   │   └── Action.java         ← Enum tất cả actions
│   └── util/
│       └── JsonUtil.java       ← Gson helper
```

#### Request.java
```java
public class Request implements Serializable {
    private String action;      // Ví dụ: "GET_ALL_TABLES"
    private String payload;     // JSON string dữ liệu gửi kèm (nullable)
    private String token;       // Session token từ Redis (sau khi login)

    public Request(String action, String payload, String token) {
        this.action = action;
        this.payload = payload;
        this.token = token;
    }
    // getters, setters...
}
```

#### Response.java
```java
public class Response implements Serializable {
    private boolean success;
    private String message;
    private String data;        // JSON string kết quả
    private int statusCode;     // 200, 400, 401, 404, 500, 503

    public static Response ok(String data) {
        return new Response(true, "OK", data, 200);
    }
    public static Response error(int code, String message) {
        return new Response(false, message, null, code);
    }
}
```

#### Action.java
```java
public enum Action {
    // Auth
    LOGIN, LOGOUT,
    // Tables
    GET_ALL_TABLES, UPDATE_TABLE_STATUS,
    // Orders
    CREATE_INVOICE, ADD_ITEM, REMOVE_ITEM, GET_INVOICE,
    // Payment
    CHECKOUT, APPLY_DISCOUNT,
    // Menu
    GET_MENU, ADD_MENU_ITEM, UPDATE_MENU_ITEM, DELETE_MENU_ITEM,
    // Customers
    GET_CUSTOMERS, ADD_CUSTOMER, UPDATE_CUSTOMER,
    // Stats
    GET_REVENUE_BY_DATE, GET_REVENUE_BY_MONTH,
    // Reservations
    CREATE_RESERVATION, GET_RESERVATIONS, CANCEL_RESERVATION
}
```

**Việc cần làm:**
- [ ] Implement `Request`, `Response`, `Action`
- [ ] Implement `JsonUtil` (Gson wrapper)
- [ ] Unit test: serialize/deserialize một Request thử

---

### PHASE 2 — Server TCP Socket (Tuần 2–3)

**Mục tiêu:** Server lắng nghe kết nối, xử lý đa client bằng ThreadPool

#### Cấu trúc Server

```
src/main/java/server/
├── Server.java                 ← Entry point, mở ServerSocket port 9999
├── ClientHandler.java          ← Thread xử lý 1 client (Runnable)
├── RequestDispatcher.java      ← Router: action → Handler
└── handler/
    ├── AuthHandler.java
    ├── TableHandler.java
    ├── OrderHandler.java
    ├── PaymentHandler.java
    ├── MenuHandler.java
    ├── CustomerHandler.java
    └── StatsHandler.java
```

#### Server.java
```java
public class Server {
    private static final int PORT = EnvConfig.serverPort(); // từ .env

    public static void main(String[] args) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(20);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Client connected: "
                    + clientSocket.getInetAddress().getHostAddress());
                pool.submit(new ClientHandler(clientSocket));
            }
        }
    }
}
```

#### ClientHandler.java
```java
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher = new RequestDispatcher();

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try (ObjectInputStream in  = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            Request request = (Request) in.readObject();
            Response response = dispatcher.dispatch(request);
            out.writeObject(response);
            out.flush();

        } catch (Exception e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
```

#### RequestDispatcher.java
```java
public class RequestDispatcher {
    private final AuthHandler    authHandler    = new AuthHandler();
    private final TableHandler   tableHandler   = new TableHandler();
    private final OrderHandler   orderHandler   = new OrderHandler();
    private final PaymentHandler paymentHandler = new PaymentHandler();
    private final MenuHandler    menuHandler    = new MenuHandler();
    private final StatsHandler   statsHandler   = new StatsHandler();

    public Response dispatch(Request request) {
        // Kiểm tra token Redis (trừ LOGIN)
        if (!"LOGIN".equals(request.getAction())) {
            if (!SessionService.isValid(request.getToken())) {
                return Response.error(401, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.");
            }
        }

        return switch (request.getAction()) {
            case "LOGIN"               -> authHandler.login(request);
            case "LOGOUT"              -> authHandler.logout(request);
            case "GET_ALL_TABLES"      -> tableHandler.getAllTables(request);
            case "UPDATE_TABLE_STATUS" -> tableHandler.updateStatus(request);
            case "CREATE_INVOICE"      -> orderHandler.createInvoice(request);
            case "ADD_ITEM"            -> orderHandler.addItem(request);
            case "REMOVE_ITEM"         -> orderHandler.removeItem(request);
            case "CHECKOUT"            -> paymentHandler.checkout(request);
            case "GET_MENU"            -> menuHandler.getAll(request);
            case "ADD_MENU_ITEM"       -> menuHandler.add(request);
            case "DELETE_MENU_ITEM"    -> menuHandler.delete(request);
            case "GET_REVENUE_BY_DATE" -> statsHandler.getByDate(request);
            default -> Response.error(400, "Action không hợp lệ: " + request.getAction());
        };
    }
}
```

**Việc cần làm:**
- [ ] Implement `Server.java`
- [ ] Implement `ClientHandler.java`
- [ ] Implement `RequestDispatcher.java`
- [ ] Test: chạy Server, kết nối thử bằng `telnet localhost 9999`

---

### PHASE 3 — Redis Session & Cache (Tuần 3)

**Mục tiêu:** Dùng Redis lưu session token sau login và cache dữ liệu hay dùng

#### Cấu trúc

```
src/main/java/cache/
├── RedisConnection.java    ← Singleton JedisPool
├── SessionService.java     ← Quản lý login token
└── CacheService.java       ← Cache menu, bàn
```

#### Key patterns trong Redis

| Key | TTL | Nội dung |
|---|---|---|
| `session:{token}` | 8 giờ | JSON: employeeId, role, loginAt |
| `cache:menu` | 5 phút | JSON array toàn bộ menu |
| `cache:tables` | 30 giây | JSON array trạng thái bàn |

#### RedisConnection.java
```java
public class RedisConnection {
    private static JedisPool pool;

    public static JedisPool getPool() {
        if (pool == null) {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(20);
            pool = new JedisPool(config, EnvConfig.redisHost(), EnvConfig.redisPort());
        }
        return pool;
    }

    public static Jedis getClient() {
        return getPool().getResource();
    }
}
```

#### SessionService.java
```java
public class SessionService {
    private static final int TOKEN_TTL = EnvConfig.sessionTtlSeconds(); // từ .env
    private static final String PREFIX  = "session:";

    public static String createSession(String employeeId, String role) {
        String token = UUID.randomUUID().toString();
        String data  = JsonUtil.toJson(Map.of(
            "employeeId", employeeId,
            "role",       role,
            "loginAt",    LocalDateTime.now().toString()
        ));
        try (Jedis jedis = RedisConnection.getClient()) {
            jedis.setex(PREFIX + token, TOKEN_TTL, data);
        }
        return token;
    }

    public static boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        try (Jedis jedis = RedisConnection.getClient()) {
            return jedis.exists(PREFIX + token);
        }
    }

    public static void invalidate(String token) {
        try (Jedis jedis = RedisConnection.getClient()) {
            jedis.del(PREFIX + token);
        }
    }
}
```

#### CacheService.java
```java
public class CacheService {
    public static void setMenu(String json) {
        try (Jedis j = RedisConnection.getClient()) { j.setex("cache:menu", 300, json); }
    }
    public static String getMenu() {
        try (Jedis j = RedisConnection.getClient()) { return j.get("cache:menu"); }
    }
    public static void invalidateMenu() {
        try (Jedis j = RedisConnection.getClient()) { j.del("cache:menu"); }
    }

    public static void setTables(String json) {
        try (Jedis j = RedisConnection.getClient()) { j.setex("cache:tables", 30, json); }
    }
    public static String getTables() {
        try (Jedis j = RedisConnection.getClient()) { return j.get("cache:tables"); }
    }
    public static void invalidateTables() {
        try (Jedis j = RedisConnection.getClient()) { j.del("cache:tables"); }
    }
}
```

**Việc cần làm:**
- [ ] Implement `RedisConnection`, `SessionService`, `CacheService`
- [ ] Tích hợp `SessionService.isValid()` vào `RequestDispatcher`
- [ ] Test: login → `redis-cli keys session:*` thấy key → logout → key biến mất

---

### PHASE 4 — AWS DynamoDB DAO Layer (Tuần 3–4)

**Mục tiêu:** Viết lại toàn bộ DAO dùng AWS SDK v2, đáp ứng CLO1

#### Cấu trúc

```
src/main/java/dao/
├── DynamoDBConnection.java     ← Singleton DynamoDbClient
├── TableDAO.java               ← 5 phương thức CRUD
├── InvoiceDAO.java             ← Embedded items list
├── MenuDAO.java
├── CustomerDAO.java
├── EmployeeDAO.java
├── ReservationDAO.java
└── StatsDAO.java               ← Scan + filter
```

#### DynamoDBConnection.java
```java
public class DynamoDBConnection {
    private static DynamoDbClient client;

    public static DynamoDbClient getInstance() {
        if (client == null) {
            AwsCredentials creds = AwsBasicCredentials.create(
                EnvConfig.awsAccessKey(),
                EnvConfig.awsSecretKey()
            );
            client = DynamoDbClient.builder()
                .region(Region.of(EnvConfig.awsRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
        }
        return client;
    }

    // Đọc credentials từ .env qua EnvConfig — không cần file properties nữa
}
```

#### TableDAO.java — Đầy đủ 5 phương thức (CLO1)
```java
public class TableDAO {
    private final DynamoDbClient db = DynamoDBConnection.getInstance();
    private static final String TBL = "Tables";

    // 1. Lấy tất cả
    public List<Map<String, AttributeValue>> findAll() {
        return db.scan(ScanRequest.builder().tableName(TBL).build()).items();
    }

    // 2. Tìm theo ID
    public Map<String, AttributeValue> findById(String tableId) {
        return db.getItem(GetItemRequest.builder()
            .tableName(TBL)
            .key(Map.of("tableId", AttributeValue.fromS(tableId)))
            .build()).item();
    }

    // 3. Thêm mới
    public void insert(Ban ban) {
        db.putItem(PutItemRequest.builder().tableName(TBL).item(Map.of(
            "tableId",   AttributeValue.fromS(ban.getTableId()),
            "tableName", AttributeValue.fromS(ban.getTableName()),
            "capacity",  AttributeValue.fromN(String.valueOf(ban.getCapacity())),
            "status",    AttributeValue.fromS(ban.getStatus()),
            "floor",     AttributeValue.fromS(ban.getFloor())
        )).build());
    }

    // 4. Cập nhật trạng thái
    public void update(String tableId, String newStatus) {
        db.updateItem(UpdateItemRequest.builder()
            .tableName(TBL)
            .key(Map.of("tableId", AttributeValue.fromS(tableId)))
            .updateExpression("SET #s = :status")
            .expressionAttributeNames(Map.of("#s", "status"))
            .expressionAttributeValues(Map.of(":status", AttributeValue.fromS(newStatus)))
            .build());
    }

    // 5. Xóa
    public void delete(String tableId) {
        db.deleteItem(DeleteItemRequest.builder()
            .tableName(TBL)
            .key(Map.of("tableId", AttributeValue.fromS(tableId)))
            .build());
    }
}
```

#### InvoiceDAO.java — Embedded items (document-style NoSQL)
```java
// Hóa đơn chứa danh sách món bên trong — đặc trưng của NoSQL
public void insert(HoaDon invoice) {
    List<AttributeValue> itemsList = invoice.getItems().stream()
        .map(item -> AttributeValue.fromM(Map.of(
            "itemId", AttributeValue.fromS(item.getItemId()),
            "name",   AttributeValue.fromS(item.getName()),
            "qty",    AttributeValue.fromN(String.valueOf(item.getQty())),
            "price",  AttributeValue.fromN(String.valueOf(item.getPrice()))
        ))).toList();

    db.putItem(PutItemRequest.builder().tableName("Invoices").item(Map.of(
        "invoiceId",  AttributeValue.fromS(invoice.getInvoiceId()),
        "tableId",    AttributeValue.fromS(invoice.getTableId()),
        "status",     AttributeValue.fromS(invoice.getStatus()),
        "createdAt",  AttributeValue.fromS(invoice.getCreatedAt().toString()),
        "items",      AttributeValue.fromL(itemsList),
        "total",      AttributeValue.fromN(String.valueOf(invoice.getTotal())),
        "employeeId", AttributeValue.fromS(invoice.getEmployeeId())
    )).build());
}
```

#### StatsDAO.java — Scan với filter expression
```java
public List<Map<String, AttributeValue>> getInvoicesByDate(String dateStr) {
    return db.scan(ScanRequest.builder()
        .tableName("Invoices")
        .filterExpression("begins_with(createdAt, :date) AND #s = :paid")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(
            ":date", AttributeValue.fromS(dateStr),  // "2025-05-01"
            ":paid", AttributeValue.fromS("PAID")
        ))
        .build()).items();
}
```

**Việc cần làm:**
- [ ] Implement `DynamoDBConnection.java`
- [ ] Implement `TableDAO.java` (5 phương thức — demo CLO1)
- [ ] Implement `InvoiceDAO.java` (embedded items)
- [ ] Implement `MenuDAO.java`, `EmployeeDAO.java`, `CustomerDAO.java`
- [ ] Implement `ReservationDAO.java`, `StatsDAO.java`
- [ ] Viết Java seed script — import dữ liệu mẫu vào DynamoDB

**Thành viên phụ trách:** 2 người (database focus)

---

### PHASE 5 — Business Logic Handlers (Tuần 4–5)

**Mục tiêu:** Kết nối DAO + Redis vào từng Handler

#### AuthHandler.java
```java
public class AuthHandler {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public Response login(Request request) {
        LoginPayload p = JsonUtil.fromJson(request.getPayload(), LoginPayload.class);
        Employee emp = employeeDAO.findByUsername(p.getUsername());

        if (emp == null || !emp.getPasswordHash().equals(hash(p.getPassword()))) {
            return Response.error(401, "Sai tên đăng nhập hoặc mật khẩu");
        }

        String token = SessionService.createSession(emp.getEmployeeId(), emp.getRole());
        return Response.ok(JsonUtil.toJson(new LoginResult(token, emp.getEmployeeId(),
            emp.getFullName(), emp.getRole())));
    }

    public Response logout(Request request) {
        SessionService.invalidate(request.getToken());
        return Response.ok(null);
    }
}
```

#### MenuHandler.java — Tích hợp Redis cache
```java
public class MenuHandler {
    private final MenuDAO menuDAO = new MenuDAO();

    public Response getAll(Request request) {
        String cached = CacheService.getMenu();
        if (cached != null) return Response.ok(cached);   // Cache hit

        List<MenuItem> items = menuDAO.findAll();          // DynamoDB
        String json = JsonUtil.toJson(items);
        CacheService.setMenu(json);                        // Lưu cache
        return Response.ok(json);
    }

    public Response add(Request request) {
        MenuItem item = JsonUtil.fromJson(request.getPayload(), MenuItem.class);
        menuDAO.insert(item);
        CacheService.invalidateMenu();                     // Xóa cache cũ
        return Response.ok(null);
    }

    public Response delete(Request request) {
        DeletePayload p = JsonUtil.fromJson(request.getPayload(), DeletePayload.class);
        menuDAO.delete(p.getCategoryId(), p.getItemId());
        CacheService.invalidateMenu();
        return Response.ok(null);
    }
}
```

**Actions cần implement theo độ ưu tiên:**

| Action | Handler | CLO |
|---|---|---|
| `LOGIN` / `LOGOUT` | AuthHandler | CLO6 |
| `GET_ALL_TABLES` | TableHandler | CLO1, CLO6 |
| `UPDATE_TABLE_STATUS` | TableHandler | CLO1 |
| `CREATE_INVOICE` | OrderHandler | CLO6 |
| `ADD_ITEM` / `REMOVE_ITEM` | OrderHandler | CLO1 |
| `CHECKOUT` | PaymentHandler | CLO6 |
| `GET_MENU` (có cache) | MenuHandler | CLO1, CLO6 |
| `ADD/DELETE_MENU_ITEM` | MenuHandler | CLO1 |
| `GET_REVENUE_BY_DATE` | StatsHandler | CLO6 |
| `CREATE_RESERVATION` | ReservationHandler | CLO1 |

**Việc cần làm:**
- [ ] Implement `AuthHandler`, `TableHandler`, `OrderHandler`
- [ ] Implement `PaymentHandler`, `MenuHandler` (với Redis cache)
- [ ] Implement `StatsHandler`, `ReservationHandler`
- [ ] Test từng handler độc lập

---

### PHASE 6 — Client ApiClient + Update UI (Tuần 5–6)

**Mục tiêu:** Client JavaFX gửi Request qua Socket thay vì gọi DAO trực tiếp

#### Cấu trúc

```
src/main/java/client/
├── ApiClient.java          ← Gửi Request, nhận Response qua Socket
└── SessionManager.java     ← Lưu token trong memory sau login
```

#### ApiClient.java
```java
public class ApiClient {
    private static final String HOST = EnvConfig.serverHost(); // từ .env
    private static final int    PORT = EnvConfig.serverPort(); // từ .env

    public static Response send(String action, Object payload) {
        String json = payload != null ? JsonUtil.toJson(payload) : null;
        Request req = new Request(action, json, SessionManager.getToken());

        try (Socket socket = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(req);
            out.flush();
            return (Response) in.readObject();

        } catch (ConnectException e) {
            return Response.error(503, "Không thể kết nối đến server.");
        } catch (Exception e) {
            return Response.error(500, "Lỗi: " + e.getMessage());
        }
    }
}
```

#### SessionManager.java
```java
public class SessionManager {
    private static String token, employeeId, role, fullName;

    public static void save(LoginResult r) {
        token = r.getToken(); employeeId = r.getEmployeeId();
        role  = r.getRole();  fullName   = r.getFullName();
    }

    public static String getToken()      { return token; }
    public static String getRole()       { return role; }
    public static boolean isAdmin()      { return "ADMIN".equals(role); }
    public static boolean isManager()    { return "MANAGER".equals(role); }
    public static void clear()           { token = employeeId = role = fullName = null; }
}
```

#### Pattern cập nhật Controllers

```java
// === CŨ (offline, gọi DAO trực tiếp) ===
List<Ban> tables = banDAO.findAll();

// === MỚI (distributed, qua Socket) ===
Response res = ApiClient.send("GET_ALL_TABLES", null);
if (res.isSuccess()) {
    List<Ban> tables = JsonUtil.fromJsonList(res.getData(), Ban.class);
    // cập nhật UI...
} else {
    showAlert("Lỗi: " + res.getMessage());
}
```

**Controllers cần cập nhật:**

| Controller | Actions gọi | Ưu tiên |
|---|---|---|
| `DangNhap.java` | `LOGIN` | 🔴 Cao nhất |
| `DatBan.java` | `GET_ALL_TABLES`, `UPDATE_TABLE_STATUS`, `CREATE_INVOICE` | 🔴 Cao nhất |
| `ManHinhChinh.java` | `GET_MENU`, `ADD_ITEM`, `REMOVE_ITEM` | 🔴 Cao nhất |
| `ThanhToanPreviewController.java` | `CHECKOUT`, `APPLY_DISCOUNT` | 🔴 Cao nhất |
| `ThongKe.java` | `GET_REVENUE_BY_DATE`, `GET_REVENUE_BY_MONTH` | 🟡 Trung bình |
| `KhachHang.java` | `GET_CUSTOMERS`, `ADD_CUSTOMER` | 🟡 Trung bình |
| `ThucDon.java` | `GET_MENU`, `ADD_MENU_ITEM`, `DELETE_MENU_ITEM` | 🟡 Trung bình |
| `NhanVienUI.java` | `GET_EMPLOYEES` (Admin only) | 🟢 Thấp |

**Việc cần làm:**
- [ ] Implement `ApiClient.java`, `SessionManager.java`
- [ ] Update `DangNhap.java` → gọi LOGIN action
- [ ] Update `DatBan.java` (163KB — chia nhỏ trước khi sửa)
- [ ] Update `ManHinhChinh.java`, `ThanhToanPreviewController.java`
- [ ] Update `ThongKe.java`, `KhachHang.java`, `ThucDon.java`
- [ ] Xử lý lỗi kết nối gracefully trong mọi Controller

---

### PHASE 7 — Integration Testing (Tuần 7)

**Mục tiêu:** Test end-to-end toàn hệ thống

**Checklist bắt buộc:**

- [ ] **CLO1 — NoSQL CRUD:** Demo đủ 5 operations trực tiếp
  - Thêm món ăn mới → DynamoDB Console thấy record
  - Cập nhật trạng thái bàn → verify trên Console
  - Tạo đặt bàn → xóa đặt bàn → verify deleted
  - Query hóa đơn theo ngày
- [ ] **CLO6 — Multi-client:** Mở 2 cửa sổ Client
  - Client 1 tạo order bàn B01
  - Client 2 refresh → thấy B01 chuyển sang "Đang dùng"
- [ ] **Redis session:** Login → `redis-cli keys session:*` → thấy token → Logout → key xóa
- [ ] **Redis cache:** GET_MENU lần 1 (DynamoDB) → lần 2 (Redis, không có DynamoDB call)
- [ ] **Lỗi mạng:** Tắt Server → Client báo lỗi gracefully, không crash
- [ ] **Phân quyền:** Tài khoản Thu ngân không thấy màn hình Admin/Quản lý

**Cách chạy demo 2 client trên 1 máy:**
```bash
# Terminal 1
java -jar server.jar

# Terminal 2 (nếu chưa chạy)
redis-server

# Chạy 2 instance JavaFX (2 cửa sổ riêng biệt)
java -jar client.jar   # Cửa sổ 1 — login Thu ngân
java -jar client.jar   # Cửa sổ 2 — login Quản lý
```

---

### PHASE 8 — Báo Cáo & Thuyết Trình (Song song từ Tuần 2)

**Cấu trúc đúng theo rubric:**

#### Chương 1: Giới thiệu
- Đặt vấn đề: nhà hàng nhiều bàn, nhiều thu ngân cùng hoạt động → cần phân tán
- Giới thiệu hệ thống Tứ Hữu — kiến trúc Client–Server trên LAN
- Công nghệ sử dụng: Java TCP Socket, AWS DynamoDB, Redis, JavaFX

#### Chương 2: Phân tích yêu cầu (CLO4)
- Actor: Admin, Quản lý, Thu ngân
- Yêu cầu chức năng từng role
- Yêu cầu phi chức năng: đa client, session timeout, cache
- Use Case Diagram tổng thể

#### Chương 3: Phân tích thiết kế (CLO1, CLO5)
- **Mô hình phân tán:** Giải thích Client–Server TCP Socket; lý do chọn TCP thay RMI (đơn giản, không cần stub/skeleton, dễ debug)
- **Giao thức giao tiếp:** Mô tả Request/Response format (JSON over ObjectStream)
- **Use Case Diagram** chi tiết từng actor
- **Activity Diagram:** Luồng đặt bàn, luồng thanh toán
- **Sequence Diagram:** Flow LOGIN (Client→Server→Redis), flow ORDER (Client→Server→DynamoDB)
- **Class Diagram:** Server-side (Handler, DAO), Client-side (ApiClient, Controller)
- **Database Design:** DynamoDB table schemas + Redis key patterns

#### Chương 4: Triển khai (CLO6)
- Thứ tự khởi động: Redis → Server → Client(s)
- Hướng dẫn cài đặt, cấu hình AWS credentials
- Screenshot giao diện từng màn hình chính
- Code giải thích: `ClientHandler.java`, `DynamoDBConnection.java`, `SessionService.java`

#### Chương 5: Kết luận
- Kết quả đạt được (map từng CLO)
- Hạn chế: Socket chưa encrypt (TLS), Redis chưa password, DynamoDB Scan tốn RCU
- Hướng phát triển: TLS Socket, Redis Cluster, DynamoDB GSI cho query phức tạp hơn

---

## 📅 TIMELINE TỔNG QUAN

```
Tuần 1  │ Phase 0 : Setup AWS, Redis, DynamoDB tables, thêm dependencies
Tuần 2  │ Phase 1 : Network protocol (Request/Response/Action/JsonUtil)
         │           Phase 8 : Viết Chương 1, 2; vẽ Use Case Diagram
Tuần 3  │ Phase 2 : Server TCP Socket (Server, ClientHandler, Dispatcher)
         │           Phase 3 : Redis (SessionService + CacheService)
         │           Phase 8 : Vẽ Sequence Diagram, Class Diagram
Tuần 4  │ Phase 4 : DynamoDB DAO Layer + seed data
         │           Phase 5 : AuthHandler, TableHandler, OrderHandler
Tuần 5  │ Phase 5 : PaymentHandler, MenuHandler, StatsHandler
         │           Phase 6 : ApiClient, SessionManager
Tuần 6  │ Phase 6 : Update tất cả UI Controllers
         │           Phase 8 : Viết Chương 4 + chụp screenshots
Tuần 7  │ Phase 7 : Integration testing, fix bugs, chuẩn bị demo
         │           Phase 8 : Finalize báo cáo, viết Chương 5
Tuần 14 │           Nộp + Thuyết trình
```

---

## 🎯 MAPPING CLOs

| CLO | Yêu cầu | Thể hiện trong project |
|---|---|---|
| **CLO1** | Kết nối NoSQL, ≥2 phương thức CRUD | Tất cả DAO classes (mỗi DAO đủ 5 ops) |
| **CLO2** | Báo cáo đúng cấu trúc, đầy đủ | File Word/PDF 5 chương + References |
| **CLO3** | Sử dụng tài nguyên hiệu quả | Redis cache giảm DynamoDB reads; ThreadPool quản lý connections |
| **CLO4** | Trình bày mô hình phân tán | Chương 2 + thuyết trình giải thích kiến trúc |
| **CLO5** | Đề xuất Socket/RMI phù hợp | Chương 3 — lý do chọn TCP Socket thay RMI |
| **CLO6** | Implement ứng dụng phân tán | Server + Client + demo 2 client đồng thời |

---

## 💡 NHỮNG GÌ GIỮ LẠI TỪ PROJECT CŨ

| Component | Giữ lại | Mức sửa đổi |
|---|---|---|
| 16 Entity classes | ✅ | Thêm `implements Serializable` |
| 21 file FXML | ✅ | Không cần sửa |
| CSS, fonts, icons, images | ✅ | Không cần sửa |
| Logic DoiBan/GopBan/TachBan | ✅ | Đóng gói vào Handler |
| QR code, PDF export | ✅ | Chạy ở Server side |
| SQL queries / JDBC | ❌ | Viết lại thành DynamoDB SDK |
| ConnectDB.java | ❌ | Thay bằng `DynamoDBConnection.java` |
| DAO method signatures | ⚠️ | Giữ tên method, đổi implementation |

---
# 🔄 KẾ HOẠCH REBUILD PROJECT — Distributed Programming With Java (UPDATED)

---

## 🚀 CẢI TIẾN QUAN TRỌNG (BỔ SUNG)

### 1. 🔁 Cải tiến Socket — xử lý nhiều request liên tục

**Vấn đề hiện tại:** Mỗi connection chỉ xử lý 1 request rồi đóng → không ổn định khi multi-client

**Giải pháp:**

```java
while (true) {
    Request request = (Request) in.readObject();
    Response response = dispatcher.dispatch(request);
    out.writeObject(response);
    out.flush();
}
```

👉 Giúp client gửi nhiều request liên tục, ổn định hơn khi demo

---

### 2. ⚡ Tối ưu DynamoDB — tránh lạm dụng Scan

**Vấn đề:** `Scan` tốn tài nguyên và dễ bị hỏi khi bảo vệ

**Giải pháp:**

* Dataset nhỏ → Scan chấp nhận được
* Production → dùng Query + GSI
* Có thể thêm GSI như `status-index`

---

### 3. 🔒 Xử lý Concurrency (tránh ghi đè dữ liệu)

**Giải pháp:**

```java
.conditionExpression("attribute_exists(tableId)")
```

👉 Đảm bảo update an toàn khi nhiều client cùng thao tác

---

### 4. 🧠 Cache Strategy rõ ràng hơn

| Event          | Cache cần invalidate |
| -------------- | -------------------- |
| Update menu    | `cache:menu`         |
| Update bàn     | `cache:tables`       |
| Checkout       | `cache:tables`       |
| Order thay đổi | `cache:tables`       |

---

### 5. 🌐 Xử lý lỗi mạng tốt hơn

```java
catch (ConnectException e) {
    return Response.error(503, "Server không khả dụng. Vui lòng thử lại.");
}
```

---

### 6. 🔐 Bổ sung phần bảo mật

* Chưa dùng TLS
* Redis chưa auth
* Password hash cơ bản

**Hướng phát triển:**

* TLS Socket
* Redis password
* BCrypt hashing

---

### 7. 📊 Logging để demo chuyên nghiệp hơn

```java
System.out.println("[REQUEST] " + request.getAction());
System.out.println("[RESPONSE] " + response.getStatusCode());
```

---

### 8. ✨ Nâng cấp nhỏ để ghi điểm

* Auto refresh bàn mỗi 5–10s
* Sync realtime giữa 2 client

---

## 🎯 KẾT LUẬN

Các cải tiến này giúp:

* Hệ thống ổn định hơn khi multi-client
* Demo mượt hơn
* Dễ trả lời câu hỏi giảng viên
* Tăng khả năng đạt điểm cao (9–10)

👉 Không cần thay đổi kiến trúc — chỉ cần refine implementation


## 🚨 LƯU Ý QUAN TRỌNG

1. **Thứ tự khởi động bắt buộc:** `Redis` → `Server` → `Client` — document rõ trong README
2. **`.env` KHÔNG được commit lên Git** — chỉ commit `.env.example`. Mỗi thành viên tự tạo `.env` từ template
3. **AWS Free Tier DynamoDB:** 25 GB storage + 25 RCU/WCU/tháng — đủ dùng cho demo học
4. **Demo thuyết trình:** Chuẩn bị sẵn 2 cửa sổ Client; thao tác Client 1 phải phản ánh ngay trên Client 2
5. **CLO1 backup plan:** Nếu UI lỗi khi demo, chuẩn bị sẵn đoạn code test chạy 5 CRUD operations thẳng từ `main()` để chứng minh NoSQL hoạt động
6. **Redis local:** Môi trường demo không cần password; production nên bật `requirepass`
