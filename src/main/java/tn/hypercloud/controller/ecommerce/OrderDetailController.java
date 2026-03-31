package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.OrderDetailDTO;
import tn.hypercloud.dto.ecommerce.OrderDetailStatsDTO;
import tn.hypercloud.entity.ecommerce.OrderDetail;
import tn.hypercloud.service.ecommerce.OrderDetailService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ecommerce/order-details")
@RequiredArgsConstructor
public class OrderDetailController {
    private final OrderDetailService orderDetailService;

    // ========== ROUTES SPÉCIFIQUES DE RECHERCHE ==========

    /**
     * GET /order-details/order/{orderId}
     * Récupère tous les détails d'une commande
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderDetailDTO>> getOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderDetailService.getOrderDetails(orderId));
    }

    /**
     * GET /order-details/product/{productId}
     * Récupère tous les détails d'un produit (qui l'a acheté)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<OrderDetailDTO>> getProductDetails(@PathVariable Long productId) {
        List<OrderDetail> details = orderDetailService.getOrderDetailRepository().findByProductId(productId);
        List<OrderDetailDTO> dtos = details.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /order-details/order/{orderId}/total
     * Calculer le total d'une commande
     */
    @GetMapping("/order/{orderId}/total")
    public ResponseEntity<Map<String, BigDecimal>> getOrderTotal(@PathVariable Long orderId) {
        BigDecimal total = orderDetailService.calculateOrderTotal(orderId);
        return ResponseEntity.ok(Map.of("total", total));
    }

    /**
     * GET /order-details/bestsellers?limit=10
     * Récupère les produits les plus vendus (TOP SELLERS)
     */
    @GetMapping("/bestsellers")
    public ResponseEntity<List<OrderDetailStatsDTO>> getTopSellingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(orderDetailService.getTopSellingProducts(limit));
    }

    // ========== ROUTES DE GESTION ==========

    /**
     * PATCH /order-details/{id}/quantity
     * Modifier la quantité d'un article dans une commande
     */
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<OrderDetailDTO> updateQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        Integer newQuantity = payload.get("quantity");
        if (newQuantity == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orderDetailService.updateQuantity(id, newQuantity));
    }

    // ========== ROUTES CRUD DE BASE ==========

    @PostMapping
    public ResponseEntity<OrderDetailDTO> createOrderDetail(@RequestBody OrderDetailDTO orderDetailDTO) {
        orderDetailService.validateOrderDetail(orderDetailDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderDetailService.createOrderDetail(orderDetailDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> getOrderDetailById(@PathVariable Long id) {
        return ResponseEntity.ok(orderDetailService.getOrderDetailById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderDetailDTO>> getAllOrderDetails() {
        return ResponseEntity.ok(orderDetailService.getAllOrderDetails());
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> updateOrderDetail(@PathVariable Long id, @RequestBody OrderDetailDTO orderDetailDTO) {
        return ResponseEntity.ok(orderDetailService.updateOrderDetail(id, orderDetailDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderDetail(@PathVariable Long id) {
        orderDetailService.deleteOrderDetail(id);
        return ResponseEntity.noContent().build();
    }

    // ========== MÉTHODE UTILITAIRE ==========
    
    private OrderDetailDTO mapToDTO(OrderDetail orderDetail) {
        BigDecimal discount = BigDecimal.ZERO;
        if (orderDetail.getProduct().getDiscountPrice() != null) {
            discount = orderDetail.getProduct().getPrice()
                    .subtract(orderDetail.getProduct().getDiscountPrice())
                    .multiply(BigDecimal.valueOf(orderDetail.getQuantity()));
        }
        
        BigDecimal totalWithDiscount = orderDetail.getSubtotal().subtract(discount);
        
        return OrderDetailDTO.builder()
                .id(orderDetail.getId())
                .orderId(orderDetail.getOrder().getId())
                .productId(orderDetail.getProduct().getId())
                .productName(orderDetail.getProductName())
                .productImage(orderDetail.getProduct().getImage())
                .productCategory(orderDetail.getProduct().getCategory().getName())
                .quantity(orderDetail.getQuantity())
                .unitPrice(orderDetail.getUnitPrice())
                .subtotal(orderDetail.getSubtotal())
                .discount(discount)
                .totalWithDiscount(totalWithDiscount)
                .build();
    }
}
