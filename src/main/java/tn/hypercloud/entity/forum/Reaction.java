package tn.hypercloud.entity.forum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "reaction")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String type; // LIKE / DISLIKE

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    // 🔗 Forum
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id")
    @JsonIgnoreProperties({"reactions", "comments", "reviews", "community", "user"})
    private Forum forum;

    // 🔗 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({
            "password", "enabled", "authorities",
            "accountNonExpired", "accountNonLocked",
            "credentialsNonExpired", "faceEmbedding",
            "hibernateLazyInitializer", "handler"
    })
    private User user;
}