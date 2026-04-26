package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.dto.ecommerce.AvailabilityDTO;
import tn.hypercloud.dto.ecommerce.ArtisanStatsDTO;
import tn.hypercloud.dto.ecommerce.ArtisanSaleDTO;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.entity.ecommerce.ProductCategory;
import tn.hypercloud.entity.ecommerce.OrderDetail;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.ecommerce.ProductRepository;
import tn.hypercloud.repository.ecommerce.ProductCategoryRepository;
import tn.hypercloud.repository.ecommerce.OrderDetailRepository;
import tn.hypercloud.repository.user.UserRepository;
import tn.hypercloud.service.ecommerce.ProductStatusEmailService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final StockNotificationService stockNotificationService;
    private final ProductStatusEmailService productStatusEmailService;

    public ProductDTO createProduct(ProductDTO productDTO) {
        User user = userRepository.findById(productDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + productDTO.getUserId()));
        ProductCategory category = productCategoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDTO.getCategoryId()));
        
        Product product = Product.builder()
                .user(user)
                .category(category)
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .discountPrice(productDTO.getDiscountPrice())
                .stockQuantity(productDTO.getStockQuantity())
                .image(productDTO.getImage())
                .build();
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToDTO(product);
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les produits actifs uniquement (CLIENT PUBLIC)
     */
    public List<ProductDTO> getActiveProducts() {
        return productRepository.findAll().stream()
                .filter(p -> "active".equals(p.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        int previousStock = product.getStockQuantity(); // ✅ save before update

        if (productDTO.getStockQuantity() >= 0) {
            product.setStockQuantity(productDTO.getStockQuantity());
        }
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setDiscountPrice(productDTO.getDiscountPrice());
        product.setImage(productDTO.getImage());
        
        Product updated = productRepository.save(product);

        // ✅ Notify if restocked from 0
        if (previousStock == 0 && productDTO.getStockQuantity() > 0) {
            try {
                stockNotificationService.notifyBackInStock(updated);
            } catch (Exception e) {
                log.error("Stock notification failed: {}", e.getMessage());
            }
        }

        return mapToDTO(updated);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Delete associated image file
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            fileUploadService.deleteProductImage(product.getImage());
        }
        
        productRepository.deleteById(id);
    }

    // ========== RECHERCHE & FILTRAGE ==========
    
    /**
     * Recherche textuelle par nom ou description (PUBLIC)
     */
    public List<ProductDTO> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPublicProducts();
        }
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query)
                .stream()
                .filter(p -> "active".equals(p.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les produits par catégorie (PUBLIC - uniquement les produits actifs)
     */
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        return productRepository.findByCategory(category)
                .stream()
                .filter(p -> "active".equals(p.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les produits publics (actifs uniquement)
     */
    private List<ProductDTO> getAllPublicProducts() {
        return productRepository.findAll().stream()
                .filter(p -> "active".equals(p.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les produits d'un artisan (PUBLIC)
     */
    public List<ProductDTO> getProductsByArtisan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Artisan not found with id: " + userId));
        return productRepository.findByUser(user)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ========== GESTION STOCK ==========

    /**
     * Mettre à jour le stock d'un produit (ARTISAN propriétaire)
     */
    public ProductDTO updateStock(Long id, int newQuantity) {
        if (newQuantity < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        int previousStock = product.getStockQuantity();
        product.setStockQuantity(newQuantity);
        Product updated = productRepository.save(product);

        // ✅ Notify if restocked from 0
        if (previousStock == 0 && newQuantity > 0) {
            try {
                stockNotificationService.notifyBackInStock(updated);
            } catch (Exception e) {
                log.error("Stock notification failed: {}", e.getMessage());
            }
        }

        return mapToDTO(updated);
    }

    /**
     * Vérifier la disponibilité d'une quantité (PUBLIC)
     */
    public AvailabilityDTO checkAvailability(Long id, int requestedQuantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        boolean isAvailable = product.getStockQuantity() >= requestedQuantity;
        String message = isAvailable 
                ? "Product is available" 
                : "Insufficient stock. Available: " + product.getStockQuantity() + ", Requested: " + requestedQuantity;
        
        return AvailabilityDTO.builder()
                .productId(id)
                .requestedQuantity(requestedQuantity)
                .availableQuantity(product.getStockQuantity())
                .available(isAvailable)
                .message(message)
                .build();
    }

    // ========== GESTION PROMOTION ==========

    /**
     * Activer une promotion sur un produit (ARTISAN propriétaire)
     */
    public ProductDTO activatePromotion(Long id, BigDecimal discountPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        if (discountPrice == null || discountPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Discount price must be greater than 0");
        }
        if (discountPrice.compareTo(product.getPrice()) >= 0) {
            throw new RuntimeException("Discount price must be less than original price");
        }
        
        product.setDiscountPrice(discountPrice);
        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    /**
     * Désactiver une promotion sur un produit (ARTISAN propriétaire)
     */
    public ProductDTO deactivatePromotion(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setDiscountPrice(null);
        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    /**
     * Mettre à jour le statut d'un produit (ADMIN uniquement)
     * Envoie une notification email à l'artisan si le produit est désactivé
     */
    public ProductDTO updateProductStatus(Long id, String newStatus) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        String oldStatus = product.getStatus();
        product.setStatus(newStatus);
        Product updated = productRepository.save(product);
        
        try {
            if ("inactive".equals(newStatus) && !"inactive".equals(oldStatus)) {
                productStatusEmailService.sendProductDisabledEmail(updated);
            } else if ("active".equals(newStatus) && !"active".equals(oldStatus)) {
                productStatusEmailService.sendProductEnabledEmail(updated);
            }
        } catch (Exception e) {
            log.error("Failed to send product status email: {}", e.getMessage());
        }
        
        return mapToDTO(updated);
    }

    // ========== STATISTIQUES ==========

    /**
     * Récupère les produits les plus vendus globalement (PUBLIC - uniquement actifs)
     */
    public List<ProductDTO> getBestSellers(int limit) {
        if (limit <= 0) {
            limit = 10;
        }
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findBestsellers(pageable)
                .stream()
                .filter(p -> "active".equals(p.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les statistiques d'un artisan (ARTISAN propriétaire)
     */
    public ArtisanStatsDTO getArtisanStats(Long artisanId) {
        User artisan = userRepository.findById(artisanId)
                .orElseThrow(() -> new RuntimeException("Artisan not found with id: " + artisanId));
        
        List<Product> products = productRepository.findByUser(artisan);
        
        int totalProducts = products.size();
        List<OrderDetail> allDetails = orderDetailRepository.findByProductUserId(artisanId);
        
        double totalRevenue = allDetails.stream()
                .mapToDouble(d -> d.getUnitPrice().doubleValue() * d.getQuantity())
                .sum();
        long productsSold = allDetails.stream().mapToLong(OrderDetail::getQuantity).sum();
        double commissionEarned = totalRevenue * 0.10; // 10% commission par défaut
        
        double averagePrice = totalProducts > 0 
                ? products.stream().mapToDouble(p -> p.getPrice().doubleValue()).average().orElse(0) 
                : 0;
        int totalStock = products.stream().mapToInt(Product::getStockQuantity).sum();
        
        // Stats du mois en cours
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        List<OrderDetail> monthlyDetails = allDetails.stream()
                .filter(d -> d.getOrder().getCreatedAt().isAfter(startOfMonth))
                .collect(Collectors.toList());
        
        double thisMonthRevenue = monthlyDetails.stream()
                .mapToDouble(d -> d.getUnitPrice().doubleValue() * d.getQuantity())
                .sum();
        long thisMonthSales = monthlyDetails.size();
        
        return ArtisanStatsDTO.builder()
                .artisanId(artisan.getId())
                .artisanName(artisan.getUsername() != null ? artisan.getUsername() : artisan.getEmail())
                .totalProducts(totalProducts)
                .totalSales(allDetails.size())
                .totalRevenue(totalRevenue)
                .averageProductPrice(averagePrice)
                .totalStockItems(totalStock)
                .thisMonthRevenue(thisMonthRevenue)
                .thisMonthSales(thisMonthSales)
                .productsSold(productsSold)
                .commissionEarned(commissionEarned)
                .netRevenue(totalRevenue - commissionEarned)
                .build();
    }

    /**
     * Récupère la liste détaillée des ventes pour un artisan
     */
    public List<ArtisanSaleDTO> getArtisanSales(Long artisanId) {
        List<OrderDetail> orderDetails = orderDetailRepository.findByProductUserId(artisanId);
        
        return orderDetails.stream().map(detail -> {
            String status = "pending";
            if (detail.getOrder().getStatus() != null) {
                switch (detail.getOrder().getStatus()) {
                    case paid:
                    case shipped:
                    case delivered:
                        status = "completed";
                        break;
                    case cancelled:
                        status = "cancelled";
                        break;
                    default:
                        status = "pending";
                }
            }
            
            return ArtisanSaleDTO.builder()
                    .id(detail.getId())
                    .productName(detail.getProductName())
                    .quantity(detail.getQuantity())
                    .unitPrice(detail.getUnitPrice())
                    .totalAmount(detail.getSubtotal())
                    .buyerName(detail.getOrder().getUser().getUsername() != null ? 
                              detail.getOrder().getUser().getUsername() : 
                              detail.getOrder().getUser().getEmail())
                    .orderId(detail.getOrder().getId())
                    .saleDate(detail.getOrder().getCreatedAt())
                    .status(status)
                    .build();
        }).collect(Collectors.toList());
    }

    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .userId(product.getUser().getId())
                .categoryId(product.getCategory().getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .stockQuantity(product.getStockQuantity())
                .image(product.getImage())
                .salesCount(product.getSalesCount())
                .status(product.getStatus())
                .build();
    }
}
