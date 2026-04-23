package tn.hypercloud.service.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.forum.Community;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.forum.CommunityRepository;
import tn.hypercloud.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;

    // CREATE
    public Community create(Community community) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seul ADMIN peut créer une community");
        }

        community.setCreatedAt(LocalDateTime.now());
        community.setCreatedBy(user.getId());
        community.setTotalMembers(0);
        community.setTotalPosts(0);

        return communityRepository.save(community);
    }

    // GET ALL
    public List<Community> getAll() {
        return communityRepository.findAll();
    }

    // GET BY ID
    public Community getById(Long id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Community introuvable"));
    }

    // UPDATE
    public Community update(Long id, Community updated) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seul ADMIN peut modifier");
        }

        Community c = getById(id);

        c.setName(updated.getName());
        c.setDescription(updated.getDescription());
        c.setCategory(updated.getCategory());
        c.setModerationLevel(updated.getModerationLevel());

        return communityRepository.save(c);
    }

    // DELETE
    public void delete(Long id) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seul ADMIN peut supprimer");
        }

        communityRepository.deleteById(id);
    }
}