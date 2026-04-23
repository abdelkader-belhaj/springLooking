package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.hypercloud.entity.transport.VehiculeAgence;
import tn.hypercloud.service.transport.IVehiculeAgenceService;

import java.util.List;

@RestController
@RequestMapping("/hypercloud/vehicules-agence")
@RequiredArgsConstructor
public class VehiculeAgenceController {

    private final IVehiculeAgenceService service;

    @PostMapping
    public VehiculeAgence create(@RequestBody VehiculeAgence vehicule) {
        return service.addVehiculeAgence(vehicule);
    }

    @GetMapping("/agence/{agenceId}")
    public List<VehiculeAgence> getByAgence(@PathVariable Long agenceId) {
        return service.getByAgence(agenceId);
    }

    @GetMapping
    public List<VehiculeAgence> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public VehiculeAgence getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public VehiculeAgence update(@PathVariable Long id, @RequestBody VehiculeAgence vehicule) {
        vehicule.setIdVehiculeAgence(id);
        return service.updateVehiculeAgence(vehicule);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteVehiculeAgence(id);
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VehiculeAgence uploadVehiculeAgencePhotos(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return service.uploadVehiculeAgencePhotos(id, files);
    }

    @DeleteMapping("/{id}/photos")
    public VehiculeAgence removeVehiculeAgencePhoto(
            @PathVariable Long id,
            @RequestParam("photoUrl") String photoUrl
    ) {
        return service.removeVehiculeAgencePhoto(id, photoUrl);
    }
}