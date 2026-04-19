package tn.hypercloud.controller.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.forum.ForumComment;
import tn.hypercloud.service.forum.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/forum/{forumId}")
    public ResponseEntity<ForumComment> create(
            @PathVariable Long forumId,
            @RequestBody ForumComment comment) {
        return ResponseEntity.ok(commentService.create(forumId, comment));
    }

    @GetMapping
    public ResponseEntity<List<ForumComment>> getAll() {
        return ResponseEntity.ok(commentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumComment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForumComment> update(
            @PathVariable Long id,
            @RequestBody ForumComment comment) {
        return ResponseEntity.ok(commentService.update(id, comment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.ok("Commentaire supprimé avec succès");
    }
    @GetMapping("/forum/{forumId}")
    public ResponseEntity<List<ForumComment>> getByForum(@PathVariable Long forumId) {
        return ResponseEntity.ok(commentService.getByForum(forumId));
    }
}