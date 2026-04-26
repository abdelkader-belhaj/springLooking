package tn.hypercloud.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoAccessResponse {
    private boolean success;
    private String message;
    private Double distanceMeters;
    private String unlockCode;  // Seulement si success == true
}
