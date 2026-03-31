package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderFilterDTO {
    private String status;              // pending, paid, shipped, delivered, cancelled
    private String paymentStatus;       // pending, paid, failed
    private Long userId;                // Pour filtrer par utilisateur (ADMIN)
    private LocalDateTime dateFrom;     // Filtre par date de début
    private LocalDateTime dateTo;       // Filtre par date de fin
}
