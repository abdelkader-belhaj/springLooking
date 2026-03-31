package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.OrderDetailDTO;
import tn.hypercloud.dto.ecommerce.OrderDetailStatsDTO;
import tn.hypercloud.entity.ecommerce.Order;
import tn.hypercloud.entity.ecommerce.OrderDetail;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.repository.ecommerce.OrderDetailRepository;
import tn.hypercloud.repository.ecommerce.OrderRepository;
import tn.hypercloud.repository.ecommerce.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // Getter pour accéder au repository depuis le controller
    public OrderDetailRepository getOrderDetailRepository() {
        return orderDetailRepository;
    }

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

    // ========== NOUVELLES MÉTHODES MÉTIER ==========

    /**
     * Récupère tous les détails d'une commande
     */
    public List<OrderDetailDTO> getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        return orderDetailRepository.findByOrderId(orderId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Ajouter un produit à une commande
     */
    public OrderDetailDTO addProductToOrder(Long orderId, Long productId, int quantity) {
        // Valider que la commande existe
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Valider que le produit existe
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        // Vérifier que la quantité est valide
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }
        
        // Vérifier qu'il n'existe pas déjà dans la commande
        if (orderDetailRepository.existsByOrderIdAndProductId(orderId, productId)) {
            throw new RuntimeException("Product already exists in this order. Use updateQuantity instead.");
        }
        
        // Créer le détail
        BigDecimal unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        OrderDetail orderDetail = OrderDetail.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .build();
        
        OrderDetail saved = orderDetailRepository.save(orderDetail);
        return mapToDTO(saved);
    }

    /**
     * Retirer un produit de la commande
     */
    public void removeProductFromOrder(Long orderId, Long productId) {
        // Vérifier que la relation existe
        if (!orderDetailRepository.existsByOrderIdAndProductId(orderId, productId)) {
            throw new RuntimeException("Product not found in this order");
        }
        
        // Récupérer et supprimer
        List<OrderDetail> details = orderDetailRepository.findByOrderId(orderId);
        OrderDetail toDelete = details.stream()
                .filter(d -> d.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OrderDetail not found"));
        
        orderDetailRepository.delete(toDelete);
    }

    /**
     * Mettre à jour la quantité d'un produit
     */
    public OrderDetailDTO updateQuantity(Long orderDetailId, int newQuantity) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                .orElseThrow(() -> new RuntimeException("OrderDetail not found with id: " + orderDetailId));
        
        if (newQuantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }
        
        // Recalculer le subtotal
        BigDecimal subtotal = orderDetail.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity));
        
        orderDetail.setQuantity(newQuantity);
        orderDetail.setSubtotal(subtotal);
        
        OrderDetail updated = orderDetailRepository.save(orderDetail);
        return mapToDTO(updated);
    }

    /**
     * Calculer le total d'une commande (sum subtotals)
     */
    public BigDecimal calculateOrderTotal(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        BigDecimal total = orderDetailRepository.findByOrderId(orderId)
                .stream()
                .map(OrderDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total;
    }

    /**
     * Obtenir les meilleurs produits vendus (TOP SELLERS)
     */
    public List<OrderDetailStatsDTO> getTopSellingProducts(int limit) {
        if (limit <= 0) {
            limit = 10; // Default
        }
        
        // Récupérer tous les détails et grouper par produit
        List<OrderDetail> allDetails = orderDetailRepository.findAll();
        
        Map<Long, List<OrderDetail>> groupedByProduct = allDetails.stream()
                .collect(Collectors.groupingBy(od -> od.getProduct().getId()));
        
        // Convertir en stats et trier
        List<OrderDetailStatsDTO> stats = groupedByProduct.entrySet().stream()
                .map(entry -> {
                    List<OrderDetail> details = entry.getValue();
                    OrderDetail first = details.get(0);
                    
                    int totalSold = details.stream().mapToInt(OrderDetail::getQuantity).sum();
                    BigDecimal totalRevenue = details.stream()
                            .map(OrderDetail::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avgPrice = totalRevenue.divide(BigDecimal.valueOf(totalSold), 2, BigDecimal.ROUND_HALF_UP);
                    
                    long orderCount = details.stream().map(d -> d.getOrder().getId()).distinct().count();
                    
                    return OrderDetailStatsDTO.builder()
                            .productId(first.getProduct().getId())
                            .productName(first.getProductName())
                            .totalItemsSold(totalSold)
                            .totalRevenue(totalRevenue)
                            .averageUnitPrice(avgPrice)
                            .numberOfOrders((int) orderCount)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getTotalItemsSold(), a.getTotalItemsSold()))
                .limit(limit)
                .collect(Collectors.toList());
        
        return stats;
    }

    /**
     * Valider les détails avant ajout
     */
    public void validateOrderDetail(OrderDetailDTO dto) {
        if (dto.getOrderId() == null) {
            throw new RuntimeException("Order ID is required");
        }
        
        if (dto.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        if (dto.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }
        
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Unit price must be greater than 0");
        }
        
        if (dto.getSubtotal() == null || dto.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Subtotal must be greater than 0");
        }
    }

    private OrderDetailDTO mapToDTO(OrderDetail orderDetail) {
        // Calculer la remise si elle existe
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
