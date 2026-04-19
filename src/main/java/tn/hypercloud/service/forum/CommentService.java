package tn.hypercloud.service.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.forum.Forum;
import tn.hypercloud.entity.forum.ForumComment;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.forum.ForumCommentRepository;
import tn.hypercloud.repository.forum.ForumRepository;
import tn.hypercloud.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final ForumCommentRepository commentRepository;
    private final ForumRepository forumRepository;

    public ForumComment create(Long forumId, ForumComment comment) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.CLIENT_TOURISTE) {
            throw new RuntimeException("Accès refusé");
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new RuntimeException("Forum introuvable"));

        comment.setForum(forum);
        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    public List<ForumComment> getAll() {
        return commentRepository.findAll();
    }

    public ForumComment getById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commentaire introuvable"));
    }

    public ForumComment update(Long id, ForumComment updated) {
        User user = SecurityUtils.getCurrentUser();
        ForumComment comment = getById(id);

        boolean isOwner = comment.getUser() != null && comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Accès refusé");
        }

        comment.setContent(updated.getContent());
        return commentRepository.save(comment);
    }

    public void delete(Long id) {
        User user = SecurityUtils.getCurrentUser();

        // Si le commentaire n'existe pas → on ignore sans erreur
        ForumComment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) return;

        boolean isOwner = comment.getUser() != null && comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Accès refusé");
        }

        commentRepository.delete(comment);
    }
    public List<ForumComment> getByForum(Long forumId) {
        return commentRepository.findByForumId(forumId);
    }
}