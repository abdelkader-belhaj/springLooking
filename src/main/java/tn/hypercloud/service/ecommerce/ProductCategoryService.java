package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.ProductCategoryDTO;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.entity.ecommerce.ProductCategory;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.repository.ecommerce.ProductCategoryRepository;
import tn.hypercloud.repository.ecommerce.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCategoryService {
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;

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
                .displayOrder(productCategoryDTO.getDisplayOrder() != null ? productCategoryDTO.getDisplayOrder() : 0)
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

    // ========== NOUVELLES MÉTHODES POUR LES ENDPOINTS ==========

    /**
     * Récupère toutes les catégories racine triées par ordre d'affichage (PUBLIC)
     */
    public List<ProductCategoryDTO> getRootCategories() {
        return productCategoryRepository.findByParentIsNullOrderByDisplayOrder()
                .stream()
                .map(this::mapToDTOWithTree)
                .collect(Collectors.toList());
    }

    /**
     * Récupère l'arborescence complète d'une catégorie (toutes les profondeurs)
     */
    public ProductCategoryDTO getCategoryTree(Long categoryId) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        return mapToDTOWithTree(category);
    }

    /**
     * Récupère les produits d'une catégorie (PUBLIC)
     */
    public List<ProductDTO> getCategoryProducts(Long categoryId) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        
        List<Product> products = productRepository.findByCategory(category);
        return products.stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recherche les catégories par nom
     */
    public List<ProductCategoryDTO> searchCategoriesByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllProductCategories();
        }
        return productCategoryRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les enfants directs d'une catégorie
     */
    public List<ProductCategoryDTO> getChildCategories(Long categoryId) {
        ProductCategory parent = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        
        return productCategoryRepository.findByParentOrderByDisplayOrder(parent)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour l'ordre d'affichage d'une catégorie
     */
    public ProductCategoryDTO updateCategoryOrder(Long categoryId, Integer newOrder) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        
        category.setDisplayOrder(newOrder != null ? newOrder : 0);
        ProductCategory updated = productCategoryRepository.save(category);
        return mapToDTO(updated);
    }

    private ProductCategoryDTO mapToDTO(ProductCategory category) {
        return ProductCategoryDTO.builder()
                .id(category.getId())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .name(category.getName())
                .description(category.getDescription())
                .image(category.getImage())
                .displayOrder(category.getDisplayOrder())
                .build();
    }

    /**
     * Mapper avec les enfants (arborescence complète)
     */
    private ProductCategoryDTO mapToDTOWithTree(ProductCategory category) {
        List<ProductCategoryDTO> children = category.getChildren() != null
                ? category.getChildren().stream()
                    .sorted((a, b) -> a.getDisplayOrder().compareTo(b.getDisplayOrder()))
                    .map(this::mapToDTOWithTree)
                    .collect(Collectors.toList())
                : List.of();
        
        return ProductCategoryDTO.builder()
                .id(category.getId())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .name(category.getName())
                .description(category.getDescription())
                .image(category.getImage())
                .displayOrder(category.getDisplayOrder())
                .children(children)
                .build();
    }

    /**
     * Helper pour mapper un Product en ProductDTO
     */
    private ProductDTO mapProductToDTO(Product product) {
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
