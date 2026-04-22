package tn.hypercloud.repository.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventReservation;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventReservationRepository
        extends JpaRepository<EventReservation, Integer> {

    List<EventReservation> findByUserId(Long userId);
    List<EventReservation> findByEventId(Integer eventId);
    List<EventReservation> findByStatus(EventReservation.ReservationStatus status); // ← garde juste celui-ci

    boolean existsByEventIdAndUserId(Integer eventId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EventReservation r where r.id = :id")
    Optional<EventReservation> findByIdForUpdate(@Param("id") Integer id);
}