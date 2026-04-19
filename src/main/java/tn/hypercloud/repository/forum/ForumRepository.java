package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.forum.Forum;

import java.util.List;

public interface ForumRepository extends JpaRepository<Forum, Long> {
    List<Forum> findByCommunityId(Long communityId);
}