package tn.hypercloud.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventPayment;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventPaymentRepository
        extends JpaRepository<EventPayment, Integer> {

    // Trouver par reservation
    Optional<EventPayment> findByReservationId(
            Integer reservationId);

    // Trouver par status
    List<EventPayment> findByPaymentStatus(
            EventPayment.PaymentStatus status);

    // Vérifier si reservation déjà payée
    boolean existsByReservationId(Integer reservationId);
}