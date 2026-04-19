package tn.hypercloud.entity.forum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "total_posts")
    private Integer totalPosts;

    @Column(name = "total_members")
    private Integer totalMembers;

    @Column(name = "ai_moderation_enabled")
    private Boolean aiModerationEnabled;

    @Column(name = "moderation_level")
    private String moderationLevel;

    @OneToMany(mappedBy = "community", fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"community"})
    @Builder.Default
    private List<Forum> forums = new ArrayList<>();
}