package tn.hypercloud.entity.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.Offre;
import tn.hypercloud.repository.reservation.OffreRepository;
import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
@CrossOrigin("*")
public class OffreController {

    private final OffreRepository offreRepo;

    @GetMapping
    public ResponseEntity<List<Offre>> getAll() {
        return ResponseEntity.ok(offreRepo.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SOCIETE')")
    public ResponseEntity<Offre> create(@RequestBody Offre offre) {
        return ResponseEntity.ok(offreRepo.save(offre));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SOCIETE')")
    public ResponseEntity<Offre> update(@PathVariable Integer id, @RequestBody Offre offre) {
        Offre existing = offreRepo.findById(id).orElseThrow();
        existing.setCode(offre.getCode());
        existing.setPourcentage(offre.getPourcentage());
        existing.setDateDebut(offre.getDateDebut());
        existing.setDateFin(offre.getDateFin());
        existing.setActif(offre.getActif());
        return ResponseEntity.ok(offreRepo.save(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SOCIETE')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        offreRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
