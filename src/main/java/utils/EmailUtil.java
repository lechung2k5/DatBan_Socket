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
     */
    public static boolean sendOTP(String toEmail, String otp) {
        String htmlContent = "<h3>Xác thực đăng ký tài khoản</h3>"
                + "<p>Chào bạn, mã OTP của bạn là: <b style='color:red; font-size: 24px;'>" + otp + "</b></p>"
                + "<p>Mã này có hiệu lực trong vòng 5 phút. Vui lòng không cung cấp mã này cho bất kỳ ai.</p>"
                + "<br/><p>Trân trọng,<br/>Đội ngũ Quản lý nhà hàng.</p>";
        
        return sendEmail(toEmail, "Mã xác thực OTP - Hệ thống Đặt Bàn Nhà Hàng", htmlContent);
    }

    /**
     * Gửi email thông thường (Tính năng từ nhánh giaminh)
     */
    public static boolean sendEmail(String recipientEmail, String subject, String content) {
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            return false;
        }

        String username = EnvConfig.mailUsername();
        String password = EnvConfig.mailPassword();

        if (username == null || password == null) {
            System.err.println("[EmailUtil] Lỗi: Chưa cấu hình MAIL_USERNAME hoặc MAIL_PASSWORD trong .env");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("[EmailUtil] Đã gửi email thành công tới: " + recipientEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("[EmailUtil] Lỗi gửi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
