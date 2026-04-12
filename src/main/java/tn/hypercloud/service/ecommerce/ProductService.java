package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.dto.ecommerce.AvailabilityDTO;
import tn.hypercloud.dto.ecommerce.ArtisanStatsDTO;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.entity.ecommerce.ProductCategory;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.ecommerce.ProductRepository;
import tn.hypercloud.repository.ecommerce.ProductCategoryRepository;
import tn.hypercloud.repository.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

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

    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        if (productDTO.getStockQuantity() >= 0) {
            product.setStockQuantity(productDTO.getStockQuantity());
        }
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setDiscountPrice(productDTO.getDiscountPrice());
        product.setImage(productDTO.getImage());
        
        Product updated = productRepository.save(product);
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
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les produits par catégorie (PUBLIC)
     */
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        return productRepository.findByCategory(category)
                .stream()
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
        product.setStockQuantity(newQuantity);
        Product updated = productRepository.save(product);
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

    // ========== STATISTIQUES ==========

    /**
     * Récupère les produits les plus vendus globalement (PUBLIC)
     */
    public List<ProductDTO> getBestSellers(int limit) {
        if (limit <= 0) {
            limit = 10;
        }
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findBestsellers(pageable)
                .stream()
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
        long totalSales = products.stream().mapToLong(Product::getSalesCount).sum();
        double totalRevenue = products.stream()
                .mapToDouble(p -> p.getPrice().doubleValue() * p.getSalesCount())
                .sum();
        double averagePrice = totalProducts > 0 
                ? products.stream().mapToDouble(p -> p.getPrice().doubleValue()).average().orElse(0) 
                : 0;
        int totalStock = products.stream().mapToInt(Product::getStockQuantity).sum();
        
        return ArtisanStatsDTO.builder()
                .artisanId(artisan.getId())
                .artisanName(artisan.getUsername() != null ? artisan.getUsername() : artisan.getEmail())
                .totalProducts(totalProducts)
                .totalSales(totalSales)
                .totalRevenue(totalRevenue)
                .averageProductPrice(averagePrice)
                .totalStockItems(totalStock)
                .build();
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
                .build();
    }
}
