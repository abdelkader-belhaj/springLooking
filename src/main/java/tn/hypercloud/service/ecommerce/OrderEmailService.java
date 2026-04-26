package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.hypercloud.dto.ecommerce.OrderDTO;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {
    private final JavaMailSender mailSender;
    private final InvoiceService invoiceService;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    /**
     * Send order confirmation email with PDF invoice attachment
     */
    public void sendOrderConfirmationEmail(OrderDTO order) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, fromName);
        helper.setTo(order.getClientEmail());
        helper.setSubject("Confirmation de votre commande - Commande #" + order.getOrderNumber());
        helper.setText(buildEmailBody(order), true);

        // ✅ Try to attach PDF — but don't fail if it breaks
        try {
            byte[] pdfBytes = invoiceService.generateInvoicePDF(order);
            if (pdfBytes != null && pdfBytes.length > 0) {
                String filename = "facture-" + order.getOrderNumber() + ".pdf";
                helper.addAttachment(filename, new ByteArrayResource(pdfBytes), "application/pdf");
                log.info("✅ PDF invoice attached for order {}", order.getOrderNumber());
            }
        } catch (Exception pdfEx) {
            log.warn("⚠️ PDF generation failed for order {}, sending email without attachment: {}",
                    order.getOrderNumber(), pdfEx.getMessage());
        }

        mailSender.send(message);
        log.info("✅ Order confirmation email sent to {} for order {}",
                order.getClientEmail(), order.getOrderNumber());

    } catch (MessagingException e) {
        log.error("❌ Failed to send order confirmation email for order {}: {}",
                order.getOrderNumber(), e.getMessage(), e);
    } catch (Exception e) {
        log.error("❌ Unexpected error sending order confirmation email for order {}: {}",
                order.getOrderNumber(), e.getMessage(), e);
    }
}

    /**
     * Build HTML email body
     */
    /**
     * Build HTML email body
     */
    private String buildEmailBody(OrderDTO order) {
        String discountRow = (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                ? String.format("""
                    <tr style="color: #dc2626;">
                        <td style="padding: 12px; border-bottom: 1px solid #edf2f7;">Remise</td>
                        <td style="padding: 12px; border-bottom: 1px solid #edf2f7; text-align: right;">-%.2f TND</td>
                    </tr>
                """, order.getDiscountAmount())
                : "";

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #1a202c; margin: 0; padding: 0; background-color: #f7fafc; }
                    .wrapper { width: 100%%; table-layout: fixed; background-color: #f7fafc; padding-bottom: 40px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; margin-top: 40px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }
                    .header { background: linear-gradient(135deg, #003974 0%%, #002a54 100%%); color: #ffffff; padding: 40px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; letter-spacing: -0.025em; }
                    .content { padding: 40px; }
                    .greeting { font-size: 20px; font-weight: 600; margin-bottom: 16px; color: #2d3748; }
                    .intro { color: #4a5568; margin-bottom: 32px; }
                    .order-card { background-color: #f8fafc; border: 1px solid #edf2f7; border-radius: 8px; padding: 24px; margin-bottom: 32px; }
                    .order-number { color: #003974; font-weight: 700; font-size: 18px; margin-bottom: 4px; }
                    .order-date { color: #718096; font-size: 14px; }
                    .section-title { font-size: 16px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: #4a5568; margin-bottom: 16px; border-bottom: 2px solid #edf2f7; padding-bottom: 8px; }
                    .detail-table { width: 100%%; border-collapse: collapse; margin-bottom: 32px; }
                    .detail-table th { text-align: left; font-size: 12px; font-weight: 700; text-transform: uppercase; color: #718096; padding: 12px; background-color: #f8fafc; }
                    .detail-table td { padding: 12px; border-bottom: 1px solid #edf2f7; font-size: 15px; }
                    .total-row { font-weight: 700; font-size: 18px; color: #003974; }
                    .info-grid { display: table; width: 100%%; margin-bottom: 32px; }
                    .info-col { display: table-cell; width: 50%%; padding-right: 20px; vertical-align: top; }
                    .status-badge { display: inline-block; padding: 6px 12px; background-color: #ebf8ff; color: #2b6cb0; border-radius: 9999px; font-size: 14px; font-weight: 600; margin-top: 8px; }
                    .footer { text-align: center; padding: 32px; color: #a0aec0; font-size: 14px; }
                    .pdf-note { background-color: #fffaf0; border: 1px solid #feebc8; color: #9c4221; padding: 16px; border-radius: 8px; font-size: 14px; margin-bottom: 32px; display: flex; align-items: center; }
                    @media screen and (max-width: 600px) {
                        .info-col { display: block; width: 100%%; padding-right: 0; margin-bottom: 24px; }
                    }
                </style>
            </head>
            <body>
                <div class="wrapper">
                    <div class="container">
                        <div class="header">
                            <h1>TunisiaTour</h1>
                            <div style="margin-top: 8px; opacity: 0.8; font-size: 16px;">Confirmation de votre commande</div>
                        </div>

                        <div class="content">
                            <div class="greeting">Bonjour %s,</div>
                            <p class="intro">Merci de votre confiance ! Votre commande a été bien reçue et notre équipe s'occupe de sa préparation avec le plus grand soin.</p>

                            <div class="order-card">
                                <div class="order-number">Commande #%s</div>
                                <div class="order-date">Passée le %s</div>
                            </div>

                            <div class="section-title">Récapitulatif</div>
                            <table class="detail-table">
                                <thead>
                                    <tr>
                                        <th>Description</th>
                                        <th style="text-align: right;">Montant</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="color: #4a5568;">Sous-total</td>
                                        <td style="text-align: right; font-weight: 600;">%.2f TND</td>
                                    </tr>
                                    %s
                                    <tr class="total-row">
                                        <td style="padding-top: 20px;">Total Final</td>
                                        <td style="text-align: right; padding-top: 20px;">%.2f TND</td>
                                    </tr>
                                </tbody>
                            </table>

                            <div class="info-grid">
                                <div class="info-col">
                                    <div class="section-title">Livraison</div>
                                    <div style="color: #4a5568; font-size: 14px; line-height: 1.4;">
                                        %s
                                    </div>
                                </div>
                                <div class="info-col">
                                    <div class="section-title">Paiement</div>
                                    <div style="color: #4a5568; font-size: 14px;">
                                        <strong>Méthode :</strong> %s<br>
                                        <div class="status-badge">
                                            Statut : %s
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="pdf-note">
                                📄 <strong>Note :</strong> Votre facture officielle est jointe à cet e-mail au format PDF.
                            </div>

                            <div style="margin-top: 40px; padding-top: 32px; border-top: 1px solid #edf2f7; color: #718096; font-size: 14px;">
                                <p>Besoin d'aide ? Notre service client est à votre disposition.</p>
                                <p>À bientôt sur TunisiaTour,<br><strong>L'équipe eCommerce</strong></p>
                            </div>
                        </div>

                        <div class="footer">
                            <p>&copy; 2026 TunisiaTour Marketplace. Tous droits réservés.</p>
                            <p>Ceci est un message automatique, merci de ne pas y répondre.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                order.getClientName(),
                order.getOrderNumber(),
                order.getCreatedAt() != null ? order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", java.util.Locale.FRENCH)) : "N/A",
                order.getSubtotal(),
                discountRow,
                order.getTotalAmount(),
                order.getShippingAddress(),
                order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A",
                getStatusLabel(order.getStatus())
            );
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "pending" -> "En attente";
            case "paid" -> "Payée";
            case "shipped" -> "Expédiée";
            case "delivered" -> "Livrée";
            case "cancelled" -> "Annulée";
            default -> status;
        };
    }

    /**
     * Helper class for email attachment
     */
    public static class ByteArrayResource extends org.springframework.core.io.ByteArrayResource {
        private final String filename;

        public ByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        public ByteArrayResource(byte[] byteArray) {
            super(byteArray);
            this.filename = "attachment";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
