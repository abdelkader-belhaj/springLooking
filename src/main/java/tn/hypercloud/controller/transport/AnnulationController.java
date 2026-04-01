package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.AnnulationTransport;
import tn.hypercloud.entity.transport.enums.AnnulePar;
import tn.hypercloud.service.transport.*;

import java.util.List;
@RestController
@RequestMapping("/hypercloud/annulations")
@AllArgsConstructor
public class AnnulationController {

    private final IAnnulationService annulationService;

    @PostMapping
    public AnnulationTransport addAnnulation(@RequestBody AnnulationTransport annulationTransport) {
        return annulationService.addAnnulation(annulationTransport);
    }

    @PutMapping("/{id}")
    public AnnulationTransport updateAnnulation(@PathVariable Long id, @RequestBody AnnulationTransport annulationTransport) {
        annulationTransport.setIdAnnulation(id);
        return annulationService.updateAnnulation(annulationTransport);
    }

    @DeleteMapping("/{id}")
    public void deleteAnnulation(@PathVariable Long id) {
        annulationService.deleteAnnulation(id);
    }

    @GetMapping("/{id}")
    public AnnulationTransport getAnnulationById(@PathVariable Long id) {
        return annulationService.getAnnulationById(id);
    }

    @GetMapping
    public List<AnnulationTransport> getAllAnnulations() {
        return annulationService.getAllAnnulations();
    }
    // Dans AnnulationController.java
    @PutMapping("/course/{courseId}/annuler")
    public AnnulationTransport annulerCourse(
            @PathVariable Long courseId,
            @RequestParam AnnulePar annulePar,
            @RequestParam(required = false) String raison) {

        return annulationService.annulerCourse(courseId, annulePar, raison);
    }
}