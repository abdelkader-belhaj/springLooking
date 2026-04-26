package tn.hypercloud.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventPaymentResponse {
    private Integer id;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private LocalDateTime paymentDate;
    private String currency;
    private LocalDateTime createdAt;
    private Integer reservationId;
    private String eventTitle;
    private String userName;
}