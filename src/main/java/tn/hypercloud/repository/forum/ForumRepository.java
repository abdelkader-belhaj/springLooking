package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.Forum;

import java.util.List;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {

    // 🔍 Récupérer les forums d’une communauté
    List<Forum> findByCommunityId(Long communityId);
}