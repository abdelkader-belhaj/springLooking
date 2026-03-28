package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.OrderDTO;
import tn.hypercloud.entity.ecommerce.Order;
import tn.hypercloud.entity.ecommerce.PromoCode;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.ecommerce.OrderRepository;
import tn.hypercloud.repository.ecommerce.PromocodeRepository;
import tn.hypercloud.repository.user.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PromocodeRepository promocodeRepository;

    public OrderDTO createOrder(OrderDTO orderDTO) {
        User user = userRepository.findById(orderDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + orderDTO.getUserId()));
        
        PromoCode promoCode = null;
        if (orderDTO.getPromoCodeId() != null) {
            promoCode = promocodeRepository.findById(orderDTO.getPromoCodeId())
                    .orElseThrow(() -> new RuntimeException("PromoCode not found with id: " + orderDTO.getPromoCodeId()));
        }
        
        Order order = Order.builder()
                .orderNumber(orderDTO.getOrderNumber())
                .user(user)
                .status(Order.OrderStatus.valueOf(orderDTO.getStatus()))
                .paymentStatus(Order.PaymentStatus.valueOf(orderDTO.getPaymentStatus()))
                .paymentMethod(orderDTO.getPaymentMethod())
                .subtotal(orderDTO.getSubtotal())
                .discountAmount(orderDTO.getDiscountAmount())
                .promoCode(promoCode)
                .totalAmount(orderDTO.getTotalAmount())
                .shippingAddress(orderDTO.getShippingAddress())
                .build();
        Order saved = orderRepository.save(order);
        return mapToDTO(saved);
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return mapToDTO(order);
    }

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO updateOrder(Long id, OrderDTO orderDTO) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        order.setStatus(Order.OrderStatus.valueOf(orderDTO.getStatus()));
        order.setPaymentStatus(Order.PaymentStatus.valueOf(orderDTO.getPaymentStatus()));
        order.setPaymentMethod(orderDTO.getPaymentMethod());
        order.setShippingAddress(orderDTO.getShippingAddress());
        
        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    private OrderDTO mapToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .status(order.getStatus().toString())
                .paymentStatus(order.getPaymentStatus().toString())
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .promoCodeId(order.getPromoCode() != null ? order.getPromoCode().getId() : null)
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .build();
    }
}
