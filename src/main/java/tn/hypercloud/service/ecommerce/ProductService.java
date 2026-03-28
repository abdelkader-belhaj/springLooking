package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.ProductDTO;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.entity.ecommerce.ProductCategory;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.ecommerce.ProductRepository;
import tn.hypercloud.repository.ecommerce.ProductCategoryRepository;
import tn.hypercloud.repository.user.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final UserRepository userRepository;

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
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
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
