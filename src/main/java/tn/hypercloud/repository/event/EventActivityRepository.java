package tn.hypercloud.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventActivity;

@Repository
public interface EventActivityRepository extends JpaRepository<EventActivity, Integer> {
}
