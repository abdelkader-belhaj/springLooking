package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.OrderDTO;
import tn.hypercloud.dto.ecommerce.OrderStatsDTO;
import tn.hypercloud.entity.ecommerce.Order;
import tn.hypercloud.entity.ecommerce.PromoCode;
import tn.hypercloud.entity.ecommerce.Order.OrderStatus;
import tn.hypercloud.entity.ecommerce.Order.PaymentStatus;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.ecommerce.OrderRepository;
import tn.hypercloud.repository.ecommerce.PromoCodeRepository;
import tn.hypercloud.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PromoCodeRepository promocodeRepository;

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

    // ========== NOUVELLES MÉTHODES MÉTIER ==========

    /**
     * Récupère les commandes d'un utilisateur (SÉCURISÉ - voir ses propres commandes)
     */
    public List<OrderDTO> getUserOrders(Long userId) {
        // Vérifier que l'utilisateur authentifié demande ses propres commandes
        User currentUser = getCurrentAuthenticatedUser();
        
        // Si l'utilisateur n'est pas admin et cherche les commandes d'un autre, bloquer
        if (!userId.equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new RuntimeException("Unauthorized: Cannot view other user's orders");
        }
        
        return orderRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recheche une commande par numéro unique
     */
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with number: " + orderNumber));
        return mapToDTO(order);
    }

    /**
     * Récupère les commandes par statut (ADMIN)
     */
    public List<OrderDTO> getOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            return orderRepository.findByStatus(orderStatus)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }

    /**
     * Récupère les commandes en attente de paiement (ADMIN)
     */
    public List<OrderDTO> getPendingPaymentOrders() {
        return orderRepository.findByPaymentStatus(PaymentStatus.pending)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mettre à jour le statut d'une commande (ADMIN)
     */
    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        try {
            order.setStatus(OrderStatus.valueOf(newStatus));
            Order updated = orderRepository.save(order);
            return mapToDTO(updated);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + newStatus);
        }
    }

    /**
     * Mettre à jour le statut de paiement d'une commande (ADMIN)
     */
    public OrderDTO updatePaymentStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        try {
            order.setPaymentStatus(PaymentStatus.valueOf(newStatus));
            Order updated = orderRepository.save(order);
            return mapToDTO(updated);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid payment status: " + newStatus);
        }
    }

    /**
     * Annuler une commande (CLIENT/ADMIN)
     */
    public OrderDTO cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Vérifier que le client ne peut annuler que ses propres commandes
        User currentUser = getCurrentAuthenticatedUser();
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new RuntimeException("Unauthorized: Cannot cancel other user's orders");
        }
        
        // On peut seulement annuler si la commande n'est pas déjà livrée
        if (order.getStatus() == OrderStatus.delivered) {
            throw new RuntimeException("Cannot cancel a delivered order");
        }
        
        order.setStatus(OrderStatus.cancelled);
        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    /**
     * Générer un numéro de commande unique (ORD-YYYYMMDD-XXXXX)
     */
    public String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = now.format(formatter);
        
        // Obtenir le dernier numéro de commande du jour et incrémenter
        long count = orderRepository.count() + 1;
        String counterPart = String.format("%05d", count % 100000);
        
        return "ORD-" + datePart + "-" + counterPart;
    }

    /**
     * Valider une commande avant création (vérifier le stock, etc.)
     */
    public void validateOrder(OrderDTO orderDTO) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(orderDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + orderDTO.getUserId()));
        
        // Vérifier que les montants sont positifs
        if (orderDTO.getSubtotal() == null || orderDTO.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Subtotal must be greater than 0");
        }
        
        if (orderDTO.getTotalAmount() == null || orderDTO.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be greater than 0");
        }
        
        // Vérifier que la remise ne dépasse pas le subtotal
        if (orderDTO.getDiscountAmount() != null && 
            orderDTO.getDiscountAmount().compareTo(orderDTO.getSubtotal()) > 0) {
            throw new RuntimeException("Discount amount cannot exceed subtotal");
        }
        
        // Vérifier l'adresse de livraison
        if (orderDTO.getShippingAddress() == null || orderDTO.getShippingAddress().trim().isEmpty()) {
            throw new RuntimeException("Shipping address is required");
        }
    }

    /**
     * Récupère les statistiques des commandes (ADMIN)
     */
    public OrderStatsDTO getOrderStats() {
        List<Order> allOrders = orderRepository.findAll();
        
        int totalOrders = allOrders.size();
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int pendingOrders = (int) allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.pending)
                .count();
        
        int shippedOrders = (int) allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.shipped)
                .count();
        
        int deliveredOrders = (int) allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.delivered)
                .count();
        
        int cancelledOrders = (int) allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.cancelled)
                .count();
        
        int pendingPaymentCount = (int) allOrders.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.pending)
                .count();
        
        return OrderStatsDTO.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .pendingOrders(pendingOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .pendingPaymentCount(pendingPaymentCount)
                .build();
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Récupère l'utilisateur actuellement authentifié
     */
    private User getCurrentAuthenticatedUser() {
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
        // Cette logique dépend de votre système de rôles
        // À adapter selon votre User entity
        return user.getRole() != null && user.getRole().name().equals("ADMIN");
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
