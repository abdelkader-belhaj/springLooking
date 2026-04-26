package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.dto.ecommerce.AvailabilityDTO;
import tn.hypercloud.dto.ecommerce.ArtisanStatsDTO;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.service.ecommerce.ProductService;
import tn.hypercloud.repository.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecommerce/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final UserRepository userRepository;

    // ========== RECHERCHE & FILTRAGE - ROUTES SPÉCIFIQUES (PUBLIC) ==========

    /**
     * GET /products/search?q={query}
     * Recherche textuelle par nom ou description
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }

    /**
     * GET /products/bestsellers?limit=10
     * Récupère les produits les plus vendus (PUBLIC)
     */
    @GetMapping("/bestsellers")
    public ResponseEntity<List<ProductDTO>> getBestSellers(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getBestSellers(limit));
    }

    /**
     * GET /products/category/{categoryId}
     * Récupère les produits d'une catégorie spécifique
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    /**
     * GET /products/artisan/{userId}/stats
     * Récupère les statistiques de ventes d'un artisan (ARTISAN propriétaire uniquement)
     */
    @GetMapping("/artisan/{userId}/stats")
    public ResponseEntity<ArtisanStatsDTO> getArtisanStats(@PathVariable Long userId) {
        // Vérifier que l'utilisateur courant voit ses propres stats
        User currentUser = getCurrentUser();
        
        if (!userId.equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(productService.getArtisanStats(userId));
    }

    /**
     * GET /products/artisan/{userId}
     * Récupère les produits d'un artisan spécifique
     */
    @GetMapping("/artisan/{userId}")
    public ResponseEntity<List<ProductDTO>> getProductsByArtisan(@PathVariable Long userId) {
        return ResponseEntity.ok(productService.getProductsByArtisan(userId));
    }

    // ========== GESTION STOCK - ROUTES AVEC ID SPÉCIFIQUES ==========

    /**
     * PATCH /products/{id}/stock
     * Mettre à jour le stock d'un produit (ARTISAN propriétaire uniquement)
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDTO> updateStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        
        // Vérifier que l'utilisateur courant est le propriétaire du produit
        User currentUser = getCurrentUser();
        ProductDTO product = productService.getProductById(id);
        
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Integer newQuantity = payload.get("stockQuantity");
        if (newQuantity == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(productService.updateStock(id, newQuantity));
    }

    /**
     * GET /products/{id}/available?quantity=X
     * Vérifier la disponibilité d'une quantité en stock (PUBLIC)
     */
    @GetMapping("/{id}/available")
    public ResponseEntity<AvailabilityDTO> checkAvailability(
            @PathVariable Long id,
            @RequestParam int quantity) {
        return ResponseEntity.ok(productService.checkAvailability(id, quantity));
    }

    // ========== GESTION PROMOTION - ROUTES AVEC ID SPÉCIFIQUES ==========

    /**
     * PATCH /products/{id}/promotion
     * Activer une promotion sur un produit (ARTISAN propriétaire uniquement)
     */
    @PatchMapping("/{id}/promotion")
    public ResponseEntity<ProductDTO> activatePromotion(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        
        // Vérifier que l'utilisateur courant est le propriétaire du produit
        User currentUser = getCurrentUser();
        ProductDTO product = productService.getProductById(id);
        
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        BigDecimal discountPrice = payload.get("discountPrice");
        if (discountPrice == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(productService.activatePromotion(id, discountPrice));
    }

    /**
     * DELETE /products/{id}/promotion
     * Désactiver une promotion sur un produit (ARTISAN propriétaire uniquement)
     */
    @DeleteMapping("/{id}/promotion")
    public ResponseEntity<ProductDTO> deactivatePromotion(@PathVariable Long id) {
        // Vérifier que l'utilisateur courant est le propriétaire du produit
        User currentUser = getCurrentUser();
        ProductDTO product = productService.getProductById(id);
        
        if (!product.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(productService.deactivatePromotion(id));
    }

    // ========== CRUD DE BASE - ROUTES GÉNÉRIQUES ==========

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(productDTO));
    }

    /**
     * GET /products
     * Récupère uniquement les produits actifs (PUBLIC - clients uniquement)
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getActiveProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.updateProduct(id, productDTO));
    }

    // NOTE: La suppression de produit est réservée aux artisans propriétaires via /api/ecommerce/artisan/products/{id}
    // L'admin ne peut que désactiver/activer via PATCH /api/products/{id}/status

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
