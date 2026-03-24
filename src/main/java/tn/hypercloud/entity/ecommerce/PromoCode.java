package tn.hypercloud.entity.ecommerce;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "promo_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** Pourcentage de réduction (ex: 20 pour 20%) */
    @Column(name = "discount_percentage", nullable = false)
    private int discountPercentage;

    /** Usage unique : mis à false après utilisation */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;
}
