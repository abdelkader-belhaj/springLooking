package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.event.EventTicket;
import tn.hypercloud.exception.GlobalExceptionHandler.ApiException;
import tn.hypercloud.repository.event.EventTicketRepository;
import tn.hypercloud.service.PasswordResetEmailService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventEmailService {

    // Consommation du service de ton ami
    // Tout passe par son service !
    private final PasswordResetEmailService passwordResetEmailService;
    private final InvoiceService invoiceService;
    private final EventTicketRepository ticketRepository;

    // ============================================================
    // Email confirmation réservation + PDF Facture
    // ============================================================
    public void sendReservationConfirmation(
            String toEmail,
            String clientName,
            String eventTitle,
            String eventDate,
            String eventAddress,
            int numberOfTickets,
            BigDecimal totalPrice,
            Integer reservationId
    ) {
        // Calculer prix unitaire (pour la facture)
        BigDecimal unitPrice = numberOfTickets > 0
          ? totalPrice.divide(BigDecimal.valueOf(numberOfTickets), 2, RoundingMode.HALF_UP)
                : totalPrice;

        // Générer facture PDF
        byte[] invoicePdf;
        try {
            // Parser la date pour LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime parsedDate = LocalDateTime.parse(eventDate, formatter);

            invoicePdf = invoiceService.generateInvoicePdf(
                    clientName,
                    toEmail,
                    eventTitle,
                    parsedDate,
                    eventAddress,
                    numberOfTickets,
                    unitPrice,
                    totalPrice,
                    reservationId
            );
        } catch (Exception e) {
            log.error("Invoice PDF generation failed", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur generation facture: " + e.getMessage());
        }

        // Préparer l'attachment (juste la facture PDF)
        Map<String, byte[]> attachments = new HashMap<>();
        // Nom de facture pro : sans ID
        String invoiceName = "facture_" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
        attachments.put(invoiceName, invoicePdf);

        // Générer PDF billets (1 page par ticket)
        try {
            java.util.List<EventTicket> tickets = ticketRepository.findByReservationIdOrderByTicketNumberAsc(reservationId);
            if (!tickets.isEmpty()) {
          java.util.List<InvoiceService.TicketPdfItem> ticketItems = tickets.stream()
            .map(t -> InvoiceService.TicketPdfItem.builder()
              .ticketNumber(t.getTicketNumber())
              .ticketCode(t.getTicketCode())
              .build())
            .toList();

          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
          LocalDateTime parsedDate = LocalDateTime.parse(eventDate, formatter);

          byte[] ticketsPdf = invoiceService.generateTicketsPdf(
            clientName,
            eventTitle,
            parsedDate,
            eventAddress,
            reservationId,
            ticketItems
          );

          String ticketFileName = "billets_" + reservationId + "_" + java.time.LocalDate.now() + ".pdf";
          attachments.put(ticketFileName, ticketsPdf);
            }
        } catch (Exception e) {
            log.warn("Ticket PDF generation failed for reservation {}: {}", reservationId, e.getMessage());
        }

        String subject = "Confirmation de votre paiement";
        String body = buildConfirmationEmail(
                clientName, eventTitle, eventDate,
                eventAddress, numberOfTickets, totalPrice
        );

        try {
            passwordResetEmailService.sendEmailWithAttachments(
                    toEmail,
                    subject,
                    body,
                    attachments
            );
            log.info("Confirmation email sent to {} with PDF", toEmail);
        } catch (Exception e) {
            log.error("Mail send failed", e);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Echec envoi mail: " + e.getMessage());
        }
    }

    // ============================================================
    // Email annulation event + remboursement
    // ============================================================
    public void sendEventCancellationEmail(
            String toEmail,
            String clientName,
            String eventTitle,
            String formattedDate,
            BigDecimal amount
    ) {
        String subject = "Annulation - " + eventTitle;
        String body = buildCancellationEmail(clientName, eventTitle, formattedDate, amount);

        try {
            // Pas de pièce jointe pour annulation
            passwordResetEmailService.sendEmailWithAttachments(
                    toEmail,
                    subject,
                    body,
                    null
            );
            log.info("Cancellation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Mail send failed", e);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Echec envoi mail: " + e.getMessage());
        }
    }

    // ============================================================
    // Template confirmation paiement (HTML)
    // ============================================================
    private String buildConfirmationEmail(
            String clientName,
            String eventTitle,
            String eventDate,
            String eventAddress,
            int numberOfTickets,
            BigDecimal totalPrice
    ) {
        String safeName = escapeHtml(clientName);
        String safeTitle = escapeHtml(eventTitle);
        String safeDate = escapeHtml(eventDate);
        String safeAddress = escapeHtml(eventAddress);
        String safeAmount = totalPrice == null ? "0.00" : totalPrice.toPlainString();

        return """
             <div style="font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;max-width:620px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden">

               <div style="background:linear-gradient(135deg,#0f766e,#0ea5e9);color:#fff;padding:28px;text-align:center">
                 <h1 style="margin:0;font-size:28px;font-weight:700">✓ Paiement Confirmé</h1>
                 <p style="margin:10px 0 0 0;font-size:16px;opacity:.92">Merci pour votre réservation</p>
               </div>

               <div style="padding:28px;background:#f9fbff;color:#1f2937;line-height:1.6">
                 <p style="margin:0 0 16px 0;font-size:15px">Bonjour <strong style="color:#0f766e">%s</strong>,</p>
                 <p style="margin:0 0 24px 0;font-size:15px">Votre paiement a été traité avec succès. Votre facture est en pièce jointe.</p>

                 <div style="background:#fff;padding:20px;border-radius:10px;margin:24px 0;border-left:5px solid #0ea5e9;box-shadow:0 1px 3px rgba(0,0,0,0.08)">
                   <h3 style="margin:0 0 12px 0;color:#111827;font-size:16px;font-weight:600">Détails de votre réservation</h3>
                   <table style="width:100%;border-collapse:collapse;font-size:14px">
                     <tr>
                       <td style="padding:6px 0;color:#4b5563"><strong>Événement</strong></td>
                       <td style="padding:6px 0;text-align:right">%s</td>
                     </tr>
                     <tr style="border-top:1px solid #f0f0f0">
                       <td style="padding:6px 0;color:#4b5563"><strong>Date</strong></td>
                       <td style="padding:6px 0;text-align:right">%s</td>
                     </tr>
                     <tr style="border-top:1px solid #f0f0f0">
                       <td style="padding:6px 0;color:#4b5563"><strong>Lieu</strong></td>
                       <td style="padding:6px 0;text-align:right">%s</td>
                     </tr>
                     <tr style="border-top:1px solid #f0f0f0">
                       <td style="padding:6px 0;color:#4b5563"><strong>Places</strong></td>
                       <td style="padding:6px 0;text-align:right">%d</td>
                     </tr>
                     <tr style="border-top:1px solid #f0f0f0;border-bottom:2px solid #0ea5e9">
                       <td style="padding:6px 0;font-weight:600;color:#111827">Montant</td>
                       <td style="padding:6px 0;text-align:right;font-weight:600;color:#0ea5e9;font-size:16px">%s TND</td>
                     </tr>
                   </table>
                 </div>

                 <div style="background:#ecfdf3;padding:14px;border-radius:8px;margin:24px 0;border-left:4px solid #16a34a;font-size:14px;color:#166534">
                   <strong>✓ Votre facture</strong> est en pièce jointe. Consultez 'Mes réservations' sur la plateforme pour accéder à votre billet.
                 </div>

                 <p style="margin:28px 0 0 0;font-size:14px;color:#4b5563">
                   Cordialement,<br>
                   <strong style="color:#0f766e">L'équipe HyperCloud</strong>
                 </p>
               </div>

               <div style="background:#111827;color:#9ca3af;padding:16px;text-align:center;font-size:12px">
                 <p style="margin:0">© 2026 HyperCloud — Plateforme d'événements et voyages</p>
               </div>
             </div>
             """.formatted(
                safeName, safeTitle, safeDate, safeAddress, numberOfTickets, safeAmount
        );
    }

    // ============================================================
    // Template annulation (HTML)
    // ============================================================
    private String buildCancellationEmail(
            String clientName,
            String eventTitle,
            String formattedDate,
            BigDecimal amount
    ) {
        String safeName = escapeHtml(clientName);
        String safeTitle = escapeHtml(eventTitle);
        String safeDate = escapeHtml(formattedDate);
        String safeAmount = amount == null ? "0.00" : amount.toPlainString();

        return """
             <div style="font-family:Arial,sans-serif;max-width:620px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden">

               <div style="background:linear-gradient(135deg,#b91c1c,#ef4444);color:#fff;padding:22px;text-align:center">
                 <h1 style="margin:0;font-size:24px">Evenement annule</h1>
                 <p style="margin:8px 0 0 0;opacity:.95">Nous sommes desoles pour la gene occasionnee</p>
               </div>

               <div style="padding:22px;background:#fff7f7;color:#1f2937">
                 <p>Bonjour <strong>%s</strong>,</p>
                 <p>Nous vous informons que l'evenement suivant a ete annule :</p>

                 <div style="background:#fff;padding:16px;border-radius:10px;margin:18px 0;border-left:4px solid #ef4444">
                   <h3 style="margin-top:0;color:#111827">Details</h3>
                   <p><strong>Evenement :</strong> %s</p>
                   <p><strong>Date prevue :</strong> %s</p>
                 </div>

                 <div style="background:#ecfdf3;padding:16px;border-radius:10px;margin:18px 0;border-left:4px solid #16a34a">
                   <h3 style="margin-top:0;color:#166534">Remboursement</h3>
                   <p>Le montant de <strong>%s TND</strong> sera rembourse sous <strong>3 a 5 jours ouvrables</strong>.</p>
                 </div>

                 <p style="margin-top:28px">
                   Cordialement,<br>
                   <strong>L'equipe HyperCloud</strong>
                 </p>
               </div>

               <div style="background:#111827;color:#9ca3af;padding:14px;text-align:center;font-size:12px">
                 <p style="margin:0">© 2026 HyperCloud</p>
               </div>
             </div>
             """.formatted(safeName, safeTitle, safeDate, safeAmount);
    }

    // ============================================================
    // Escape HTML basique
    // ============================================================
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }}