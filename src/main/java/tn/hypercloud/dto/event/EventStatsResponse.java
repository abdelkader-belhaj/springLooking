package tn.hypercloud.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStatsResponse {
    private long totalEvents;
    private long draftCount;
    private long publishedCount;
    private long rejectedCount;
    private long cancelledCount;
}
