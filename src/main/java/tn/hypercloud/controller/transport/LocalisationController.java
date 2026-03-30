package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.service.transport.*;

import java.util.List;
@RestController
@RequestMapping("/hypercloud/localisations")
@AllArgsConstructor
public class LocalisationController {

    private final ILocalisationService localisationService;

    @PostMapping
    public Localisation addLocalisation(@RequestBody Localisation localisation) {
        return localisationService.addLocalisation(localisation);
    }

    @PutMapping("/{id}")
    public Localisation updateLocalisation(@PathVariable Long id, @RequestBody Localisation localisation) {
        localisation.setIdLocalisation(id);
        return localisationService.updateLocalisation(localisation);
    }

    @DeleteMapping("/{id}")
    public void deleteLocalisation(@PathVariable Long id) {
        localisationService.deleteLocalisation(id);
    }

    @GetMapping("/{id}")
    public Localisation getLocalisationById(@PathVariable Long id) {
        return localisationService.getLocalisationById(id);
    }

    @GetMapping
    public List<Localisation> getAllLocalisations() {
        return localisationService.getAllLocalisations();
    }
}
