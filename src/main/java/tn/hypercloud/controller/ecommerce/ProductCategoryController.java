package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.ProductCategoryDTO;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.service.ecommerce.ProductCategoryService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecommerce/categories")
@RequiredArgsConstructor
public class ProductCategoryController {
    private final ProductCategoryService productCategoryService;

    // ========== ROUTES PUBLIQUES SPÉCIFIQUES ==========

    /**
     * GET /categories
     * Récupère toutes les catégories racine triées par ordre (PUBLIC)
     */
    @GetMapping
    public ResponseEntity<List<ProductCategoryDTO>> getRootCategories() {
        return ResponseEntity.ok(productCategoryService.getRootCategories());
    }

    /**
     * GET /categories/{id}/tree
     * Récupère l'arborescence complète d'une catégorie (PUBLIC)
     */
    @GetMapping("/{id}/tree")
    public ResponseEntity<ProductCategoryDTO> getCategoryTree(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getCategoryTree(id));
    }

    /**
     * GET /categories/{id}/products
     * Récupère tous les produits d'une catégorie (PUBLIC)
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductDTO>> getCategoryProducts(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getCategoryProducts(id));
    }

    /**
     * GET /categories/search?name={query}
     * Recherche les catégories par nom (PUBLIC)
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductCategoryDTO>> searchCategories(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(productCategoryService.searchCategoriesByName(name));
    }

    // ========== ROUTES AVEC ID ==========

    /**
     * GET /categories/{id}
     * Récupère une catégorie avec ses enfants directs
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> getProductCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getProductCategoryById(id));
    }

    // ========== ROUTES D'ADMINISTRATION (ADMIN) ==========

    /**
     * POST /categories
     * Créer une nouvelle catégorie (ADMIN)
     */
    @PostMapping
    public ResponseEntity<ProductCategoryDTO> createProductCategory(@RequestBody ProductCategoryDTO productCategoryDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productCategoryService.createProductCategory(productCategoryDTO));
    }

    /**
     * PATCH /categories/{id}
     * Modifier une catégorie existante (ADMIN)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> updateProductCategory(
            @PathVariable Long id,
            @RequestBody ProductCategoryDTO productCategoryDTO) {
        return ResponseEntity.ok(productCategoryService.updateProductCategory(id, productCategoryDTO));
    }

    /**
     * PATCH /categories/{id}/order
     * Changer l'ordre d'affichage d'une catégorie (ADMIN)
     */
    @PatchMapping("/{id}/order")
    public ResponseEntity<ProductCategoryDTO> updateCategoryOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        Integer newOrder = payload.get("displayOrder");
        if (newOrder == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(productCategoryService.updateCategoryOrder(id, newOrder));
    }

    /**
     * DELETE /categories/{id}
     * Supprimer une catégorie (ADMIN)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductCategory(@PathVariable Long id) {
        productCategoryService.deleteProductCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ========== ROUTES COMPATIBILITÉ LEGACY ==========

    /**
     * GET /categories (OLD)
     * Garder la compatibilité avec l'ancienne route
     */
    @GetMapping("/all")
    public ResponseEntity<List<ProductCategoryDTO>> getAllProductCategories() {
        return ResponseEntity.ok(productCategoryService.getAllProductCategories());
    }

    /**
     * PUT /categories/{id} (OLD)
     * Garder la compatibilité avec l'ancienne route
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> updateProductCategoryLegacy(
            @PathVariable Long id,
            @RequestBody ProductCategoryDTO productCategoryDTO) {
        return ResponseEntity.ok(productCategoryService.updateProductCategory(id, productCategoryDTO));
    }
}
