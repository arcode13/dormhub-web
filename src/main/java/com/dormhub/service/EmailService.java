package com.dormhub.service;

import org.springframework.stereotype.Service;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Service
public class EmailService {

    public void sendResetPasswordEmail(String to, String resetUrl) throws MessagingException {
        String host = "mail.dormhub.my.id";
        String username = "support@dormhub.my.id";
        String password = "@dormhub2024";

        // Konfigurasi SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true"); 
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "465");

        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });


        MailcapCommandMap mailcap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mailcap.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mailcap.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mailcap.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mailcap.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mailcap.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mailcap);

        
        session.setDebug(true);

        
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(username, "DormHub Support"));
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Error setting 'from' address", e);
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject("Reset Password DormHub");
        message.setText("tester");

        
        Transport.send(message);
    }
}
