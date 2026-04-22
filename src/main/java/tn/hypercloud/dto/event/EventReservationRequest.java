package tn.hypercloud.dto.event;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventReservationRequest {
    private int numberOfTickets;
    private Integer eventId;
    // ❌ userId supprimé → depuis token JWT
}