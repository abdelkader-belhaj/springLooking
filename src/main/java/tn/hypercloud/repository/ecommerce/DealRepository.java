package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.Deal;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {
}
