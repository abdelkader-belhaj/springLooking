package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCategoryDTO {
    private Long id;
    private Long parentId;
    private String name;
    private String description;
    private String image;
    private Integer displayOrder;
    private List<ProductCategoryDTO> children;
}
