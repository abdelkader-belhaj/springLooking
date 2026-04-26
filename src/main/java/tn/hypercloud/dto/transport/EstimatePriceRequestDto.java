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
public class EstimatePriceRequestDto {
    private BigDecimal departLatitude;
    private BigDecimal departLongitude;
    private BigDecimal arriveeLatitude;
    private BigDecimal arriveeLongitude;
    private String typeVehiculeDemande;
}
