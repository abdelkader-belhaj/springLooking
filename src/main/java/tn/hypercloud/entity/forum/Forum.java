package tn.hypercloud.entity.forum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Forum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String status;

    private Integer views;

    @Column(name = "likesCount")
    private Integer likesCount;

    @Column(name = "dislikesCount")
    private Integer dislikesCount;

    @Column(name = "flaggedByAI")
    private Boolean flaggedByAI;

    @Column(name = "toxicityScore")
    private Double toxicityScore;

    @Column(name = "aiDecision", length = 255)
    private String aiDecision;

    @Column(name = "containsForbiddenWords")
    private Boolean containsForbiddenWords;

    @Column(name = "aiStatus", length = 50)
    private String aiStatus;

    @Column(name = "aiReason", length = 255)
    private String aiReason;

    @Column(name = "sentiment", length = 20)
    private String sentiment;

    // 🔗 Community
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    @JsonIgnoreProperties({"forums"})
    private Community community;

    // 🔗 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({
            "password", "enabled", "authorities",
            "hibernateLazyInitializer", "handler", "faceEmbedding"
    })
    private User user;

    // 🔗 Comments
    @OneToMany(mappedBy = "forum", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<ForumComment> comments = new ArrayList<>();

    // 🔗 Reactions
    @OneToMany(mappedBy = "forum", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Reaction> reactions = new ArrayList<>();

    // 🔗 Reviews
    @OneToMany(mappedBy = "forum", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();
}