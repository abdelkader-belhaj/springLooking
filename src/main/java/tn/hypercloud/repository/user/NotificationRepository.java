package tn.hypercloud.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.user.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
}
