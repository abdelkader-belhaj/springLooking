package tn.hypercloud.controller.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.forum.Forum;
import tn.hypercloud.service.forum.ForumService;

import java.util.List;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @PostMapping("/community/{communityId}")
    public ResponseEntity<Forum> create(
            @PathVariable Long communityId,
            @RequestBody Forum forum) {
        return ResponseEntity.ok(forumService.create(forum, communityId));
    }

    @GetMapping
    public ResponseEntity<List<Forum>> getAll() {
        return ResponseEntity.ok(forumService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Forum> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(forumService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Forum> update(
            @PathVariable Long id,
            @RequestBody Forum forum) {
        return ResponseEntity.ok(forumService.update(id, forum));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        forumService.delete(id);
        return ResponseEntity.ok("Forum supprimé avec succès");
    }
    // ✅ Après
    @GetMapping("/community/{communityId}")
    public ResponseEntity<List<Forum>> getByCommunity(@PathVariable Long communityId) {
        try {
            List<Forum> forums = forumService.getByCommunity(communityId);
            return ResponseEntity.ok(forums);
        } catch (Exception e) {
            System.err.println("=== ERREUR getByCommunity: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}