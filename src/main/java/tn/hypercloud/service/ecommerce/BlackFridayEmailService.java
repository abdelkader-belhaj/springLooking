package tn.hypercloud.service.ecommerce;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.user.UserRepository;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlackFridayEmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    /**
     * Runs at 9:00 AM every day in November
     * Only sends emails on the last Friday of November (Black Friday)
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 9 * 11 *")
    public void sendBlackFridayEmails() {
        // Check if today is the last Friday of November
        if (!isLastFridayOfNovember()) {
            return;
        }

        log.info("🛍️ Starting Black Friday email campaign...");

        List<User> clients = userRepository.findByRole(Role.CLIENT_TOURISTE);

        if (clients.isEmpty()) {
            log.info("No clients found for Black Friday campaign");
            return;
        }

        int sent = 0;
        for (User client : clients) {
            try {
                sendBlackFridayEmail(client.getEmail(), client.getUsername());
                sent++;
            } catch (Exception e) {
                log.error("❌ Failed to send Black Friday email to {}: {}",
                        client.getEmail(), e.getMessage());
            }
        }

        log.info("✅ Black Friday campaign complete — sent to {}/{} clients",
                sent, clients.size());
    }

    /**
     * Checks if today is the last Friday of November
     * Black Friday is always the last Friday of November
     */
    private boolean isLastFridayOfNovember() {
        LocalDate today = LocalDate.now();
        
        // Must be in November
        if (today.getMonthValue() != 11) {
            return false;
        }
        
        // Must be a Friday
        if (today.getDayOfWeek() != DayOfWeek.FRIDAY) {
            return false;
        }
        
        // Must be the last Friday of November (check if adding 7 days goes to December)
        LocalDate nextWeek = today.plusDays(7);
        return nextWeek.getMonthValue() == 12;
    }

    /**
     * Can also be triggered manually from the admin controller
     */
    public void sendBlackFridayEmailsManual() {
        sendBlackFridayEmails();
    }

    private void sendBlackFridayEmail(String recipientEmail, String username)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, fromName);
        helper.setTo(recipientEmail);
        helper.setSubject("🖤 BLACK FRIDAY — Offres exclusives TunisiaTour!");
        helper.setText(buildBlackFridayEmailBody(username), true);

        mailSender.send(message);
    }

    private String buildBlackFridayEmailBody(String username) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; color: #333;
                       margin: 0; padding: 0; background: #000; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: linear-gradient(135deg, #1a1a1a, #2d1b4e);
                          color: white; padding: 40px 20px;
                          text-align: center; border-radius: 8px 8px 0 0; }
                .header h1 { font-size: 42px; margin: 0; letter-spacing: 4px;
                             color: #FFD700; text-shadow: 0 0 20px #FFD700; }
                .header .subtitle { color: #ccc; font-size: 16px; margin-top: 10px; }
                .content { background-color: #1a1a1a; padding: 30px;
                           border: 1px solid #333; color: #eee; }
                .offer-box { background: linear-gradient(135deg, #2d1b4e, #1a1a1a);
                             border: 2px solid #FFD700; border-radius: 8px;
                             padding: 25px; margin: 20px 0; text-align: center; }
                .discount { font-size: 64px; font-weight: bold; color: #FFD700;
                            line-height: 1; text-shadow: 0 0 30px #FFD700; }
                .discount-label { color: #ccc; font-size: 16px; margin-top: 5px; }
                .deals { display: grid; gap: 15px; margin: 20px 0; }
                .deal-item { background: #2a2a2a; border-left: 4px solid #FFD700;
                             padding: 15px; border-radius: 4px; }
                .deal-title { color: #FFD700; font-weight: bold; font-size: 16px; }
                .deal-desc { color: #ccc; font-size: 13px; margin-top: 5px; }
                .cta { display: block; background: linear-gradient(135deg, #FFD700, #FFA500);
                       color: #000; padding: 16px 30px; border-radius: 8px;
                       text-decoration: none; font-weight: bold; font-size: 18px;
                       text-align: center; margin: 25px 0;
                       letter-spacing: 1px; }
                .countdown { background: #FFD700; color: #000; padding: 10px;
                             text-align: center; border-radius: 4px;
                             font-weight: bold; margin: 15px 0; }
                .footer { background-color: #111; padding: 15px; text-align: center;
                          font-size: 12px; color: #666;
                          border-radius: 0 0 8px 8px; }
            </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🖤 BLACK FRIDAY</h1>
                        <div class="subtitle">Les meilleures offres de l'année sont là!</div>
                    </div>

                    <div class="content">
                        <p style="color:#eee">Bonjour <strong style="color:#FFD700">%s</strong>,</p>
                        <p style="color:#ccc">
                            Le Black Friday est arrivé chez TunisiaTour!
                            Profitez de réductions incroyables sur nos produits artisanaux
                            et nos bons plans touristiques.
                        </p>

                        <div class="offer-box">
                            <div class="discount">-50%%</div>
                            <div class="discount-label">
                                jusqu'à 50%% de réduction sur tout le site
                            </div>
                        </div>

                        <div class="countdown">
                            ⏰ Offres valables du 22 au 29 novembre — Ne manquez pas ça!
                        </div>

                        <div class="deals">
                            <div class="deal-item">
                                <div class="deal-title">🎁 Produits Artisanaux</div>
                                <div class="deal-desc">
                                    Découvrez nos artisans tunisiens avec des prix
                                    exceptionnels sur toute la collection
                                </div>
                            </div>
                            <div class="deal-item">
                                <div class="deal-title">🗺️ Bons Plans Touristiques</div>
                                <div class="deal-desc">
                                    Explorez la Tunisie à prix réduit —
                                    excursions, activités et découvertes
                                </div>
                            </div>
                            <div class="deal-item">
                                <div class="deal-title">💳 Codes Promo Exclusifs</div>
                                <div class="deal-desc">
                                    Des codes promo spéciaux Black Friday
                                    envoyés à nos clients fidèles
                                </div>
                            </div>
                        </div>

                        <a href="http://localhost:4200/marketplace" class="cta">
                            🛍️ VOIR TOUTES LES OFFRES →
                        </a>

                        <p style="color:#888; font-size:13px; text-align:center">
                            Offres limitées dans le temps. Valables du 22 au 29 novembre.
                        </p>

                        <p style="color:#ccc">
                            Cordialement,<br>
                            <strong style="color:#FFD700">Équipe TunisiaTour</strong>
                        </p>
                    </div>

                    <div class="footer">
                        <p>&copy; 2026 TunisiaTour. Tous droits réservés.</p>
                        <p>Cet email a été envoyé automatiquement.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username != null ? username : "cher client");
    }
}
