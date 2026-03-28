package tn.hypercloud.dto.ecommerce;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromocodeDTO {
    private Long id;
    private String code;
    private int discountPercentage;
    private boolean isActive;
}
