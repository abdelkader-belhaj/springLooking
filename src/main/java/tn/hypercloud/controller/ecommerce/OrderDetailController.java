package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.OrderDetailDTO;
import tn.hypercloud.service.ecommerce.OrderDetailService;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/order-details")
@RequiredArgsConstructor
public class OrderDetailController {
    private final OrderDetailService orderDetailService;

    @PostMapping
    public ResponseEntity<OrderDetailDTO> createOrderDetail(@RequestBody OrderDetailDTO orderDetailDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDetailService.createOrderDetail(orderDetailDTO));
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
}
