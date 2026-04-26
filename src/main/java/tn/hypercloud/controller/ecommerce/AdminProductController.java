package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.service.ecommerce.ProductService;
import tn.hypercloud.payload.response.ApiResponse;
import java.util.List;
import java.util.Map;

/**
 * Admin API endpoints for marketplace management
 * Provides simplified API for marketplace UI
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductService productService;

    /**
     * Get all products
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(
                ApiResponse.success("Products retrieved", products)
        );
    }

    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProduct(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Product retrieved", product)
        );
    }

    // NOTE: L'admin ne peut pas modifier le contenu des produits.
    // Seul l'artisan peut modifier via PUT /api/ecommerce/artisan/products/{id}
    // L'admin peut uniquement activer/désactiver via PATCH /api/products/{id}/status

    /**
     * Toggle product status (admin only)
     * PATCH /api/products/{id}/status
     * Changes product status between active and inactive
     * Sends email notification to artisan when disabled
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductDTO>> toggleProductStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        if (newStatus == null || (!newStatus.equals("active") && !newStatus.equals("inactive"))) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Invalid status. Must be 'active' or 'inactive'")
            );
        }
        
        ProductDTO updated = productService.updateProductStatus(id, newStatus);
        return ResponseEntity.ok(
                ApiResponse.success("Product status updated", updated)
        );
    }

}

