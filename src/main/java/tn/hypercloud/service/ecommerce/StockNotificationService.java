package tn.hypercloud.service.ecommerce;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.user.UserRepository;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockNotificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public void notifyBackInStock(Product product) {
        // Get all CLIENT_TOURISTE users
        List<User> clients = userRepository.findByRole(Role.CLIENT_TOURISTE);

        if (clients.isEmpty()) {
            log.info("No clients to notify for product: {}", product.getName());
            return;
        }

        int sent = 0;
        for (User client : clients) {
            try {
                sendBackInStockEmail(client.getEmail(), product);
                sent++;
            } catch (Exception e) {
                log.error("❌ Failed to send stock notification to {}: {}",
                        client.getEmail(), e.getMessage());
            }
        }
        log.info("✅ Stock notification sent to {}/{} clients for product: {}",
                sent, clients.size(), product.getName());
    }

    private void sendBackInStockEmail(String recipientEmail, Product product)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, fromName);
        helper.setTo(recipientEmail);
        helper.setSubject("🎉 " + product.getName() + " est de nouveau disponible!");
        helper.setText(buildStockEmailBody(product), true);

        mailSender.send(message);
    }

    private String buildStockEmailBody(Product product) {
        String priceInfo = product.getDiscountPrice() != null
                ? String.format("<span style='text-decoration:line-through;color:#999'>%.2f TND</span>" +
                  " <span style='color:#e53e3e;font-weight:bold'>%.2f TND</span>",
                  product.getPrice(), product.getDiscountPrice())
                : String.format("<strong>%.2f TND</strong>", product.getPrice());

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #003974; color: white; padding: 30px 20px;
                          text-align: center; border-radius: 8px 8px 0 0; }
                .content { background-color: #f9f9f9; padding: 30px;
                           border: 1px solid #ddd; }
                .product-box { background: white; border: 2px solid #003974;
                               border-radius: 8px; padding: 20px; margin: 20px 0;
                               text-align: center; }
                .product-name { font-size: 22px; font-weight: bold;
                                color: #003974; margin-bottom: 10px; }
                .badge { display: inline-block; background-color: #48bb78;
                         color: white; padding: 6px 16px; border-radius: 20px;
                         font-size: 13px; font-weight: bold; margin-bottom: 15px; }
                .cta { display: inline-block; background-color: #003974; color: white;
                       padding: 12px 30px; border-radius: 8px; text-decoration: none;
                       font-weight: bold; font-size: 15px; margin-top: 20px; }
                .footer { background-color: #f0f0f0; padding: 15px; text-align: center;
                          font-size: 12px; color: #666; border-radius: 0 0 8px 8px; }
            </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Bonne Nouvelle!</h1>
                        <p style="margin:8px 0 0 0; opacity:0.9;">
                            Un produit que vous pourriez aimer est de nouveau disponible
                        </p>
                    </div>
                    <div class="content">
                        <p>Bonjour,</p>
                        <p>Un produit est de nouveau en stock sur TunisiaTour!</p>

                        <div class="product-box">
                            <div class="badge">✓ De nouveau en stock</div>
                            <div class="product-name">%s</div>
                            <p style="color:#666; font-size:14px; margin:10px 0">%s</p>
                            <div style="font-size:18px; margin:10px 0">%s</div>
                            <p style="color:#48bb78; font-weight:bold; font-size:14px">
                                Stock disponible: %d unité(s)
                            </p>
                        </div>

                        <p style="text-align:center">
                            <a href="http://localhost:4200/marketplace" class="cta">
                                Voir le produit →
                            </a>
                        </p>

                        <p>Ne tardez pas — les stocks peuvent partir vite!</p>
                        <p>Cordialement,<br><strong>Équipe TunisiaTour</strong></p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 TunisiaTour. Tous droits réservés.</p>
                        <p>Cet email a été envoyé automatiquement.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                product.getName(),
                product.getDescription() != null
                    ? product.getDescription().substring(0,
                        Math.min(product.getDescription().length(), 100)) + "..."
                    : "",
                priceInfo,
                product.getStockQuantity()
            );
    }
}
