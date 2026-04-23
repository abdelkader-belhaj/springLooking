package tn.hypercloud.controller.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.forum.Review;
import tn.hypercloud.service.forum.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/forum/{forumId}")
    public ResponseEntity<Review> create(
            @PathVariable Long forumId,
            @RequestBody Review review) {
        return ResponseEntity.ok(reviewService.create(forumId, review));
    }

    @GetMapping
    public ResponseEntity<List<Review>> getAll() {
        return ResponseEntity.ok(reviewService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> update(
            @PathVariable Long id,
            @RequestBody Review review) {
        return ResponseEntity.ok(reviewService.update(id, review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.ok("Review supprimé avec succès");
    }
    @GetMapping("/forum/{forumId}")
    public ResponseEntity<List<Review>> getByForum(@PathVariable Long forumId) {
        return ResponseEntity.ok(reviewService.getByForum(forumId));
    }
}