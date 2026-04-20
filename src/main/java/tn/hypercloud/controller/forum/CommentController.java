package tn.hypercloud.controller.forum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.hypercloud.dto.ApiResponse;
import tn.hypercloud.entity.forum.ForumComment;
import tn.hypercloud.service.forum.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            commentService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("Commentaire supprimé avec succès", null));
        } catch (RuntimeException e) {
            log.error("Error deleting comment id: {}", id, e);
            if (e.getMessage() != null && e.getMessage().contains("Accès refusé")) {
                return ResponseEntity.status(403).body(
                        ApiResponse.error("Accès refusé : vous ne pouvez supprimer que vos propres commentaires")
                );
            }
            if (e.getMessage() != null && e.getMessage().contains("non authentifié")) {
                return ResponseEntity.status(401).body(
                        ApiResponse.error("Non authentifié : veuillez vous reconnecter")
                );
            }
            return ResponseEntity.status(400).body(
                    ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Erreur lors de la suppression")
            );
        }
    }
    @GetMapping("/forum/{forumId}")
    public ResponseEntity<List<ForumComment>> getByForum(@PathVariable Long forumId) {
        return ResponseEntity.ok(commentService.getByForum(forumId));
    }

    // 🎤 Voice Comment Upload
    @PostMapping("/forum/{forumId}/voice")
    public ResponseEntity<ForumComment> createVoiceComment(
            @PathVariable Long forumId,
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            log.info("Uploading voice message for forum: {}, file size: {} bytes", 
                    forumId, audioFile.getSize());
            ForumComment voiceComment = commentService.createVoiceComment(forumId, audioFile);
            return ResponseEntity.ok(voiceComment);
        } catch (Exception e) {
            log.error("Error uploading voice comment", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}