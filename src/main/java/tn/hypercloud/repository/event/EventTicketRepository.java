package tn.hypercloud.repository.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventTicket;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventTicketRepository extends JpaRepository<EventTicket, Integer> {

    List<EventTicket> findByReservationIdOrderByTicketNumberAsc(Integer reservationId);

    boolean existsByReservationId(Integer reservationId);

    long countByReservationId(Integer reservationId);

    long countByReservationIdAndUsedTrue(Integer reservationId);

    Optional<EventTicket> findByTicketCode(String ticketCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from EventTicket t where t.ticketCode = :ticketCode")
    Optional<EventTicket> findByTicketCodeForUpdate(@Param("ticketCode") String ticketCode);
}
