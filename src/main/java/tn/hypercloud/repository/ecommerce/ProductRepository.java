package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
