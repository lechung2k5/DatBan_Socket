
🍽️ Dự án Quản lý Đặt bàn - Nhà hàng Tứ Hữu
Dự án này là hệ thống quản lý nghiệp vụ nhà hàng toàn diện, tập trung vào trải nghiệm người dùng (UX) cho thu ngân và tối ưu hóa quy trình in ấn hóa đơn thực tế.

## ✨ Tính năng nổi bật


Sơ đồ bàn thông minh: Tự động hiển thị trạng thái bàn theo thời gian thực (Trống - Trắng, Đã đặt - Đỏ, Đang sử dụng - Cam).

Logic đặt bàn nâng cao: Hỗ trợ vùng đệm an toàn 4 tiếng (khóa bàn) và cảnh báo mềm 8 tiếng để tránh trùng lịch đặt.

Quản lý gọi món (Order): Menu phân loại theo danh mục (Khai vị, Nướng, Lẩu...), hỗ trợ tìm kiếm nhanh và tùy chỉnh số lượng món linh hoạt.

Nghiệp vụ bàn phức tạp: Hỗ trợ các chức năng Đổi bàn, Tách bàn và Gộp bàn trực tiếp trên hóa đơn đang hoạt động.

Hệ thống Thanh toán QR: Tích hợp tạo mã QR động (VietQR/MoMo) tự động tính toán số tiền và nội dung chuyển khoản.

Quản lý Tiền cọc & Uu đãi: Tự động áp dụng khuyến mãi theo % và chính sách hoàn tiền cọc dựa trên thời gian hủy bàn.

🛠️ Công nghệ sử dụng
Ngôn ngữ: Java 21 (Yêu cầu JDK 21+ có JavaFX).

Giao diện: JavaFX (FXML & CSS Custom).

Quản lý dự án: Maven.

Thư viện hỗ trợ:

Apache PDFBox: Xử lý xuất hóa đơn PDF chuyên dụng.

JavaMail API: Gửi thông báo chương trình thành viên qua Email.

Cơ sở dữ liệu: AWS DynamoDB (NoSQL) & Redis (Caching).

🚀 Hướng dẫn Build & Chạy ứng dụng
1. Yêu cầu hệ thống
- Java: OpenJDK 21 hoặc Azul Zulu (có sẵn JavaFX).
- Maven: Phiên bản 3.9 trở lên.

2. Cấu hình Môi trường (.env)
Dự án sử dụng file `.env` để quản lý các thông số kết nối bí mật. 
- Vui lòng copy file `.env.example` thành `.env`.
- **LƯU Ý:** Để đảm bảo an toàn thông tin, các Key truy cập AWS và Redis trong file nộp bài đã được ẩn đi. 
- **Để lấy thông tin kết nối thực tế nhằm chấm bài, vui lòng liên hệ với nhóm qua zalo hoặc email: lechung020905@gmail.com.**

3. Biên dịch và đóng gói (Executable JAR)
Sử dụng Maven để tạo file JAR thực thi bao gồm tất cả thư viện:
```bash
mvn clean package -DskipTests
```
File kết quả sẽ nằm trong thư mục `/target/` dưới dạng `javafx-app.jar`.

4. Chạy ứng dụng

#### Cách 1: Chạy nhanh bằng Maven (Khuyên dùng khi chấm bài)
Mở hai cửa sổ terminal riêng biệt và chạy các lệnh sau:
- **Chạy Server:**
  ```bash
  mvn exec:java -Dexec.mainClass="network.Service"
  ```
- **Chạy Client (Giao diện):**
  ```bash
  mvn javafx:run
  ```

#### Cách 2: Chạy bằng file JAR (Sau khi đã build ở bước 3)
- **Chạy Server:**
  ```bash
  java -cp target/javafx-app.jar network.Service
  ```
- **Sau đó chạy Client:**
  ```bash
  java -jar target/javafx-app.jar
  ```


5. Tài khoản dùng thử
- Admin (Toàn quyền) - username: `admin` | password: `admin123`
- Quản lý - username: `quanly` | password: `quanly123`
- Nhân viên thu ngân - username: `thungan1` | password: `tn123`

---
📧 **Thông tin liên hệ nhóm:** 
- Nhóm trưởng: Lê Công Chung
- Email: lechung020905@gmail.com
- SĐT: 0377019958

