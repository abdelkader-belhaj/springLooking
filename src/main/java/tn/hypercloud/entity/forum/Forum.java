package tn.hypercloud.entity.forum;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    @Column(name = "likesCount")
    private Integer likesCount;

    @Column(name = "dislikesCount")
    private Integer dislikesCount;

    private Integer views;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @OneToMany(mappedBy = "forum")
    @Builder.Default
    private List<ForumComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "forum")
    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "forum")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
}
