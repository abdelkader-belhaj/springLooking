package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.transport.MessageTransport;
import java.time.LocalDateTime;
import java.util.List;

public interface MessageTransportRepository extends JpaRepository<MessageTransport, Long> {

    // Méthode corrigée (utilise le nom réel de la propriété + underscore)
// In MessageTransportRepository
    List<MessageTransport> findByCourse_IdCourseOrderByDateEnvoiAsc(Long courseId);
    // Marquer comme délivré
    @Modifying
    @Query("UPDATE MessageTransport m SET m.delivered = true WHERE m.id = :messageId")
    void markAsDelivered(@Param("messageId") Long messageId);

    // Marquer comme lu
    @Modifying
    @Query("UPDATE MessageTransport m SET m.isRead = true, m.dateLecture = :date WHERE m.id = :messageId")
    void markAsRead(@Param("messageId") Long messageId, @Param("date") LocalDateTime date);
}