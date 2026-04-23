package tn.hypercloud.controller.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.event.EventActivityRequest;
import tn.hypercloud.dto.event.EventActivityResponse;
import tn.hypercloud.dto.event.EventCancelRequest;
import tn.hypercloud.dto.event.EventRejectRequest;
import tn.hypercloud.dto.event.EventReviewRequest;
import tn.hypercloud.dto.event.EventReviewResponse;
import tn.hypercloud.dto.event.EventStatsResponse;
import tn.hypercloud.service.event.EventActivityService;
import tn.hypercloud.service.event.EventReviewService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EventActivityController {

    private final EventActivityService service;
    private final EventReviewService reviewService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ORGANISATEUR','ADMIN')")
    public ResponseEntity<List<EventActivityResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /** GET /api/events/all — Pour ADMIN uniquement (tous les événements). */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventActivityResponse>> getAllForAdmin() {
        return ResponseEntity.ok(service.getAll());
    }

    /** Catalogue (Angular) : événements publiés uniquement. */
    @GetMapping("/published")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventActivityResponse>> getPublished() {
        return ResponseEntity.ok(service.getPublished());
    }

    /** Fiche événement publique (sans fuite des brouillons / rejetés). */
    @GetMapping("/published/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<EventActivityResponse> getPublishedById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.getPublishedById(id));
    }

    @GetMapping("/{id}/reviews")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventReviewResponse>> getReviews(
            @PathVariable Integer id) {
        return ResponseEntity.ok(reviewService.getByEvent(id));
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<EventReviewResponse> submitReview(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventReviewRequest request) {
        return ResponseEntity.ok(reviewService.submit(id, request, userDetails.getUsername()));
    }

    @GetMapping("/mes-events")
    @PreAuthorize("hasRole('ORGANISATEUR')")
    public ResponseEntity<List<EventActivityResponse>> getMesEvents(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                service.getMesEvents(userDetails.getUsername()));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventActivityResponse>> getByType(
            @PathVariable String type) {
        return ResponseEntity.ok(service.getPublishedByType(type));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ORGANISATEUR','ADMIN')")
    public ResponseEntity<List<EventActivityResponse>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @GetMapping("/city/{city}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventActivityResponse>> getByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(service.getPublishedByCity(city));
    }

    @GetMapping("/check-date")
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<Map<String, Object>> checkDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.checkDateAvailability(date));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<EventActivityResponse>> getByCategoryId(
            @PathVariable Integer categoryId) {
        return ResponseEntity.ok(service.getPublishedByCategoryId(categoryId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventActivityResponse> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANISATEUR')")
    public ResponseEntity<EventActivityResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventActivityRequest request) {
        return ResponseEntity.ok(
                service.create(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANISATEUR')")
    public ResponseEntity<EventActivityResponse> update(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventActivityRequest request) {
        return ResponseEntity.ok(
                service.update(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventActivityResponse> publish(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.publish(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventActivityResponse> reject(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventRejectRequest request) {
        return ResponseEntity.ok(service.reject(id, userDetails.getUsername(), request.getReason()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventActivityResponse> cancel(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) EventCancelRequest request) {
        return ResponseEntity.ok(
                service.cancel(id, userDetails.getUsername(), request != null ? request.getReason() : null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventStatsResponse> getStats() {
        return ResponseEntity.ok(service.getStats());
    }
}
