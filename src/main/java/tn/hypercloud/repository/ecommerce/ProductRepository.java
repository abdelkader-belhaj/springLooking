package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.Product;
import tn.hypercloud.entity.ecommerce.ProductCategory;
import tn.hypercloud.entity.user.User;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * Recherche textuelle sur le nom et la description du produit
     */
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
    
    /**
     * Recherche par catégorie
     */
    List<Product> findByCategory(ProductCategory category);
    
    /**
     * Recherche par artisan (vendeur)
     */
    List<Product> findByUser(User user);
    
    /**
     * Produits d'un artisan triés par nombre de ventes (bestsellers pour cet artisan)
     */
    List<Product> findByUserIdOrderBySalesCountDesc(Long userId);
    
    /**
     * Les produits les plus vendus globalement avec limite
     */
    @Query("SELECT p FROM Product p ORDER BY p.salesCount DESC")
    List<Product> findBestsellers(Pageable pageable);
}

