package tn.hypercloud.dto.ecommerce;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilityDTO {
    private Long productId;
    private int requestedQuantity;
    private int availableQuantity;
    private boolean available;
    private String message;
}
