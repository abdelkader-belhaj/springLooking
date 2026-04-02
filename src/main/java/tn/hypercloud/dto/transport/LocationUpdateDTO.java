package tn.hypercloud.dto.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateDTO {
    private Long courseId;           // ID de la course en cours
    private Long chauffeurId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    // Optionnel : timestamp, vitesse, etc.
}