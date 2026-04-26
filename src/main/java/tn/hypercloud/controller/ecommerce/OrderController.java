package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.OrderDTO;
import tn.hypercloud.dto.ecommerce.OrderStatsDTO;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.service.ecommerce.OrderService;
import tn.hypercloud.service.ecommerce.PromocodeService;
import tn.hypercloud.service.ecommerce.InvoiceService;
import tn.hypercloud.service.ecommerce.OrderEmailService;
import tn.hypercloud.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;


import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ecommerce/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final PromocodeService promocodeService;
    private final OrderEmailService orderEmailService;

    // ========== ROUTES SPÉCIFIQUES DE RECHERCHE ==========

    /**
     * GET /orders/my-orders
     * Récupère les commandes de l'utilisateur authentifié (CLIENT)
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(orderService.getUserOrders(currentUser.getId()));
    }

    /**
     * GET /orders/number/{orderNumber}
     * Trouve une commande par son numéro unique (PUBLIC)
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDTO> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByOrderNumber(orderNumber));
    }

    /**
     * GET /orders/status/{status}
     * Récupère les commandes par statut (ADMIN)
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    /**
     * GET /orders/payment-pending
     * Récupère les commandes non payées (ADMIN)
     */
    @GetMapping("/payment-pending")
    public ResponseEntity<List<OrderDTO>> getPendingPaymentOrders() {
        return ResponseEntity.ok(orderService.getPendingPaymentOrders());
    }

    /**
     * GET /orders/stats
     * Récupère les statistiques des commandes (ADMIN)
     */
    @GetMapping("/stats")
    public ResponseEntity<OrderStatsDTO> getOrderStats() {
        return ResponseEntity.ok(orderService.getOrderStats());
    }

    // ========== ROUTES DE GESTION D'ÉTAT ==========

    /**
     * PATCH /orders/{id}/status
     * Mettre à jour le statut d'une commande (ADMIN)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, newStatus));
    }

    /**
     * PATCH /orders/{id}/payment-status
     * Mettre à jour le statut de paiement (ADMIN)
     */
    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<OrderDTO> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("paymentStatus");
        if (newStatus == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, newStatus));
    }

    /**
     * PATCH /orders/{id}/cancel
     * Annuler une commande (CLIENT/ADMIN)
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        return ResponseEntity.ok(orderService.cancelOrder(id, reason));
    }

    /**
     * GET /orders/{id}/invoice/pdf
     * Télécharger la facture en PDF (CLIENT/ADMIN)
     */
    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePDF(@PathVariable Long id) {
        try {
            OrderDTO order = orderService.getOrderById(id);

            // Vérifier que l'utilisateur peut accéder à cette facture
            User currentUser = getCurrentUser();
            if (!order.getUserId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            byte[] pdfBytes = invoiceService.generateInvoicePDF(order);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "facture-" + order.getOrderNumber() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== ROUTES CRUD DE BASE ==========

    @PostMapping
public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
    log.error("🚀 ORDER CONTROLLER CALLED");
    log.error("📧 clientEmail: {}", orderDTO.getClientEmail());

    User currentUser = getCurrentUser();
    orderDTO.setUserId(currentUser.getId());

    orderService.validateOrder(orderDTO);
    if (orderDTO.getOrderNumber() == null || orderDTO.getOrderNumber().isEmpty()) {
        orderDTO.setOrderNumber(orderService.generateOrderNumber());
    }

    OrderDTO created = orderService.createOrder(orderDTO);
    System.out.println("✅ Order saved: " + created.getId());

    if (orderDTO.getPromoCodeId() != null) {
        try {
            promocodeService.usePromoCode(orderDTO.getPromoCodeId());
            System.out.println("✅ Promo incremented");
        } catch (Exception e) {
            System.out.println("❌ Promo failed: " + e.getMessage());
        }
    }

    try {
        if (orderDTO.getClientEmail() != null) {
            created.setClientEmail(orderDTO.getClientEmail());
        }
        if (orderDTO.getClientName() != null) {
            created.setClientName(orderDTO.getClientName());
        }
        log.error("📨 Sending email to: {}", created.getClientEmail());
    orderEmailService.sendOrderConfirmationEmail(created);
    log.error("✅ Email sent!");
    } catch (Exception e) {
        System.out.println("❌ Email failed: " + e.getMessage());
        e.printStackTrace();
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable Long id, @RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.updateOrder(id, orderDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Récupère l'utilisateur actuellement authentifié
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Vérifier si l'utilisateur est admin
     */
    private boolean isAdmin(User user) {
        return user.getRole() != null && user.getRole().name().equals("ADMIN");
    }
}
