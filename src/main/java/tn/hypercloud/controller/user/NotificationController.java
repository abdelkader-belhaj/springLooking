package tn.hypercloud.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.user.Notification;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.payload.response.NotificationResponse;
import tn.hypercloud.repository.user.NotificationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationRepository notifRepo;
    private final UserRepository userRepo;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        List<NotificationResponse> notifs = notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(notifs);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Notification notif = notifRepo.findById(id).orElseThrow(() -> new RuntimeException("Notif not found"));
        if (!notif.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        notif.setRead(true);
        notifRepo.save(notif);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Notification notif = notifRepo.findById(id).orElseThrow(() -> new RuntimeException("Notif not found"));
        if (!notif.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        notifRepo.delete(notif);
        return ResponseEntity.ok().build();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
