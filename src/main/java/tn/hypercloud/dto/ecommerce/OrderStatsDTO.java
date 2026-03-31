package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatsDTO {
    private int totalOrders;           // Nombre total de commandes
    private BigDecimal totalRevenue;   // Revenu total
    private int pendingOrders;         // Commandes non livrées
    private int shippedOrders;         // Commandes expédiées
    private int deliveredOrders;       // Commandes livrées
    private int cancelledOrders;       // Commandes annulées
    private int pendingPaymentCount;   // Commandes non payées
}
