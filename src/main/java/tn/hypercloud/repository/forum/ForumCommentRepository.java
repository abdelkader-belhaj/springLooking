package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.ForumComment;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
}
