package tn.hypercloud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.User;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset.frontend-url}")
    private String frontendResetUrl;

    @Value("${app.mail.from-address:${spring.mail.username:}}")
    private String fromAddress;

    @Value("${app.mail.from-name:TunisiaTour Security}")
    private String fromName;

    public void sendResetPasswordEmail(User user, String token) {
        String resetLink = frontendResetUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setTo(user.getEmail());
            helper.setSubject("Reinitialisation de votre mot de passe");
            helper.setText(
                "Bonjour " + user.getFullName() + ",\n\n"
                            + "Une demande de reinitialisation de mot de passe a ete effectuee.\n"
                            + "Cliquez sur ce lien pour definir un nouveau mot de passe :\n"
                            + resetLink + "\n\n"
                            + "Si vous n'etes pas a l'origine de cette demande, ignorez cet email.\n"
                            + "Le lien expire dans 30 minutes."
            );

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(new InternetAddress(fromAddress, fromName));
            }

            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible d envoyer l email de reinitialisation", ex);
        }
    }

    public void sendAccountApprovedEmail(User user) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setTo(user.getEmail());
            helper.setSubject("Votre compte TunisiaTour a ete active");
            helper.setText(
                "Bonjour " + user.getFullName() + ",\n\n"
                            + "Bonne nouvelle : votre compte a ete valide par l administrateur.\n"
                            + "Vous pouvez maintenant vous connecter a votre espace TunisiaTour.\n\n"
                            + "Si vous n etes pas a l origine de cette inscription, contactez le support."
            );

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(new InternetAddress(fromAddress, fromName));
            }

            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible d envoyer l email d activation de compte", ex);
        }
    }
}
