package tn.hypercloud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset.frontend-url}")
    private String frontendResetUrl;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendResetPasswordEmail(User user, String token) {
        String resetLink = frontendResetUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setSubject("Reinitialisation de votre mot de passe");
        message.setText(
            "Bonjour " + user.getFullName() + ",\n\n"
                        + "Une demande de reinitialisation de mot de passe a ete effectuee.\n"
                        + "Cliquez sur ce lien pour definir un nouveau mot de passe :\n"
                        + resetLink + "\n\n"
                        + "Si vous n'etes pas a l'origine de cette demande, ignorez cet email.\n"
                        + "Le lien expire dans 30 minutes."
        );

        mailSender.send(message);
    }
}
