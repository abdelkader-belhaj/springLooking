package tn.hypercloud.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventReservationResponse {
    private Integer id;
    private LocalDateTime reservationDate;
    private int numberOfTickets;
    private BigDecimal totalPrice;
    private String status;
    private Integer eventId;
    private String eventTitle;
    private BigDecimal eventPrice;
    private Long userId;
    private String userName;
    private boolean qrUsed;
    private LocalDateTime qrUsedAt;
    private String qrUsedBy;
}