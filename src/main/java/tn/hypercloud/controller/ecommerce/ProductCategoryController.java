package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.ProductCategoryDTO;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.service.ecommerce.ProductCategoryService;
import tn.hypercloud.payload.response.ApiResponse;
import java.time.LocalDateTime;
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
    public ResponseEntity<ApiResponse<List<ProductCategoryDTO>>> getRootCategories() {
        List<ProductCategoryDTO> categories = productCategoryService.getRootCategories();
        return ResponseEntity.ok(new ApiResponse<>(true, "Categories loaded successfully", categories, LocalDateTime.now()));
    }

    /**
     * GET /categories/{id}/tree
     * Récupère l'arborescence complète d'une catégorie (PUBLIC)
     */
    @GetMapping("/{id}/tree")
    public ResponseEntity<ApiResponse<ProductCategoryDTO>> getCategoryTree(@PathVariable Long id) {
        ProductCategoryDTO categoryTree = productCategoryService.getCategoryTree(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category tree loaded successfully", categoryTree, LocalDateTime.now()));
    }

    /**
     * GET /categories/{id}/products
     * Récupère tous les produits d'une catégorie (PUBLIC)
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getCategoryProducts(@PathVariable Long id) {
        List<ProductDTO> products = productCategoryService.getCategoryProducts(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Products loaded successfully", products, LocalDateTime.now()));
    }

    /**
     * GET /categories/search?name={query}
     * Recherche les catégories par nom (PUBLIC)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductCategoryDTO>>> searchCategories(@RequestParam(required = false) String name) {
        List<ProductCategoryDTO> results = productCategoryService.searchCategoriesByName(name);
        return ResponseEntity.ok(new ApiResponse<>(true, "Search completed successfully", results, LocalDateTime.now()));
    }

    // ========== ROUTES AVEC ID ==========

    /**
     * GET /categories/{id}
     * Récupère une catégorie avec ses enfants directs
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductCategoryDTO>> getProductCategoryById(@PathVariable Long id) {
        ProductCategoryDTO category = productCategoryService.getProductCategoryById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category loaded successfully", category, LocalDateTime.now()));
    }

    // ========== ROUTES D'ADMINISTRATION (ADMIN) ==========

    /**
     * GET /categories/{id}/children
     * Récupère les enfants directs d'une catégorie (pour le filtrage)
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<ProductCategoryDTO>>> getChildCategories(@PathVariable Long id) {
        List<ProductCategoryDTO> children = productCategoryService.getChildCategories(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Child categories loaded successfully", children, LocalDateTime.now()));
    }

    /**
     * POST /categories
     * Créer une nouvelle catégorie (ADMIN)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductCategoryDTO>> createProductCategory(@RequestBody ProductCategoryDTO productCategoryDTO) {
        ProductCategoryDTO created = productCategoryService.createProductCategory(productCategoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Category created successfully", created, LocalDateTime.now()));
    }

    /**
     * PATCH /categories/{id}
     * Modifier une catégorie existante (ADMIN)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductCategoryDTO>> updateProductCategory(
            @PathVariable Long id,
            @RequestBody ProductCategoryDTO productCategoryDTO) {
        ProductCategoryDTO updated = productCategoryService.updateProductCategory(id, productCategoryDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category updated successfully", updated, LocalDateTime.now()));
    }

    /**
     * PATCH /categories/{id}/order
     * Changer l'ordre d'affichage d'une catégorie (ADMIN)
     */
    @PatchMapping("/{id}/order")
    public ResponseEntity<ApiResponse<ProductCategoryDTO>> updateCategoryOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        Integer newOrder = payload.get("displayOrder");
        if (newOrder == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Display order is required", null, LocalDateTime.now()));
        }
        ProductCategoryDTO updated = productCategoryService.updateCategoryOrder(id, newOrder);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category order updated successfully", updated, LocalDateTime.now()));
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
     * GET /categories/all (OLD)
     * Garder la compatibilité avec l'ancienne route
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProductCategoryDTO>>> getAllProductCategories() {
        List<ProductCategoryDTO> categories = productCategoryService.getAllProductCategories();
        return ResponseEntity.ok(new ApiResponse<>(true, "All categories loaded successfully", categories, LocalDateTime.now()));
    }

    /**
     * PUT /categories/{id} (OLD)
     * Garder la compatibilité avec l'ancienne route
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductCategoryDTO>> updateProductCategoryLegacy(
            @PathVariable Long id,
            @RequestBody ProductCategoryDTO productCategoryDTO) {
        ProductCategoryDTO updated = productCategoryService.updateProductCategory(id, productCategoryDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category updated successfully", updated, LocalDateTime.now()));
    }
}
