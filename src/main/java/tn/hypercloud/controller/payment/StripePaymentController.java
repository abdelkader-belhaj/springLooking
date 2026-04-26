package tn.hypercloud.controller.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.config.StripeConfig;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"}, allowCredentials = "true")
public class StripePaymentController {

    private final StripeConfig stripeConfig;

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            // Extraire les données de la requête
            Long reservationId = Long.valueOf(request.get("reservationId").toString());
            Long logementId = Long.valueOf(request.get("logementId").toString());
            String logementName = request.get("logementName").toString();
            Long amountInCents = Long.valueOf(request.get("amountInCents").toString());
            String currency = request.get("currency").toString();

            log.info("Creating payment intent for reservation: {}, logement: {}, amount: {} {}", 
                    reservationId, logementId, amountInCents, currency);

            // Créer le PaymentIntent avec Stripe
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency)
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build())
                    .putMetadata("reservation_id", reservationId.toString())
                    .putMetadata("logement_id", logementId.toString())
                    .putMetadata("logement_name", logementName)
                    .setDescription("Paiement réservation #" + reservationId + " - " + logementName)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Retourner le clientSecret au frontend
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());

            log.info("Payment intent created successfully: {}", paymentIntent.getId());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Stripe error while creating payment intent: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error while creating payment intent: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne du serveur"));
        }
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, Object> request) {
        try {
            // Implémenter si nécessaire pour les sessions de checkout Stripe
            log.info("Creating checkout session for request: {}", request);
            
            // Pour l'instant, retourner une réponse de base
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", "cs_test_placeholder");
            response.put("url", "https://checkout.stripe.com/pay/cs_test_placeholder");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error while creating checkout session: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne du serveur"));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<Map<String, String>> requestRefund(@RequestBody Map<String, Object> request) {
        try {
            String paymentIntentId = request.get("paymentIntentId").toString();
            Long amount = request.containsKey("amount") ? Long.valueOf(request.get("amount").toString()) : null;
            
            log.info("Processing refund for payment intent: {}, amount: {}", paymentIntentId, amount);
            
            // Implémenter la logique de remboursement ici
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("refundId", "re_placeholder");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error while processing refund: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur lors du remboursement"));
        }
    }
}
