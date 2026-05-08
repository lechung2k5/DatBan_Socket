package utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {
    private static final String SENDER_EMAIL = "nhahangtuhuu@gmail.com"; // Default sender
    private static final String SENDER_APP_PASSWORD = "your_app_password_here"; // Will need Dotenv if available

    public static void sendEmail(String recipientEmail, String subject, String content) {
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            return;
        }
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                // In a real app, read from Dotenv, here we use placeholders or dotenv if available
                String email = io.github.cdimascio.dotenv.Dotenv.load().get("MAIL_USERNAME", SENDER_EMAIL);
                String pass = io.github.cdimascio.dotenv.Dotenv.load().get("MAIL_PASSWORD", SENDER_APP_PASSWORD);
                return new PasswordAuthentication(email, pass);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("[EmailUtil] Đã gửi email thành công tới " + recipientEmail);
        } catch (MessagingException e) {
            System.err.println("[EmailUtil] Lỗi gửi email: " + e.getMessage());
        }
    }
}
