package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.service.transport.*;

import java.util.List;
@RestController
@RequestMapping("/hypercloud/vehicules")
@AllArgsConstructor
public class VehiculeController {

    private final IVehiculeService vehiculeService;

    @PostMapping
    public Vehicule addVehicule(@RequestBody Vehicule vehicule) {
        return vehiculeService.addVehicule(vehicule);
    }

    @PutMapping("/{id}")
    public Vehicule updateVehicule(@PathVariable Long id, @RequestBody Vehicule vehicule) {
        vehicule.setIdVehicule(id);
        return vehiculeService.updateVehicule(vehicule);
    }

    @DeleteMapping("/{id}")
    public void deleteVehicule(@PathVariable Long id) {
        vehiculeService.deleteVehicule(id);
    }

    @GetMapping("/{id}")
    public Vehicule getVehiculeById(@PathVariable Long id) {
        return vehiculeService.getVehiculeById(id);
    }

    @GetMapping
    public List<Vehicule> getAllVehicules() {
        return vehiculeService.getAllVehicules();
    }

    @GetMapping("/actifs")
    public List<Vehicule> getActiveVehicules() {
        return vehiculeService.getActiveVehicules();
    }

    @PutMapping("/{id}/activer")
    public Vehicule activateVehicule(@PathVariable Long id) {
        return vehiculeService.activateVehicule(id);
    }

    @PutMapping("/{id}/desactiver")
    public Vehicule deactivateVehicule(@PathVariable Long id) {
        return vehiculeService.deactivateVehicule(id);
    }
}