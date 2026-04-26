package tn.hypercloud.service.ecommerce;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.ecommerce.PromoCode;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public void sendPromoCodeEmail(String recipientEmail, PromoCode promoCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject("🎁 Votre code promo exclusif - " + promoCode.getCode());
            helper.setText(buildEmailBody(promoCode), true);

            mailSender.send(message);
            log.info("✅ Promo code email sent to {} for code {}", recipientEmail, promoCode.getCode());

        } catch (MessagingException e) {
            log.error("❌ Failed to send promo email to {}: {}", recipientEmail, e.getMessage());
            throw new RuntimeException("Failed to send promo code email: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error sending promo email: {}", e.getMessage());
            throw new RuntimeException("Unexpected error: " + e.getMessage());
        }
    }

    private String buildEmailBody(PromoCode promoCode) {
        String maxUsesText = promoCode.getMaxUses() != null
                ? "Ce code peut être utilisé <strong>" + promoCode.getMaxUses() + " fois</strong>."
                : "Ce code peut être utilisé un <strong>nombre illimité de fois</strong>.";

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #003974; color: white; padding: 30px 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .header h1 { margin: 0; font-size: 26px; }
                    .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .code-box { background-color: #003974; color: white; font-family: monospace; font-size: 32px; font-weight: bold; letter-spacing: 6px; text-align: center; padding: 20px; border-radius: 8px; margin: 25px 0; }
                    .discount-badge { background-color: #e8f4f8; border: 2px solid #003974; border-radius: 8px; padding: 15px; text-align: center; margin: 20px 0; }
                    .discount-badge .percent { font-size: 48px; font-weight: bold; color: #003974; line-height: 1; }
                    .discount-badge .label { font-size: 14px; color: #555; margin-top: 5px; }
                    .info-box { background-color: white; border: 1px solid #ddd; border-radius: 8px; padding: 15px; margin: 20px 0; }
                    .footer { background-color: #f0f0f0; padding: 15px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 8px 8px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎁 Votre Code Promo Exclusif</h1>
                        <p style="margin: 8px 0 0 0; opacity: 0.9;">TunisiaTour vous offre une réduction spéciale</p>
                    </div>

                    <div class="content">
                        <p>Bonjour,</p>
                        <p>Nous avons le plaisir de vous offrir un code promo exclusif. Utilisez-le lors de votre prochaine commande!</p>

                        <div class="discount-badge">
                            <div class="percent">%s%%</div>
                            <div class="label">de réduction sur votre commande</div>
                        </div>

                        <p style="text-align:center; font-size: 14px; color: #555; margin-bottom: 5px;">Votre code promo :</p>
                        <div class="code-box">%s</div>

                        <div class="info-box">
                            <p style="margin: 0 0 8px 0;">📋 <strong>Conditions d'utilisation :</strong></p>
                            <p style="margin: 0 0 5px 0;">%s</p>
                            <p style="margin: 0; color: #888; font-size: 13px;">Code valide dès maintenant.</p>
                        </div>

                        <p>Profitez-en pour explorer nos offres et découvrir la Tunisie!</p>
                        <p>Cordialement,<br><strong>Équipe TunisiaTour</strong></p>
                    </div>

                    <div class="footer">
                        <p>&copy; 2026 TunisiaTour. Tous droits réservés.</p>
                        <p>Cet email a été envoyé automatiquement. Veuillez ne pas y répondre directement.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                promoCode.getDiscountPercentage(),
                promoCode.getCode(),
                maxUsesText
            );
    }
}
