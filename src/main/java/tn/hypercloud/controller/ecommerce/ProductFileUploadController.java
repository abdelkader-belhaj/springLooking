package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.dto.ecommerce.ProductImageUploadDTO;
import tn.hypercloud.service.ecommerce.ProductService;
import tn.hypercloud.service.ecommerce.FileUploadService;
import java.io.IOException;

@RestController
@RequestMapping("/api/ecommerce/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductFileUploadController {
    
    private final ProductService productService;
    private final FileUploadService fileUploadService;

    /**
     * Upload product image
     * POST /api/ecommerce/products/{productId}/upload-image
     * Form-data: file (MultipartFile)
     * Query params: replaceExisting (boolean, default: true)
     */
    @PostMapping("/{productId}/upload-image")
    public ResponseEntity<ProductImageUploadDTO> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceExisting", defaultValue = "true") boolean replaceExisting) {
        
        try {
            ProductDTO productDTO = productService.getProductById(productId);
            
            // Delete old image if exists and replace flag is true
            if (replaceExisting && productDTO.getImage() != null && !productDTO.getImage().isEmpty()) {
                fileUploadService.deleteProductImage(productDTO.getImage());
            }
            
            // Upload new image
            String imagePath = fileUploadService.uploadProductImage(file, productId);
            
            // Update product with new image path
            productDTO.setImage(imagePath);
            productService.updateProduct(productId, productDTO);
            
            ProductImageUploadDTO response = ProductImageUploadDTO.builder()
                    .productId(productId)
                    .imageUrl(imagePath)
                    .message("Image uploaded successfully")
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            ProductImageUploadDTO error = ProductImageUploadDTO.builder()
                    .productId(productId)
                    .message("Error uploading image: " + e.getMessage())
                    .success(false)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            ProductImageUploadDTO error = ProductImageUploadDTO.builder()
                    .productId(productId)
                    .message(e.getMessage())
                    .success(false)
                    .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Delete product image
     * DELETE /api/ecommerce/products/{productId}/delete-image
     */
    @DeleteMapping("/{productId}/delete-image")
    public ResponseEntity<ProductImageUploadDTO> deleteProductImage(@PathVariable Long productId) {
        try {
            ProductDTO productDTO = productService.getProductById(productId);
            
            if (productDTO.getImage() == null || productDTO.getImage().isEmpty()) {
                return ResponseEntity.ok(ProductImageUploadDTO.builder()
                        .productId(productId)
                        .message("No image to delete")
                        .success(true)
                        .build());
            }
            
            // Delete the image file
            fileUploadService.deleteProductImage(productDTO.getImage());
            
            // Update product with null image
            productDTO.setImage(null);
            productService.updateProduct(productId, productDTO);
            
            return ResponseEntity.ok(ProductImageUploadDTO.builder()
                    .productId(productId)
                    .message("Image deleted successfully")
                    .success(true)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ProductImageUploadDTO.builder()
                            .productId(productId)
                            .message(e.getMessage())
                            .success(false)
                            .build());
        }
    }
}
