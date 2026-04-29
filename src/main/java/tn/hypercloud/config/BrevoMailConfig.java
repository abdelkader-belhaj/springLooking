package tn.hypercloud.config; // 👈 make sure this matches your config folder package

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class BrevoMailConfig {

    @Bean(name = "brevoMailSender")
    public JavaMailSender brevoMailSender(
            @Value("${brevo.mail.host}") String host,
            @Value("${brevo.mail.port}") int port,
            @Value("${brevo.mail.username}") String username,
            @Value("${brevo.mail.password}") String password) {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", host);
        return sender;
    }
}