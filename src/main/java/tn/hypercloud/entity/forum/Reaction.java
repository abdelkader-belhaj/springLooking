package tn.hypercloud.entity.forum;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import tn.hypercloud.entity.user.User;

@Entity
@Table(name = "reaction")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String type;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "forum_id")
    @JsonIgnoreProperties({"reactions", "comments", "reviews"})
    private Forum forum;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "enabled", "authorities", "accountNonExpired",
            "accountNonLocked", "credentialsNonExpired", "faceEmbedding", "hibernateLazyInitializer", "handler"})
    private User user;
}