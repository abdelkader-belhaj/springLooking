package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.ProductCategory;
import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    
    /**
     * Récupère toutes les catégories racine (sans parent) triées par ordre d'affichage
     */
    List<ProductCategory> findByParentIsNullOrderByDisplayOrder();
    
    /**
     * Récupère toutes les catégories racine (sans parent)
     */
    List<ProductCategory> findByParentIsNull();
    
    /**
     * Récupère les sous-catégories d'une catégorie parent
     */
    List<ProductCategory> findByParentOrderByDisplayOrder(ProductCategory parent);
    
    /**
     * Récupère les sous-catégories d'une catégorie parent (sans tri)
     */
    List<ProductCategory> findByParent(ProductCategory parent);
    
    /**
     * Recherche les catégories par nom (case-insensitive)
     */
    List<ProductCategory> findByNameContainingIgnoreCase(String name);
}
