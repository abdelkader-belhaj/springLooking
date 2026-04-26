package tn.hypercloud.controller.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.forum.Reaction;
import tn.hypercloud.service.forum.ReactionService;

import java.util.List;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/forum/{forumId}")
    public ResponseEntity<Reaction> create(
            @PathVariable Long forumId,
            @RequestBody Reaction reaction) {
        return ResponseEntity.ok(reactionService.create(forumId, reaction));
    }

    @GetMapping
    public ResponseEntity<List<Reaction>> getAll() {
        return ResponseEntity.ok(reactionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reactionService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        reactionService.delete(id);
        return ResponseEntity.ok("Reaction supprimée avec succès");
    }
    @GetMapping("/forum/{forumId}")
    public ResponseEntity<List<Reaction>> getByForum(@PathVariable Long forumId) {
        return ResponseEntity.ok(reactionService.getByForum(forumId));
    }

    @DeleteMapping("/forum/{forumId}/user/{userId}")
    public ResponseEntity<String> deleteByForumAndUser(
            @PathVariable Long forumId,
            @PathVariable Long userId) {
        reactionService.deleteByForumAndUser(forumId, userId);
        return ResponseEntity.ok("Reaction supprimée");
    }
}