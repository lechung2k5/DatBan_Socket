
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

SQL Server: Cơ sở dữ liệu lưu trữ nghiệp vụ.

🚀 Hướng dẫn Build & Chạy ứng dụng
1. Yêu cầu hệ thống
Java: OpenJDK 21 hoặc Azul Zulu (có sẵn JavaFX).

Maven: Phiên bản 3.9 trở lên.

2. Cài đặt font chữ (Dành cho in ấn)
Hệ thống sử dụng font Arial hệ thống để đảm bảo hiển thị Tiếng Việt trên hóa đơn. Đảm bảo máy tính của bạn có font Arial tại đường dẫn C:/Windows/Fonts/arial.ttf.

3. Biên dịch và đóng gói (Executable JAR)
Sử dụng Maven để tạo file JAR thực thi bao gồm tất cả thư viện:

Bash

mvn clean package
File kết quả sẽ nằm trong thư mục /target/ dưới dạng {Tên_Dự_Án}.jar.

4. Chạy ứng dụng
Bash

java -jar target/Nhom08_QuanLyDatBan.jar
5.Tài khoản - mật khẩu
Admin (Toàn quyền) - username: admin | password: admin123
Quản lý - username: nv001 | password: 123
Nhân viên thu ngân - username: nv003 | password: 123