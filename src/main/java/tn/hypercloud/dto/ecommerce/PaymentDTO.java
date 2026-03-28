package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private String stripePaymentIntentId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
