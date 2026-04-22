package tn.hypercloud.dto.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCancelRequest {
    private String reason;
}
