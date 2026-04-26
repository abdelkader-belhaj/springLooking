package tn.hypercloud.dto.ecommerce;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String clientName;
    private String clientEmail;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private Long promoCodeId;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderDetailDTO> orderDetails;
    private LocalDateTime createdAt;
}
