package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.ProductCategoryDTO;
import tn.hypercloud.entity.ecommerce.ProductCategory;
import tn.hypercloud.repository.ecommerce.ProductCategoryRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCategoryService {
    private final ProductCategoryRepository productCategoryRepository;

    public ProductCategoryDTO createProductCategory(ProductCategoryDTO productCategoryDTO) {
        ProductCategory parent = null;
        if (productCategoryDTO.getParentId() != null) {
            parent = productCategoryRepository.findById(productCategoryDTO.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found with id: " + productCategoryDTO.getParentId()));
        }
        
        ProductCategory category = ProductCategory.builder()
                .parent(parent)
                .name(productCategoryDTO.getName())
                .description(productCategoryDTO.getDescription())
                .image(productCategoryDTO.getImage())
                .build();
        ProductCategory saved = productCategoryRepository.save(category);
        return mapToDTO(saved);
    }

    public ProductCategoryDTO getProductCategoryById(Long id) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductCategory not found with id: " + id));
        return mapToDTO(category);
    }

    public List<ProductCategoryDTO> getAllProductCategories() {
        return productCategoryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ProductCategoryDTO updateProductCategory(Long id, ProductCategoryDTO productCategoryDTO) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductCategory not found with id: " + id));
        
        category.setName(productCategoryDTO.getName());
        category.setDescription(productCategoryDTO.getDescription());
        category.setImage(productCategoryDTO.getImage());
        
        ProductCategory updated = productCategoryRepository.save(category);
        return mapToDTO(updated);
    }

    public void deleteProductCategory(Long id) {
        if (!productCategoryRepository.existsById(id)) {
            throw new RuntimeException("ProductCategory not found with id: " + id);
        }
        productCategoryRepository.deleteById(id);
    }

    private ProductCategoryDTO mapToDTO(ProductCategory category) {
        return ProductCategoryDTO.builder()
                .id(category.getId())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .name(category.getName())
                .description(category.getDescription())
                .image(category.getImage())
                .build();
    }
}
