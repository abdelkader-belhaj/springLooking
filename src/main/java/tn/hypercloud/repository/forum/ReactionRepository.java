package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.Reaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    // 🔍 Récupérer toutes les réactions d’un forum
    List<Reaction> findByForumId(Long forumId);

    // 🔍 Vérifier si un user a déjà réagi
    Optional<Reaction> findFirstByForumIdAndUserId(Long forumId, Long userId);
}