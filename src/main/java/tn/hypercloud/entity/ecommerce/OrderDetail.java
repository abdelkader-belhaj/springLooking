package tn.hypercloud.entity.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Snapshot du nom au moment de l'achat */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    /** Prix unitaire snapshot au moment de l'achat */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** quantity × unit_price */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
