package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages_transport")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MessageTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_course", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sender", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(updatable = false)
    private LocalDateTime dateEnvoi;

    // === Accusés de réception ===
    private boolean delivered = false;

    @Column(name = "is_read")          // on évite le mot réservé "read"
    private boolean isRead = false;

    private LocalDateTime dateLecture;

    @PrePersist
    protected void onCreate() {
        dateEnvoi = LocalDateTime.now();
    }
}