package utils;

import db.EnvConfig;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * EmailUtil - Tiện ích gửi email (OTP, thông báo)
 */
public class EmailUtil {

    /**
     * Gửi mã OTP đến email khách hàng
     * @param toEmail Email người nhận
     * @param otp Mã OTP cần gửi
     * @return true nếu gửi thành công
     */
    public static boolean sendOTP(String toEmail, String otp) {
        String username = EnvConfig.mailUsername();
        String password = EnvConfig.mailPassword();

        if (username == null || password == null) {
            System.err.println("[EmailUtil] Lỗi: Chưa cấu hình MAIL_USERNAME hoặc MAIL_PASSWORD trong .env");
            return false;
        }

        // Cấu hình SMTP server (Gmail)
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Tạo phiên làm việc
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            // Tạo tin nhắn
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            message.setSubject("Mã xác thực OTP - Hệ thống Đặt Bàn Nhà Hàng");
            
            String htmlContent = "<h3>Xác thực đăng ký tài khoản</h3>"
                    + "<p>Chào bạn, mã OTP của bạn là: <b style='color:red; font-size: 24px;'>" + otp + "</b></p>"
                    + "<p>Mã này có hiệu lực trong vòng 5 phút. Vui lòng không cung cấp mã này cho bất kỳ ai.</p>"
                    + "<br/><p>Trân trọng,<br/>Đội ngũ Quản lý nhà hàng.</p>";
            
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Gửi email
            Transport.send(message);

            System.out.println("[EmailUtil] Đã gửi OTP thành công đến: " + toEmail);
            return true;

        } catch (MessagingException e) {
            System.err.println("[EmailUtil] Lỗi gửi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
