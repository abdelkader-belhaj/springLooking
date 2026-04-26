package tn.hypercloud.service.ecommerce;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.ecommerce.Product;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStatusEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public void sendProductDisabledEmail(Product product) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(product.getUser().getEmail());
            helper.setSubject("Votre produit a été désactivé - Action requise");
            helper.setText(buildDisabledEmailBody(product), true);

            mailSender.send(message);
            log.info("Product disabled email sent to {} for product {}", product.getUser().getEmail(), product.getName());
        } catch (Exception e) {
            log.error("Failed to send product disabled email: {}", e.getMessage());
        }
    }

    public void sendProductEnabledEmail(Product product) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(product.getUser().getEmail());
            helper.setSubject("Votre produit a été réactivé");
            helper.setText(buildEnabledEmailBody(product), true);

            mailSender.send(message);
            log.info("Product enabled email sent to {} for product {}", product.getUser().getEmail(), product.getName());
        } catch (Exception e) {
            log.error("Failed to send product enabled email: {}", e.getMessage());
        }
    }

    private String buildDisabledEmailBody(Product product) {
        String recipientName = product.getUser().getUsername() != null ? product.getUser().getUsername() : "Artisan";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #b91c1c; color: white; padding: 30px 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .product-box { background: white; border: 2px solid #b91c1c; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .product-name { font-size: 20px; font-weight: bold; color: #b91c1c; }
                    .warning-box { background: #fef2f2; border-left: 4px solid #b91c1c; padding: 15px; margin: 15px 0; border-radius: 4px; }
                    .steps { background: white; border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .step { display: flex; align-items: flex-start; margin-bottom: 12px; }
                    .step-num { background: #003974; color: white; border-radius: 50%%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; margin-right: 10px; flex-shrink: 0; }
                    .footer { background-color: #f0f0f0; padding: 15px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 8px 8px; }
                </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Produit désactivé</h1>
                            <p style="margin:8px 0 0 0; opacity:0.9;">Action requise de votre part</p>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            <p>L'administrateur a désactivé l'un de vos produits sur TunisiaTour.</p>

                            <div class="product-box">
                                <div class="product-name">%s</div>
                                <p style="color:#666; margin:8px 0 0 0; font-size:14px">Prix: <strong>%.2f TND</strong></p>
                            </div>

                            <div class="warning-box">
                                <strong>Ce produit n'est plus visible par les clients</strong>
                                <p style="margin:8px 0 0 0; font-size:14px; color:#666">Les clients ne peuvent pas voir ni acheter ce produit tant qu'il est désactivé.</p>
                            </div>

                            <div class="steps">
                                <p><strong>Que faire maintenant ?</strong></p>
                                <div class="step"><div class="step-num">1</div><div>Connectez-vous à votre espace artisan</div></div>
                                <div class="step"><div class="step-num">2</div><div>Accédez à "Mes Produits" et trouvez le produit désactivé</div></div>
                                <div class="step"><div class="step-num">3</div><div>Apportez les modifications nécessaires</div></div>
                                <div class="step"><div class="step-num">4</div><div>L'administrateur pourra ensuite le réactiver après vérification</div></div>
                            </div>

                            <p>Si vous avez des questions, contactez notre support.</p>
                            <p>Cordialement,<br><strong>Équipe TunisiaTour</strong></p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 TunisiaTour. Tous droits réservés.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(recipientName, product.getName(), product.getPrice());
    }

    private String buildEnabledEmailBody(Product product) {
        String recipientName = product.getUser().getUsername() != null ? product.getUser().getUsername() : "Artisan";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #003974; color: white; padding: 30px 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .product-box { background: white; border: 2px solid #003974; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center; }
                    .product-name { font-size: 20px; font-weight: bold; color: #003974; }
                    .success-box { background: #f0fdf4; border-left: 4px solid #16a34a; padding: 15px; margin: 15px 0; border-radius: 4px; }
                    .footer { background-color: #f0f0f0; padding: 15px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 8px 8px; }
                </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Produit réactivé</h1>
                            <p style="margin:8px 0 0 0; opacity:0.9;">Bonne nouvelle !</p>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            <p>Votre produit a été réactivé par l'administrateur et est de nouveau visible par les clients.</p>

                            <div class="product-box">
                                <div class="product-name">%s</div>
                                <p style="color:#666; margin:8px 0 0 0; font-size:14px">Prix: <strong>%.2f TND</strong></p>
                            </div>

                            <div class="success-box">
                                <strong>Votre produit est maintenant visible par tous les clients</strong>
                                <p style="margin:8px 0 0 0; font-size:14px; color:#666">Les clients peuvent à nouveau voir et acheter ce produit.</p>
                            </div>

                            <p>Merci pour votre collaboration !</p>
                            <p>Cordialement,<br><strong>Équipe TunisiaTour</strong></p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 TunisiaTour. Tous droits réservés.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(recipientName, product.getName(), product.getPrice());
    }
}