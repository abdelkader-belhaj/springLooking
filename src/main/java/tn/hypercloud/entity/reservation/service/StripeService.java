package tn.hypercloud.entity.reservation.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StripeService {

    // ============================================================
    //  CRÉER ET CONFIRMER UN PAIEMENT STRIPE
    //  Retourne le PaymentIntent confirmé
    // ============================================================
    public PaymentIntent creerEtConfirmerPaiement(BigDecimal montant,
                                                  String paymentMethodId)
            throws StripeException {

        long montantCentimes = montant
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montantCentimes)
                .setCurrency("eur") // remplace par "tnd" si supporté par ton compte Stripe
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true)
                .setReturnUrl("http://localhost:4200/paiement-confirmation")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(
                                        PaymentIntentCreateParams.AutomaticPaymentMethods
                                                .AllowRedirects.NEVER
                                )
                                .build()
                )
                .build();

        return PaymentIntent.create(params);
    }

    // ============================================================
    //  REMBOURSER UN PAIEMENT STRIPE
    //  paymentIntentId = referenceTx stockée dans PaiementVol
    //  montant = null => remboursement total
    // ============================================================
    public Refund rembourserPaiement(String paymentIntentId, BigDecimal montant)
            throws StripeException {

        RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId);

        if (montant != null) {
            long montantCentimes = montant
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();
            builder.setAmount(montantCentimes);
        }

        return Refund.create(builder.build());
    }
}