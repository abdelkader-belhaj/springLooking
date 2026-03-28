package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductDTO {
    private Long id;
    private Long userId;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private int stockQuantity;
    private String image;
    private int salesCount;
}
