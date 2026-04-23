package tn.hypercloud.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.User;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AccountApprovalEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address:${spring.mail.username:}}")
    private String fromAddress;

    @Value("${app.mail.from-name:TunisiaTour Security}")
    private String fromName;

    public void sendApprovalEmail(User user) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new RuntimeException("Configuration email manquante: app.mail.from-address / MAIL_USERNAME");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setTo(user.getEmail());
            helper.setSubject("Votre compte a ete approuve");
            helper.setText(
                    "Bonjour " + user.getFullName() + ",\n\n"
                            + "Votre compte a ete approuve par l administrateur.\n"
                            + "Vous pouvez maintenant vous connecter a l application avec votre email et votre mot de passe.\n\n"
                            + "Cordialement,\n"
                            + "L equipe TunisiaTour"
            );

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(new InternetAddress(fromAddress, fromName));
            }

            mailSender.send(mimeMessage);
        } catch (MailAuthenticationException ex) {
            throw new RuntimeException("Authentification SMTP echouee. Verifie MAIL_USERNAME et MAIL_PASSWORD", ex);
        } catch (MailException ex) {
            throw new RuntimeException("Echec d envoi SMTP vers " + user.getEmail(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible d envoyer l email d approbation", ex);
        }
    }
}
