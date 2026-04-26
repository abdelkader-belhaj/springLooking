package tn.hypercloud.service.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.forum.Forum;
import tn.hypercloud.entity.forum.Reaction;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.forum.ForumRepository;
import tn.hypercloud.repository.forum.ReactionRepository;
import tn.hypercloud.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ForumRepository forumRepository;

    public Reaction create(Long forumId, Reaction reaction) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.CLIENT_TOURISTE) {
            throw new RuntimeException("Accès refusé");
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new RuntimeException("Forum introuvable"));

        // ✅ Chercher réaction existante
        var existing = reactionRepository.findFirstByForumIdAndUserId(forumId, user.getId());

        if (existing.isPresent()) {
            Reaction old = existing.get();

            // ✅ Même type → toggle OFF (supprimer sans recréer)
            if (old.getType().equalsIgnoreCase(reaction.getType())) {
                if ("LIKE".equalsIgnoreCase(old.getType())) {
                    forum.setLikesCount(Math.max(0, (forum.getLikesCount() == null ? 0 : forum.getLikesCount()) - 1));
                } else if ("DISLIKE".equalsIgnoreCase(old.getType())) {
                    forum.setDislikesCount(Math.max(0, (forum.getDislikesCount() == null ? 0 : forum.getDislikesCount()) - 1));
                }
                forumRepository.save(forum);
                reactionRepository.delete(old);
                return old; // retourne l'ancienne sans recréer
            }

            // ✅ Type différent → supprimer l'ancienne et créer la nouvelle
            if ("LIKE".equalsIgnoreCase(old.getType())) {
                forum.setLikesCount(Math.max(0, (forum.getLikesCount() == null ? 0 : forum.getLikesCount()) - 1));
            } else if ("DISLIKE".equalsIgnoreCase(old.getType())) {
                forum.setDislikesCount(Math.max(0, (forum.getDislikesCount() == null ? 0 : forum.getDislikesCount()) - 1));
            }
            reactionRepository.delete(old);
        }

        // ✅ Créer la nouvelle réaction
        reaction.setForum(forum);
        reaction.setUser(user);
        reaction.setCreatedAt(LocalDateTime.now());

        if ("LIKE".equalsIgnoreCase(reaction.getType())) {
            forum.setLikesCount((forum.getLikesCount() == null ? 0 : forum.getLikesCount()) + 1);
        } else if ("DISLIKE".equalsIgnoreCase(reaction.getType())) {
            forum.setDislikesCount((forum.getDislikesCount() == null ? 0 : forum.getDislikesCount()) + 1);
        }

        forumRepository.save(forum);
        return reactionRepository.save(reaction);
    }

    public List<Reaction> getAll() {
        return reactionRepository.findAll();
    }

    public Reaction getById(Long id) {
        return reactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reaction introuvable"));
    }

    public void delete(Long id) {
        User user = SecurityUtils.getCurrentUser();
        Reaction reaction = getById(id);

        boolean isOwner = reaction.getUser() != null && reaction.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Accès refusé");
        }

        Forum forum = reaction.getForum();

        if ("LIKE".equalsIgnoreCase(reaction.getType())) {
            forum.setLikesCount(Math.max(0, (forum.getLikesCount() == null ? 0 : forum.getLikesCount()) - 1));
        } else if ("DISLIKE".equalsIgnoreCase(reaction.getType())) {
            forum.setDislikesCount(Math.max(0, (forum.getDislikesCount() == null ? 0 : forum.getDislikesCount()) - 1));
        }

        forumRepository.save(forum);
        reactionRepository.delete(reaction);
    }

    public List<Reaction> getByForum(Long forumId) {
        return reactionRepository.findByForumId(forumId);
    }

    public void deleteByForumAndUser(Long forumId, Long userId) {
        // ✅ findFirstByForumIdAndUserId au lieu de findByForumIdAndUserId
        reactionRepository.findFirstByForumIdAndUserId(forumId, userId)
                .ifPresent(reaction -> {
                    Forum forum = reaction.getForum();
                    if ("LIKE".equalsIgnoreCase(reaction.getType())) {
                        forum.setLikesCount(Math.max(0,
                                forum.getLikesCount() == null ? 0 : forum.getLikesCount() - 1));
                    } else if ("DISLIKE".equalsIgnoreCase(reaction.getType())) {
                        forum.setDislikesCount(Math.max(0,
                                forum.getDislikesCount() == null ? 0 : forum.getDislikesCount() - 1));
                    }
                    forumRepository.save(forum);
                    reactionRepository.delete(reaction);
                });
    }
}