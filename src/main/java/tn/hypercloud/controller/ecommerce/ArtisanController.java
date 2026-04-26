package tn.hypercloud.controller.ecommerce;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.dto.ecommerce.ArtisanStatsDTO;
import tn.hypercloud.dto.ecommerce.ArtisanSaleDTO;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.service.ecommerce.ProductService;
import tn.hypercloud.repository.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ecommerce/artisan")
public class ArtisanController {
    private final ProductService productService;
    private final UserRepository userRepository;

    public ArtisanController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/ecommerce/artisan/profile
     * Récupère le profil de l'artisan actuellement authentifié
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getArtisanProfile() {
        User currentUser = getCurrentUser();
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", currentUser.getId());
        profile.put("username", currentUser.getUsername());
        profile.put("email", currentUser.getEmail());
        profile.put("bio", currentUser.getBio());
        profile.put("phone", currentUser.getPhone());
        profile.put("profileImage", currentUser.getProfileImage());
        profile.put("role", currentUser.getRole());
        profile.put("createdAt", currentUser.getCreatedAt());
        
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/ecommerce/artisan/products
     * Récupère tous les produits de l'artisan actuellement authentifié
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getArtisanProducts() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(productService.getProductsByArtisan(currentUser.getId()));
    }

    /**
     * GET /api/ecommerce/artisan/products/{productId}
     * Récupère un produit spécifique de l'artisan (avec vérification de propriété)
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductDTO> getArtisanProduct(@PathVariable Long productId) {
        User currentUser = getCurrentUser();
        ProductDTO product = productService.getProductById(productId);
        
        // Vérifier que l'artisan est propriétaire du produit
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(product);
    }

    /**
     * POST /api/ecommerce/artisan/products
     * Crée un nouveau produit pour l'artisan actuellement authentifié
     */
    @PostMapping("/products")
    public ResponseEntity<ProductDTO> createArtisanProduct(@RequestBody ProductDTO productDTO) {
        User currentUser = getCurrentUser();
        
        // Forcer l'ID utilisateur au profil actuel
        productDTO.setUserId(currentUser.getId());
        
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    /**
     * PUT /api/ecommerce/artisan/products/{productId}
     * Met à jour un produit de l'artisan (avec vérification de propriété)
     */
    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductDTO> updateArtisanProduct(
            @PathVariable Long productId,
            @RequestBody ProductDTO productDTO) {
        User currentUser = getCurrentUser();
        
        // Vérifier que l'artisan est propriétaire du produit
        ProductDTO existingProduct = productService.getProductById(productId);
        if (!existingProduct.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Forcer l'ID utilisateur au profil actuel (sécurité)
        productDTO.setUserId(currentUser.getId());
        
        ProductDTO updatedProduct = productService.updateProduct(productId, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * DELETE /api/ecommerce/artisan/products/{productId}
     * Supprime un produit de l'artisan (avec vérification de propriété)
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteArtisanProduct(@PathVariable Long productId) {
        User currentUser = getCurrentUser();
        
        // Vérifier que l'artisan est propriétaire du produit
        ProductDTO product = productService.getProductById(productId);
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/ecommerce/artisan/products/{productId}/stock
     * Met à jour le stock d'un produit de l'artisan
     */
    @PatchMapping("/products/{productId}/stock")
    public ResponseEntity<ProductDTO> updateArtisanProductStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> payload) {
        User currentUser = getCurrentUser();
        
        // Vérifier que l'artisan est propriétaire du produit
        ProductDTO product = productService.getProductById(productId);
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Integer newQuantity = payload.get("stockQuantity");
        if (newQuantity == null) {
            return ResponseEntity.badRequest().build();
        }
        
        ProductDTO updatedProduct = productService.updateStock(productId, newQuantity);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * PATCH /api/ecommerce/artisan/products/{productId}/promotion
     * Active une promotion sur un produit de l'artisan
     */
    @PatchMapping("/products/{productId}/promotion")
    public ResponseEntity<ProductDTO> activateProductPromotion(
            @PathVariable Long productId,
            @RequestBody Map<String, BigDecimal> payload) {
        User currentUser = getCurrentUser();
        
        // Vérifier que l'artisan est propriétaire du produit
        ProductDTO product = productService.getProductById(productId);
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        BigDecimal discountPrice = payload.get("discountPrice");
        if (discountPrice == null) {
            return ResponseEntity.badRequest().build();
        }
        
        ProductDTO updatedProduct = productService.activatePromotion(productId, discountPrice);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * DELETE /api/ecommerce/artisan/products/{productId}/promotion
     * Désactive la promotion d'un produit de l'artisan
     */
    @DeleteMapping("/products/{productId}/promotion")
    public ResponseEntity<ProductDTO> deactivateProductPromotion(@PathVariable Long productId) {
        User currentUser = getCurrentUser();
        
        // Vérifier que l'artisan est propriétaire du produit
        ProductDTO product = productService.getProductById(productId);
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        ProductDTO updatedProduct = productService.deactivatePromotion(productId);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * GET /api/ecommerce/artisan/sales/stats
     * Récupère les statistiques de ventes pour l'artisan actuellement authentifié
     */
    @GetMapping("/sales/stats")
    public ResponseEntity<ArtisanStatsDTO> getArtisanSalesStats() {
        User currentUser = getCurrentUser();
        ArtisanStatsDTO stats = productService.getArtisanStats(currentUser.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/ecommerce/artisan/sales
     * Récupère la liste détaillée des ventes pour l'artisan actuellement authentifié
     */
    @GetMapping("/sales")
    public ResponseEntity<List<ArtisanSaleDTO>> getArtisanSales() {
        User currentUser = getCurrentUser();
        List<ArtisanSaleDTO> sales = productService.getArtisanSales(currentUser.getId());
        return ResponseEntity.ok(sales);
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
}
