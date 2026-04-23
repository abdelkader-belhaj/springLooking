package tn.hypercloud.entity.reservation.dto;

import lombok.Data;

@Data
public class StripePaymentRequest {
    private Integer reservationId;
    private String methode;           // "carte"
    private String paymentMethodId;   // pm_xxxx envoyé depuis le front Angular
}