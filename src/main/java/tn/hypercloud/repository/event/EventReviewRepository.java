package tn.hypercloud.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventReview;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventReviewRepository extends JpaRepository<EventReview, Integer> {

    List<EventReview> findByEventIdOrderByUpdatedAtDesc(Integer eventId);

    Optional<EventReview> findByEventIdAndUserId(Integer eventId, Long userId);
}
