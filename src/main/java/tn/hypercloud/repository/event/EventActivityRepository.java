package tn.hypercloud.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventActivity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventActivityRepository extends JpaRepository<EventActivity, Integer> {

    List<EventActivity> findByType(EventActivity.EventType type);
    List<EventActivity> findByStatus(EventActivity.EventStatus status);
    List<EventActivity> findByCategoryId(Integer categoryId);
    List<EventActivity> findByOrganizerId(Long organizerId);
    List<EventActivity> findByCity(String city);
    List<EventActivity> findByStartDateBetweenOrderByStartDateAsc(LocalDateTime start, LocalDateTime end);
    long countByStartDateBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(EventActivity.EventStatus status);
}