package tn.hypercloud.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.event.EventCategoryRequest;
import tn.hypercloud.dto.event.EventCategoryResponse;
import tn.hypercloud.service.event.EventCategoryService;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EventCategoryController {

    private final EventCategoryService service;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventCategoryResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<EventCategoryResponse> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventCategoryResponse>> getByType(
            @PathVariable String type) {
        return ResponseEntity.ok(service.getByType(type));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventCategoryResponse> create(
            @RequestBody EventCategoryRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventCategoryResponse> update(
            @PathVariable Integer id,
            @RequestBody EventCategoryRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
