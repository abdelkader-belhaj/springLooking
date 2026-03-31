package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.OrderDetail;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    
    /**
     * Récupère tous les détails d'une commande
     */
    List<OrderDetail> findByOrderId(Long orderId);
    
    /**
     * Récupère tous les détails d'un produit (qui a acheté ce produit)
     */
    List<OrderDetail> findByProductId(Long productId);
    
    /**
     * Vérifier si un produit est dans une commande
     */
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
}
