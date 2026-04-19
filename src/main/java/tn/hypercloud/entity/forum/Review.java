package tn.hypercloud.entity.forum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "flaggedByAI")
    private Boolean flaggedByAI;

    @Column(length = 50)
    private String status;

    // Relation avec Forum
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id")
    @JsonIgnoreProperties({"reactions", "comments", "reviews"})
    private Forum forum;

    // Relation avec User
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