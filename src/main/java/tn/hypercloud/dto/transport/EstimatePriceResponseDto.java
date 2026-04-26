package tn.hypercloud.dto.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimatePriceResponseDto {
    private BigDecimal prixEstimeCalcule;
    private double distanceKm;
    private double dureeEstimeeMinutes;
}
