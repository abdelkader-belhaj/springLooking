package tn.hypercloud.controller.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tn.hypercloud.payload.request.CategorieRequest;
import tn.hypercloud.payload.response.CategorieResponse;
import tn.hypercloud.service.accommodation.CategorieService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService service;

    // CREATE CATEGORIE (admin + hebergeur)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<CategorieResponse> create(
            @RequestBody CategorieRequest req) {

        return ResponseEntity.ok(service.create(req));
    }

    // GET ALL CATEGORIES (PUBLIC)
    @GetMapping
    public ResponseEntity<List<CategorieResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // GET ACTIVE CATEGORIES (PUBLIC)
    @GetMapping("/actives")
    public ResponseEntity<List<CategorieResponse>> getActives() {
        return ResponseEntity.ok(service.getActives());
    }

    // GET BY ID (PUBLIC)
    @GetMapping("/{id}")
    public ResponseEntity<CategorieResponse> getById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(service.getById(id));
    }

    // UPDATE (ADMIN + hébergeur sur ses catégories)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<CategorieResponse> update(
            @PathVariable Integer id,
            @RequestBody CategorieRequest req) {

        return ResponseEntity.ok(service.update(id, req));
    }

    // DELETE (ADMIN + hébergeur sur ses catégories)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {

        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}