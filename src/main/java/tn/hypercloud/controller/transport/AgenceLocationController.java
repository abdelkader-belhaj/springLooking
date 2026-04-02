package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.AgenceLocation;
import tn.hypercloud.service.transport.IAgenceLocationService;

import java.util.List;

@RestController
@RequestMapping("/hypercloud/agences-location")
@RequiredArgsConstructor
public class AgenceLocationController {

    private final IAgenceLocationService agenceService;

    @PostMapping
    public AgenceLocation createAgence(@RequestBody AgenceLocation agence) {
        return agenceService.createAgence(agence);
    }

    @PutMapping("/{id}")
    public AgenceLocation updateAgence(@PathVariable Long id, @RequestBody AgenceLocation agence) {
        agence.setIdAgence(id);
        return agenceService.updateAgence(agence);
    }

    @DeleteMapping("/{id}")
    public void deleteAgence(@PathVariable Long id) {
        agenceService.deleteAgence(id);
    }

    @GetMapping("/{id}")
    public AgenceLocation getAgenceById(@PathVariable Long id) {
        return agenceService.getById(id);
    }

    @GetMapping
    public List<AgenceLocation> getAllAgences() {
        return agenceService.getAllAgences();
    }

    @GetMapping("/actives")
    public List<AgenceLocation> getActiveAgences() {
        return agenceService.getActiveAgences();
    }

    @PutMapping("/{id}/approuver")
    public AgenceLocation approveAgence(@PathVariable Long id) {
        return agenceService.approveAgence(id);
    }

    @PutMapping("/{id}/desactiver")
    public AgenceLocation deactivateAgence(@PathVariable Long id) {
        return agenceService.deactivateAgence(id);
    }
}