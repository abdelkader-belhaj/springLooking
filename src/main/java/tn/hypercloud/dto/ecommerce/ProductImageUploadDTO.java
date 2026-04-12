package tn.hypercloud.dto.ecommerce;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImageUploadDTO {
    private Long productId;
    private String imageUrl;
    private String message;
    private boolean success;
}
