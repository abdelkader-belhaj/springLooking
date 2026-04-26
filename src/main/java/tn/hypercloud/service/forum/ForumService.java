package tn.hypercloud.service.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.forum.Community;
import tn.hypercloud.entity.forum.Forum;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.forum.CommunityRepository;
import tn.hypercloud.repository.forum.ForumRepository;
import tn.hypercloud.security.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
    private final CommunityRepository communityRepository;
    private final AiService aiService; // ✅ ajouté

    public Forum create(Forum forum, Long communityId) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN &&
                user.getRole() != Role.CLIENT_TOURISTE) {
            throw new RuntimeException("Access denied");
        }

        Community community = communityRepository.findById(communityId)
                .orElseThrow();

        forum.setUser(user);
        forum.setCommunity(community);
        forum.setCreatedAt(LocalDateTime.now());
        forum.setLikesCount(0);
        forum.setDislikesCount(0);
        forum.setViews(0);

        // ✅ Analyse sentiment directe
        String sentiment = aiService.analyzeSentiment(forum.getContent());
        forum.setSentiment(sentiment);

        return forumRepository.save(forum);
    }

    public List<Forum> getAll() {
        return forumRepository.findAll();
    }

    public Forum getById(Long id) {
        return forumRepository.findById(id).orElseThrow();
    }

    public Forum update(Long id, Forum updated) {
        User user = SecurityUtils.getCurrentUser();
        Forum forum = getById(id);

        if (!forum.getUser().getId().equals(user.getId())
                && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }

        forum.setTitle(updated.getTitle());
        forum.setContent(updated.getContent());

        // ✅ Re-analyse sentiment à la modification
        String sentiment = aiService.analyzeSentiment(updated.getContent());
        forum.setSentiment(sentiment);

        return forumRepository.save(forum);
    }

    @Transactional
    public void delete(Long id) {
        User user = SecurityUtils.getCurrentUser();
        Forum forum = getById(id);

        if (!forum.getUser().getId().equals(user.getId())
                && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }

        forumRepository.delete(forum);
    }

    public List<Forum> getByCommunity(Long communityId) {
        return forumRepository.findByCommunityId(communityId);
    }
}