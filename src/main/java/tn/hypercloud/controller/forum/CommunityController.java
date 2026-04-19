package tn.hypercloud.controller.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.forum.Community;
import tn.hypercloud.service.forum.CommunityService;

import java.util.List;
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping
    public ResponseEntity<Community> create(@RequestBody Community community) {
        return ResponseEntity.ok(communityService.create(community));
    }

    @GetMapping
    public ResponseEntity<List<Community>> getAll() {
        return ResponseEntity.ok(communityService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Community> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Community> update(
            @PathVariable Long id,
            @RequestBody Community community) {
        return ResponseEntity.ok(communityService.update(id, community));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        communityService.delete(id);
        return ResponseEntity.ok("Community supprimée");
    }
}