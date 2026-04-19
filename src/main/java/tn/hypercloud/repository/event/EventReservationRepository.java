package tn.hypercloud.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventReservation;

@Repository
public interface EventReservationRepository extends JpaRepository<EventReservation, Integer> {
}
