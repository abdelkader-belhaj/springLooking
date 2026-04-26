package tn.hypercloud.dto.event;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventPaymentRequest {
    private BigDecimal amount;
    private String paymentMethod;
    private String currency;
    private String transactionId;
    private Integer reservationId;
    private String promoCode;
}