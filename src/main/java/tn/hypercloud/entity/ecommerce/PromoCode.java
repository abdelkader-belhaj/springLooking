package tn.hypercloud.entity.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Column(name = "is_active", nullable = false)
@Builder.Default
@Getter(AccessLevel.NONE)
@Setter(AccessLevel.NONE)
private Boolean isActive = true;

@JsonProperty("isActive")
public Boolean getIsActive() { return isActive; }

@JsonProperty("isActive")
public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    

    /** Number of times this code has been used */
    @Column(name = "times_used", nullable = false)
    @Builder.Default
    private int timesUsed = 0;

    /** Maximum number of times this code can be used (null = unlimited) */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
