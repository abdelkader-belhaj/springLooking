package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.Forum;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {
}
