package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.Annulation;
import tn.hypercloud.entity.transport.enums.AnnulePar;
import tn.hypercloud.service.transport.*;

import java.util.List;
@RestController
@RequestMapping("/hypercloud/annulations")
@AllArgsConstructor
public class AnnulationController {

    private final IAnnulationService annulationService;

    @PostMapping
    public Annulation addAnnulation(@RequestBody Annulation annulation) {
        return annulationService.addAnnulation(annulation);
    }

    @PutMapping("/{id}")
    public Annulation updateAnnulation(@PathVariable Long id, @RequestBody Annulation annulation) {
        annulation.setIdAnnulation(id);
        return annulationService.updateAnnulation(annulation);
    }

    @DeleteMapping("/{id}")
    public void deleteAnnulation(@PathVariable Long id) {
        annulationService.deleteAnnulation(id);
    }

    @GetMapping("/{id}")
    public Annulation getAnnulationById(@PathVariable Long id) {
        return annulationService.getAnnulationById(id);
    }

    @GetMapping
    public List<Annulation> getAllAnnulations() {
        return annulationService.getAllAnnulations();
    }
    // Dans AnnulationController.java
    @PutMapping("/course/{courseId}/annuler")
    public Annulation annulerCourse(
            @PathVariable Long courseId,
            @RequestParam AnnulePar annulePar,
            @RequestParam(required = false) String raison) {

        return annulationService.annulerCourse(courseId, annulePar, raison);
    }
}