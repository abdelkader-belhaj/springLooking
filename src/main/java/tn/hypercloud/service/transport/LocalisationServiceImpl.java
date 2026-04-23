package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.repository.transport.LocalisationRepository;

import java.util.List;
@Service
@AllArgsConstructor
public class LocalisationServiceImpl implements ILocalisationService {
    private final LocalisationRepository localisationRepository;

    @Override
    public Localisation addLocalisation(Localisation localisation) {
        return localisationRepository.save(localisation);
    }

    @Override
    public Localisation updateLocalisation(Localisation localisation) {
        return localisationRepository.save(localisation);
    }

    @Override
    public void deleteLocalisation(Long id) {
        localisationRepository.deleteById(id);
    }

    @Override
    public Localisation getLocalisationById(Long id) {
        return localisationRepository.findById(id).orElse(null);
    }

    @Override
    public List<Localisation> getAllLocalisations() {
        return localisationRepository.findAll();
    }
}
