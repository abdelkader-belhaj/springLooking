package tn.hypercloud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset.frontend-url}")
    private String frontendResetUrl;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    // ============================================================
    // Méthode existante — NE PAS MODIFIER
    // ============================================================
    public void sendResetPasswordEmail(User user, String token) {
        String resetLink = frontendResetUrl + "?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

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

    // ============================================================
    // Email simple texte
    // ============================================================
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // ============================================================
    // Email HTML avec pièce jointe optionnelle
    // ============================================================
    public void sendEmailWithAttachment(
            String to,
            String subject,
            String body,
            byte[] attachment,
            String fileName
    ) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setSubject(subject);

        // IMPORTANT: true => le body est interprété comme HTML
        helper.setText(body, true);

        // Pièce jointe optionnelle
        if (attachment != null && attachment.length > 0
                && fileName != null && !fileName.isBlank()) {
            helper.addAttachment(fileName, new ByteArrayResource(attachment));
        }

        mailSender.send(mimeMessage);
    }

    // ============================================================
    // Email HTML avec PLUSIEURS pièces jointes
    // ============================================================
    public void sendEmailWithAttachments(
            String to,
            String subject,
            String body,
            Map<String, byte[]> attachments
    ) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setSubject(subject);

        // IMPORTANT: true => le body est interprété comme HTML
        helper.setText(body, true);

        // Pièces jointes multiples (PDF + QR code, etc.)
        if (attachments != null && !attachments.isEmpty()) {
            for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileContent = entry.getValue();
                
                if (fileContent != null && fileContent.length > 0
                        && fileName != null && !fileName.isBlank()) {
                    helper.addAttachment(fileName, new ByteArrayResource(fileContent));
                }
            }
        }

        mailSender.send(mimeMessage);
    }}