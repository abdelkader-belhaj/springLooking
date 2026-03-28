package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.ProductCategoryDTO;
import tn.hypercloud.service.ecommerce.ProductCategoryService;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {
    private final ProductCategoryService productCategoryService;

    @PostMapping
    public ResponseEntity<ProductCategoryDTO> createProductCategory(@RequestBody ProductCategoryDTO productCategoryDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productCategoryService.createProductCategory(productCategoryDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> getProductCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getProductCategoryById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductCategoryDTO>> getAllProductCategories() {
        return ResponseEntity.ok(productCategoryService.getAllProductCategories());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> updateProductCategory(@PathVariable Long id, @RequestBody ProductCategoryDTO productCategoryDTO) {
        return ResponseEntity.ok(productCategoryService.updateProductCategory(id, productCategoryDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductCategory(@PathVariable Long id) {
        productCategoryService.deleteProductCategory(id);
        return ResponseEntity.noContent().build();
    }
}
