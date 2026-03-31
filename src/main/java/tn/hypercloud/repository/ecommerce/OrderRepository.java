package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.Order;
import tn.hypercloud.entity.ecommerce.Order.OrderStatus;
import tn.hypercloud.entity.ecommerce.Order.PaymentStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Récupère toutes les commandes d'un utilisateur triées par date descendante
     */
    List<Order> findByUserIdOrderByIdDesc(Long userId);
    
    /**
     * Récupère les commandes par statut
     */
    List<Order> findByStatus(OrderStatus status);
    
    /**
     * Récupère les commandes par statut de paiement
     */
    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);
    
    /**
     * Cherche une commande par numéro unique
     */
    Optional<Order> findByOrderNumber(String orderNumber);
}
