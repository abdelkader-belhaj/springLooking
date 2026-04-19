package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.forum.ForumComment;

import java.util.List;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
    List<ForumComment> findByForumId(Long forumId);
}