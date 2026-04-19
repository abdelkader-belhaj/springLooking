package tn.hypercloud.entity.forum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "`comment`")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "flaggedByAI")
    private Boolean flaggedByAI;

    @Column(name = "aiStatus", length = 50)
    private String aiStatus;

    @Column(name = "aiReason", length = 255)
    private String aiReason;

    // 🔗 Forum
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id")
    @JsonIgnoreProperties({"comments", "reactions", "reviews", "community", "user"})
    private Forum forum;

    // 🔗 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({
            "password", "enabled", "authorities",
            "hibernateLazyInitializer", "handler", "faceEmbedding"
    })
    private User user;
}