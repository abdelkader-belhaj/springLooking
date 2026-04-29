package tn.hypercloud.entity.reservation.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.ReclamationVol;
import tn.hypercloud.entity.reservation.ReservationVol;
import tn.hypercloud.entity.reservation.Vol;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from-address}")
    private String fromAddress;

    public EmailService(@Qualifier("gmailMailSender") JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ============================================================
    //  EMAIL CONFIRMATION PAIEMENT + QR CODE
    // ============================================================
    @Async
    public void envoyerEmailPaiement(ReservationVol res, String qrCodeBase64) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(res.getTouriste().getEmail());
            helper.setSubject("✅ Confirmation de paiement – " + res.getReference());

            String volRetourHtml = "";
            if (res.getVolRetour() != null) {
                volRetourHtml = """
                    <tr>
                        <td style="padding:8px 0;color:#64748b;font-size:14px;">Vol retour</td>
                        <td style="padding:8px 0;font-weight:700;font-size:14px;">
                            %s → %s &nbsp;|&nbsp; %s &nbsp;|&nbsp; %s %s
                        </td>
                    </tr>
                """.formatted(
                        res.getVolRetour().getDepart(),
                        res.getVolRetour().getArrivee(),
                        res.getVolRetour().getNumero(),
                        res.getVolRetour().getDateDepart().format(DATE_FMT),
                        res.getVolRetour().getHeureDepart().toString()
                );
            }

            // Section QR Code
            String qrSection = """
                <!-- QR Code billet -->
                <div style="text-align:center;margin:24px 0;">
                  <p style="color:#0f172a;font-size:15px;font-weight:700;margin:0 0 12px;">
                    🎫 Votre billet électronique
                  </p>
                  <p style="color:#64748b;font-size:13px;margin:0 0 16px;">
                    Présentez ce QR code à l'aéroport pour embarquer
                  </p>
                  <img src="data:image/png;base64,%s"
                       alt="QR Code billet"
                       style="width:200px;height:200px;border:3px solid #0ea5e9;
                              border-radius:12px;padding:8px;background:white;" />
                  <p style="color:#94a3b8;font-size:11px;margin:12px 0 0;">
                    Référence : <strong>%s</strong>
                  </p>
                </div>
            """.formatted(qrCodeBase64, res.getReference());

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f8fafc;font-family:sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:white;
                              border-radius:16px;overflow:hidden;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#0ea5e9,#0284c7);
                                padding:32px;text-align:center;">
                      <h1 style="color:white;margin:0;font-size:24px;">✅ Paiement confirmé</h1>
                      <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;">
                        Votre réservation est confirmée
                      </p>
                    </div>

                    <!-- Body -->
                    <div style="padding:32px;">
                      <p style="color:#0f172a;font-size:16px;margin:0 0 24px;">
                        Bonjour <strong>%s</strong>,
                      </p>
                      <p style="color:#475569;font-size:14px;margin:0 0 24px;">
                        Votre paiement a été effectué avec succès.
                        Voici le récapitulatif de votre réservation :
                      </p>

                      <!-- Référence -->
                      <div style="background:#f0f9ff;border:1px solid #bae6fd;
                                  border-radius:10px;padding:16px;
                                  text-align:center;margin-bottom:24px;">
                        <p style="color:#0369a1;font-size:12px;
                                  text-transform:uppercase;margin:0 0 4px;">
                          Référence de réservation
                        </p>
                        <p style="color:#0284c7;font-size:24px;
                                  font-weight:900;margin:0;letter-spacing:2px;">
                          %s
                        </p>
                      </div>

                      <!-- Détails -->
                      <table style="width:100%%;border-collapse:collapse;margin-bottom:24px;">
                        <tr style="border-bottom:1px solid #e2e8f0;">
                          <td style="padding:8px 0;color:#64748b;font-size:14px;">Vol aller</td>
                          <td style="padding:8px 0;font-weight:700;font-size:14px;">
                            %s → %s &nbsp;|&nbsp; %s &nbsp;|&nbsp; %s %s
                          </td>
                        </tr>
                        %s
                        <tr style="border-bottom:1px solid #e2e8f0;">
                          <td style="padding:8px 0;color:#64748b;font-size:14px;">Passagers</td>
                          <td style="padding:8px 0;font-weight:700;font-size:14px;">%d</td>
                        </tr>
                        <tr style="border-bottom:1px solid #e2e8f0;">
                          <td style="padding:8px 0;color:#64748b;font-size:14px;">Type billet</td>
                          <td style="padding:8px 0;font-weight:700;font-size:14px;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:8px 0;color:#64748b;font-size:14px;">Prix total</td>
                          <td style="padding:8px 0;font-weight:900;
                                     font-size:18px;color:#0ea5e9;">
                            %s TND
                          </td>
                        </tr>
                      </table>

                      <!-- Paiement info -->
                      <div style="background:#dcfce7;border-radius:10px;
                                  padding:16px;margin-bottom:24px;">
                        <p style="color:#166534;font-size:14px;margin:0;font-weight:600;">
                          ✅ Paiement par carte bancaire – Traité via Stripe
                        </p>
                        <p style="color:#166534;font-size:13px;margin:6px 0 0;">
                          Date : %s
                        </p>
                      </div>

                      <!-- ✅ QR CODE BILLET -->
                      %s

                      <p style="color:#94a3b8;font-size:12px;text-align:center;margin:0;">
                        Merci de votre confiance – HyperCloud Travel
                      </p>
                    </div>
                  </div>
                </body>
                </html>
            """.formatted(
                    res.getTouriste().getUsername(),   // 1 - Bonjour
                    res.getReference(),                // 2 - Référence
                    res.getVolAller().getDepart(),     // 3 - Départ aller
                    res.getVolAller().getArrivee(),    // 4 - Arrivée aller
                    res.getVolAller().getNumero(),     // 5 - Numéro vol aller
                    res.getVolAller().getDateDepart().format(DATE_FMT), // 6
                    res.getVolAller().getHeureDepart().toString(),      // 7
                    volRetourHtml,                     // 8 - Vol retour (ou vide)
                    (int) res.getNbPassagers(),        // 9 - Passagers
                    res.getTypeBillet().toString().replace("_", " "),   // 10 - Type billet
                    res.getPrixTotal().toString(),     // 11 - Prix
                    res.getPaiement().getDatePaiement().format(DATETIME_FMT), // 12 - Date paiement
                    qrSection                          // 13 - QR Code
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Erreur envoi email paiement : " + e.getMessage());
        }
    }

    // ============================================================
    //  EMAIL CONFIRMATION ANNULATION + REMBOURSEMENT
    //  (inchangé)
    // ============================================================
    @Async
    public void envoyerEmailAnnulation(ReservationVol res) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(res.getTouriste().getEmail());
            helper.setSubject("❌ Annulation & Remboursement – " + res.getReference());

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f8fafc;font-family:sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:white;
                              border-radius:16px;overflow:hidden;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#ef4444,#dc2626);
                                padding:32px;text-align:center;">
                      <h1 style="color:white;margin:0;font-size:24px;">
                        ❌ Réservation annulée
                      </h1>
                      <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;">
                        Votre remboursement a été initié
                      </p>
                    </div>

                    <!-- Body -->
                    <div style="padding:32px;">
                      <p style="color:#0f172a;font-size:16px;margin:0 0 24px;">
                        Bonjour <strong>%s</strong>,
                      </p>
                      <p style="color:#475569;font-size:14px;margin:0 0 24px;">
                        Votre réservation a été annulée et le remboursement
                        a été initié automatiquement via Stripe.
                      </p>

                      <!-- Référence -->
                      <div style="background:#fef2f2;border:1px solid #fecaca;
                                  border-radius:10px;padding:16px;
                                  text-align:center;margin-bottom:24px;">
                        <p style="color:#dc2626;font-size:12px;
                                  text-transform:uppercase;margin:0 0 4px;">
                          Référence annulée
                        </p>
                        <p style="color:#b91c1c;font-size:24px;
                                  font-weight:900;margin:0;letter-spacing:2px;">
                          %s
                        </p>
                      </div>

                      <!-- Détails remboursement -->
                      <table style="width:100%%;border-collapse:collapse;margin-bottom:24px;">
                        <tr style="border-bottom:1px solid #e2e8f0;">
                          <td style="padding:8px 0;color:#64748b;font-size:14px;">Vol aller</td>
                          <td style="padding:8px 0;font-weight:700;font-size:14px;">
                            %s → %s
                          </td>
                        </tr>
                        <tr style="border-bottom:1px solid #e2e8f0;">
                          <td style="padding:8px 0;color:#64748b;font-size:14px;">
                            Montant remboursé
                          </td>
                          <td style="padding:8px 0;font-weight:900;
                                     font-size:18px;color:#0369a1;">
                            %s TND
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:8px 0;color:#64748b;font-size:14px;">
                            Date annulation
                          </td>
                          <td style="padding:8px 0;font-weight:700;font-size:14px;">
                            %s
                          </td>
                        </tr>
                      </table>

                      <!-- Remboursement info -->
                      <div style="background:#e0f2fe;border-radius:10px;
                                  padding:16px;margin-bottom:24px;">
                        <p style="color:#0369a1;font-size:14px;margin:0;font-weight:600;">
                          💳 Remboursement traité via Stripe
                        </p>
                        <p style="color:#0369a1;font-size:13px;margin:6px 0 0;">
                          Le montant sera crédité sur votre carte sous 5 à 10 jours ouvrables.
                        </p>
                      </div>

                      <p style="color:#94a3b8;font-size:12px;text-align:center;margin:0;">
                        HyperCloud Travel – Service client
                      </p>
                    </div>
                  </div>
                </body>
                </html>
            """.formatted(
                    res.getTouriste().getUsername(),
                    res.getReference(),
                    res.getVolAller().getDepart(),
                    res.getVolAller().getArrivee(),
                    res.getPrixTotal().toString(),
                    res.getPaiement().getDatePaiement().format(DATETIME_FMT)
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Erreur envoi email annulation : " + e.getMessage());
        }
    }

    // ============================================================
    //  EMAIL NOTIFICATION RETARD
    // ============================================================
    @Async
    public void envoyerEmailRetard(ReservationVol res, Vol volRetarde, int minutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(res.getTouriste().getEmail());
            helper.setSubject("⚠️ Notification de retard – " + res.getReference());

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#fff7ed;font-family:sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:white;
                              border-radius:16px;overflow:hidden;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#f59e0b,#d97706);
                                padding:32px;text-align:center;">
                      <h1 style="color:white;margin:0;font-size:24px;">⚠️ Retard de vol</h1>
                      <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;">
                        Information importante sur votre voyage
                      </p>
                    </div>

                    <!-- Body -->
                    <div style="padding:32px;">
                      <p style="color:#0f172a;font-size:16px;margin:0 0 24px;">
                        Bonjour <strong>%s</strong>,
                      </p>
                      <p style="color:#475569;font-size:14px;margin:0 0 24px;">
                        Nous vous informons qu'un retard a été signalé pour un vol de votre réservation.
                      </p>

                      <!-- Retard info -->
                      <div style="background:#fffbeb;border:1px solid #fde68a;
                                  border-radius:10px;padding:20px;
                                  text-align:center;margin-bottom:24px;">
                        <p style="color:#92400e;font-size:12px;
                                  text-transform:uppercase;margin:0 0 4px;">
                          Temps de retard estimé
                        </p>
                        <p style="color:#d97706;font-size:28px;
                                  font-weight:900;margin:0;">
                          +%d minutes
                        </p>
                      </div>

                      <!-- Détails Vol -->
                      <div style="background:#f8fafc;border-radius:10px;padding:16px;margin-bottom:24px;">
                         <p style="color:#64748b;font-size:13px;margin:0 0 8px;">Détails du vol concerné :</p>
                         <p style="color:#0f172a;font-weight:700;margin:0;">
                            %s → %s | Vol %s
                         </p>
                         <p style="color:#64748b;font-size:13px;margin:4px 0 0;">
                            Départ prévu : %s à %s
                         </p>
                      </div>

                      <p style="color:#475569;font-size:14px;margin:24px 0;">
                        Nous vous prions de nous excuser pour ce désagrément indépendant de notre volonté.
                      </p>

                      <p style="color:#94a3b8;font-size:12px;text-align:center;margin:0;">
                        HyperCloud Travel – Service Opérations
                      </p>
                    </div>
                  </div>
                </body>
                </html>
            """.formatted(
                    res.getTouriste().getUsername(),
                    minutes,
                    volRetarde.getDepart(),
                    volRetarde.getArrivee(),
                    volRetarde.getNumero(),
                    volRetarde.getDateDepart().format(DATE_FMT),
                    volRetarde.getHeureDepart().toString()
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Erreur envoi email retard : " + e.getMessage());
        }
    }

    // ============================================================
    //  EMAIL : RÉPONSE À UNE RÉCLAMATION
    // ============================================================
    @Async
    public void envoyerEmailReponseReclamation(ReclamationVol reclamation) {
        try {
            if (reclamation == null || reclamation.getTouriste() == null) {
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(reclamation.getTouriste().getEmail());
            helper.setSubject("📩 Réponse à votre réclamation");

            String ref = reclamation.getReservation() != null ? reclamation.getReservation().getReference() : null;
            String refLine = (ref != null && !ref.isBlank())
                    ? "<p style=\"margin:0 0 12px;color:#475569;font-size:14px;\">Réservation : <strong>" + ref + "</strong></p>"
                    : "";

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f8fafc;font-family:sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:white;
                              border-radius:16px;overflow:hidden;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08);">
                    <div style="background:linear-gradient(135deg,#0ea5e9,#0284c7);
                                padding:28px;text-align:center;">
                      <h1 style="color:white;margin:0;font-size:20px;">📩 Réponse à votre réclamation</h1>
                      <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:13px;">
                        Une réponse est disponible dans votre espace client
                      </p>
                    </div>

                    <div style="padding:28px;">
                      <p style="color:#0f172a;font-size:15px;margin:0 0 16px;">
                        Bonjour <strong>%s</strong>,
                      </p>
                      %s

                      <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px;margin:16px 0;">
                        <p style="margin:0 0 8px;color:#64748b;font-size:12px;text-transform:uppercase;">Votre sujet</p>
                        <p style="margin:0;color:#0f172a;font-size:14px;line-height:1.5;">%s</p>
                      </div>

                      <div style="background:#ecfeff;border:1px solid #a5f3fc;border-radius:12px;padding:16px;margin:16px 0;">
                        <p style="margin:0 0 8px;color:#155e75;font-size:12px;text-transform:uppercase;">Notre réponse</p>
                        <p style="margin:0;color:#0f172a;font-size:14px;line-height:1.5;">%s</p>
                      </div>

                      <p style="color:#94a3b8;font-size:12px;text-align:center;margin:18px 0 0;">
                        HyperCloud Travel – Service client
                      </p>
                    </div>
                  </div>
                </body>
                </html>
            """.formatted(
                    reclamation.getTouriste().getUsername(),
                    refLine,
                    safeHtml(reclamation.getSujet()),
                    safeHtml(reclamation.getReponse())
            );

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur envoi email réclamation : " + e.getMessage());
        }
    }

    private String safeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br/>");
    }
}