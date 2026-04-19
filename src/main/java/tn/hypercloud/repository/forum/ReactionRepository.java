package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.forum.Reaction;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByForumId(Long forumId);
    Optional<Reaction> findFirstByForumIdAndUserId(Long forumId, Long userId);
}