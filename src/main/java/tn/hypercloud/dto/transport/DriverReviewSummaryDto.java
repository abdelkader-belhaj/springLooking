package tn.hypercloud.dto.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverReviewSummaryDto {
    private boolean success;
    private Long chauffeurId;
    private String summary;
    private Integer nombreAvis;
    private Double averageNote;
    private List<String> highlights;
    private List<String> concerns;
    private String confidence;
    private String message;
}
