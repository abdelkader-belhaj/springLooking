package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDetailDTO {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImage;      // Image du produit (snapshot)
    private String productCategory;   // Catégorie du produit
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private BigDecimal discount;      // Remise éventuellement appliquée
    private BigDecimal totalWithDiscount; // Subtotal après remise
}
