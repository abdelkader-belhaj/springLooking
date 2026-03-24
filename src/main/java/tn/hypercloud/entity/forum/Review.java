package tn.hypercloud.entity.forum;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id")
    private Forum forum;
}
