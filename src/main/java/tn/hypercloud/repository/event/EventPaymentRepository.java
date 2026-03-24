package tn.hypercloud.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventPayment;

@Repository
public interface EventPaymentRepository extends JpaRepository<EventPayment, Integer> {
}
