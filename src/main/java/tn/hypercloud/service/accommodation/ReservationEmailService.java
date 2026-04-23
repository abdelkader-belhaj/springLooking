package tn.hypercloud.service.accommodation;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReservationEmailService
{

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendReservationConfirmationEmail(
            String toEmail, 
            String clientUsername, 
            String logementName, 
            LocalDate dateDebut, 
            LocalDate dateFin, 
            BigDecimal prixTotal) {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDateDebut = dateDebut.format(dateFormatter);
        String formattedDateFin = dateFin.format(dateFormatter);
        String formattedPrice = prixTotal.stripTrailingZeros().toPlainString();
                long nights = Math.max(ChronoUnit.DAYS.between(dateDebut, dateFin), 1);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
                        helper.setSubject("Confirmation de reservation - " + logementName);
            
            String htmlContent = """
                                <div style="margin:0; padding:0; background-color:#f1f5f9;">
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9; padding:24px 0;">
                                        <tr>
                                            <td align="center">
                                                <table role="presentation" width="640" cellspacing="0" cellpadding="0" style="max-width:640px; width:100%%; background:#ffffff; border:1px solid #dbeafe; border-radius:16px; overflow:hidden; font-family:Arial, Helvetica, sans-serif;">
                                                    <tr>
                                                        <td style="background:#1d4ed8; padding:26px 28px; color:#ffffff;">
                                                            <p style="margin:0; font-size:13px; letter-spacing:.4px; opacity:.95;">TunisiaTour</p>
                                                            <h1 style="margin:8px 0 0 0; font-size:26px; line-height:1.25; font-weight:800;">Reservation confirmee</h1>
                                                        </td>
                                                    </tr>

                                                    <tr>
                                                        <td style="padding:24px 28px 10px 28px; color:#0f172a;">
                                                            <p style="margin:0 0 10px 0; font-size:16px; line-height:1.5;">Bonjour <strong>%s</strong>,</p>
                                                            <p style="margin:0; font-size:15px; line-height:1.6; color:#334155;">Votre reservation est bien enregistree. Voici les details de votre sejour.</p>
                                                        </td>
                                                    </tr>

                                                    <tr>
                                                        <td style="padding:12px 28px 8px 28px;">
                                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;">
                                                                <tr>
                                                                    <td style="padding:12px 14px; font-size:14px; color:#475569; background:#f8fafc; width:45%%;">Logement</td>
                                                                    <td style="padding:12px 14px; font-size:14px; color:#0f172a; font-weight:700; background:#ffffff;">%s</td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="padding:12px 14px; font-size:14px; color:#475569; background:#f8fafc; border-top:1px solid #e2e8f0;">Utilisateur</td>
                                                                    <td style="padding:12px 14px; font-size:14px; color:#0f172a; font-weight:700; background:#ffffff; border-top:1px solid #e2e8f0;">%s</td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="padding:12px 14px; font-size:14px; color:#475569; background:#f8fafc; border-top:1px solid #e2e8f0;">Periode</td>
                                                                    <td style="padding:12px 14px; font-size:14px; color:#0f172a; font-weight:700; background:#ffffff; border-top:1px solid #e2e8f0;">Du %s au %s (%s nuit%s)</td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="padding:12px 14px; font-size:14px; color:#475569; background:#f8fafc; border-top:1px solid #e2e8f0;">Prix total</td>
                                                                    <td style="padding:12px 14px; font-size:18px; color:#1d4ed8; font-weight:800; background:#ffffff; border-top:1px solid #e2e8f0;">%s DT</td>
                                                                </tr>
                                                            </table>
                                                        </td>
                                                    </tr>

                                                    <tr>
                                                        <td style="padding:16px 28px 26px 28px;">
                                                            <div style="background:#ecfeff; border:1px solid #bae6fd; border-radius:10px; padding:12px 14px; color:#0f172a; font-size:13px; line-height:1.6;">
                                                                Vous pouvez gerer votre reservation depuis votre espace client (modification/annulation selon les regles de delai).
                                                            </div>
                                                            <p style="margin:16px 0 0 0; font-size:12px; color:#64748b;">Merci de votre confiance,<br><strong style="color:#1e293b;">L'equipe TunisiaTour</strong></p>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                                """.formatted(clientUsername, logementName, clientUsername, formattedDateDebut, formattedDateFin, nights, nights > 1 ? "s" : "", formattedPrice);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de confirmation : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible d'envoyer l'email de confirmation", e);
        }
    }
}
