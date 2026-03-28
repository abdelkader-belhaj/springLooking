package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDetailDTO {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
