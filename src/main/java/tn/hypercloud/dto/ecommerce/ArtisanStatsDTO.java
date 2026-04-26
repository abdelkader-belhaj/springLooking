package tn.hypercloud.dto.ecommerce;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArtisanStatsDTO {
    private Long artisanId;
    private String artisanName;
    private int totalProducts;
    private long totalSales;
    private double totalRevenue;
    private double averageProductPrice;
    private int totalStockItems;
    private double thisMonthRevenue;
    private long thisMonthSales;
    private long productsSold;
    private double commissionEarned;
    private double netRevenue;
}
