package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DealFavoriteDTO {
    private Long id;
    private Long userId;
    private Long dealId;
    private LocalDateTime addedAt;
}
