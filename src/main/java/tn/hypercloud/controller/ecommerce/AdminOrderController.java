package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.OrderDTO;
import tn.hypercloud.service.ecommerce.OrderService;
import tn.hypercloud.payload.response.ApiResponse;
import java.util.List;
import java.util.Map;

/**
 * Admin API endpoints for order management
 * Provides simplified API for marketplace UI
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;

    /**
     * Get all orders
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(
                ApiResponse.success("Orders retrieved", orders)
        );
    }

    /**
     * Get order by ID
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Order retrieved", order)
        );
    }

    /**
     * Update order status
     * PATCH /api/orders/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().build();
        }
        OrderDTO updated = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(
                ApiResponse.success("Order status updated", updated)
        );
    }
}
