package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDetailStatsDTO {
    private Long productId;
    private String productName;
    private int totalItemsSold;        // Nombre d'articles vendus
    private BigDecimal totalRevenue;   // Revenu total
    private BigDecimal averageUnitPrice; // Prix moyen par article
    private int numberOfOrders;        // Nombre de commandes contenant ce produit
}
