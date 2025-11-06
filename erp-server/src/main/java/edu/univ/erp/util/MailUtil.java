package edu.univ.erp.util;

import java.util.Properties;

import edu.univ.erp.dao.settings.SettingDAO;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Simple email utility using Jakarta Mail. Reads SMTP settings from settings table.
 * Expected setting keys (best-effort): SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM, ADMIN_EMAIL
 */
public class MailUtil {
    private static final SettingDAO settingDAO = new SettingDAO();

    public static void sendEmailToAdmin(String subject, String body) throws Exception {
        String adminEmail = settingDAO.getSetting("ADMIN_EMAIL");
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            throw new Exception("ADMIN_EMAIL_NOT_CONFIGURED:Admin email is not configured on the server.");
        }
        sendEmail(adminEmail, subject, body);
    }

    public static void sendEmail(String to, String subject, String body) throws Exception {
        // Read SMTP configuration
        String host = settingDAO.getSetting("SMTP_HOST");
        String port = settingDAO.getSetting("SMTP_PORT");
        String user = settingDAO.getSetting("SMTP_USER");
        String pass = settingDAO.getSetting("SMTP_PASS");
        String from = settingDAO.getSetting("SMTP_FROM");

        if (host == null || host.trim().isEmpty()) {
            throw new Exception("SMTP_NOT_CONFIGURED:SMTP_HOST is not configured.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        if (port != null && !port.isEmpty()) props.put("mail.smtp.port", port);

        boolean auth = (user != null && !user.isEmpty());
        if (auth) {
            props.put("mail.smtp.auth", "true");
        }

        // Enable STARTTLS if configured (best-effort)
        String starttls = settingDAO.getSetting("SMTP_STARTTLS");
        if ("true".equalsIgnoreCase(starttls)) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        Session session;
        if (auth) {
            final String fuser = user;
            final String fpass = pass;
            session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fuser, fpass);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        try {
            MimeMessage message = new MimeMessage(session);
            if (from != null && !from.isEmpty()) {
                message.setFrom(new InternetAddress(from));
            } else if (user != null && !user.isEmpty()) {
                message.setFrom(new InternetAddress(user));
            } else {
                message.setFrom();
            }
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
        } catch (MessagingException me) {
            throw new Exception("Failed to send email: " + me.getMessage());
        }
    }
}
