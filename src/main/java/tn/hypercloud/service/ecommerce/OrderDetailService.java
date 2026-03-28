package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.OrderDetailDTO;
import tn.hypercloud.entity.ecommerce.Order;
import tn.hypercloud.entity.ecommerce.OrderDetail;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.repository.ecommerce.OrderDetailRepository;
import tn.hypercloud.repository.ecommerce.OrderRepository;
import tn.hypercloud.repository.ecommerce.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderDetailDTO createOrderDetail(OrderDetailDTO orderDetailDTO) {
        Order order = orderRepository.findById(orderDetailDTO.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderDetailDTO.getOrderId()));
        Product product = productRepository.findById(orderDetailDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + orderDetailDTO.getProductId()));
        
        OrderDetail orderDetail = OrderDetail.builder()
                .order(order)
                .product(product)
                .productName(orderDetailDTO.getProductName())
                .quantity(orderDetailDTO.getQuantity())
                .unitPrice(orderDetailDTO.getUnitPrice())
                .subtotal(orderDetailDTO.getSubtotal())
                .build();
        OrderDetail saved = orderDetailRepository.save(orderDetail);
        return mapToDTO(saved);
    }

    public OrderDetailDTO getOrderDetailById(Long id) {
        OrderDetail orderDetail = orderDetailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OrderDetail not found with id: " + id));
        return mapToDTO(orderDetail);
    }

    public List<OrderDetailDTO> getAllOrderDetails() {
        return orderDetailRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public OrderDetailDTO updateOrderDetail(Long id, OrderDetailDTO orderDetailDTO) {
        OrderDetail orderDetail = orderDetailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OrderDetail not found with id: " + id));
        
        orderDetail.setQuantity(orderDetailDTO.getQuantity());
        orderDetail.setUnitPrice(orderDetailDTO.getUnitPrice());
        orderDetail.setSubtotal(orderDetailDTO.getSubtotal());
        
        OrderDetail updated = orderDetailRepository.save(orderDetail);
        return mapToDTO(updated);
    }

    public void deleteOrderDetail(Long id) {
        if (!orderDetailRepository.existsById(id)) {
            throw new RuntimeException("OrderDetail not found with id: " + id);
        }
        orderDetailRepository.deleteById(id);
    }

    private OrderDetailDTO mapToDTO(OrderDetail orderDetail) {
        return OrderDetailDTO.builder()
                .id(orderDetail.getId())
                .orderId(orderDetail.getOrder().getId())
                .productId(orderDetail.getProduct().getId())
                .productName(orderDetail.getProductName())
                .quantity(orderDetail.getQuantity())
                .unitPrice(orderDetail.getUnitPrice())
                .subtotal(orderDetail.getSubtotal())
                .build();
    }
}
