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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "createdBy")
    private Long createdBy;

    @Column(name = "totalPosts")
    private Integer totalPosts;

    @Column(name = "totalMembers")
    private Integer totalMembers;

    @Column(name = "aiModerationEnabled")
    private Boolean aiModerationEnabled;

    @Column(name = "moderationLevel", length = 50)
    private String moderationLevel;

    // 🔗 Forums liés
    @OneToMany(mappedBy = "community", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"community"})
    private List<Forum> forums = new ArrayList<>();

    // 🔄 Auto date
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}