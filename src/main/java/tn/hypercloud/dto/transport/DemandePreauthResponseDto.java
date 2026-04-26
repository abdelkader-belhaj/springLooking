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
public class DemandePreauthResponseDto {
    private Long demandeId;
    private boolean authorized;
    private BigDecimal holdAmount;
    private String authorizationRef;
    private String message;
}
