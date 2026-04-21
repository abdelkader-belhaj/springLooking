package tn.hypercloud.entity.reservation.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.PaiementVol;
import tn.hypercloud.repository.reservation.PaiementVolRepository;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final PaiementVolRepository paiementRepo;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Signature invalide");
        }

        switch (event.getType()) {

            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) event
                        .getDataObjectDeserializer().getObject().orElse(null);
                if (intent != null) {
                    paiementRepo.findByReferenceTx(intent.getId()).ifPresent(p -> {
                        p.setStatut(PaiementVol.StatutPaiement.paye);
                        paiementRepo.save(p);
                    });
                }
            }

            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) event
                        .getDataObjectDeserializer().getObject().orElse(null);
                if (intent != null) {
                    paiementRepo.findByReferenceTx(intent.getId()).ifPresent(p -> {
                        p.setStatut(PaiementVol.StatutPaiement.echec);
                        paiementRepo.save(p);
                    });
                }
            }

            case "charge.refunded" -> {
                // Stripe confirme le remboursement côté serveur
                // Le statut est déjà mis à "rembourse" dans demanderAnnulation()
                // Ce cas sert de double-vérification
            }
        }

        return ResponseEntity.ok("ok");
    }
}