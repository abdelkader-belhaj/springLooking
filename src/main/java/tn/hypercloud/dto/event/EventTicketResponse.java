package tn.hypercloud.dto.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTicketResponse {

    private Integer id;
    private String ticketCode;
    private Integer ticketNumber;
    private String status;
    private boolean used;
    private LocalDateTime usedAt;
    private String usedBy;
    private Integer reservationId;
    private Integer eventId;
    private String eventTitle;
    private Long ownerUserId;
    private String ownerName;
}
