package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
