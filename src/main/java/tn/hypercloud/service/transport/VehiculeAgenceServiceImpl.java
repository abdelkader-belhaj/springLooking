package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.AgenceLocation;
import tn.hypercloud.entity.transport.VehiculeAgence;
import tn.hypercloud.repository.transport.AgenceLocationRepository;
import tn.hypercloud.repository.transport.VehiculeAgenceRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class VehiculeAgenceServiceImpl implements IVehiculeAgenceService {

    private final VehiculeAgenceRepository repository;
    private final AgenceLocationRepository agenceRepository;

    @Override
    @Transactional
    public VehiculeAgence addVehiculeAgence(VehiculeAgence v) {
        if (repository.existsByNumeroPlaque(v.getNumeroPlaque())) {
            throw new IllegalArgumentException("Ce numéro de plaque existe déjà!");
        }

        if (v.getAgenceId() != null) {
            AgenceLocation agence = agenceRepository.findById(v.getAgenceId())
                    .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
            v.setAgence(agence);
        }

        return repository.save(v);
    }

    @Override
    public VehiculeAgence updateVehiculeAgence(VehiculeAgence v) {
        return repository.save(v);
    }

    @Override
    public void deleteVehiculeAgence(Long id) {
        repository.deleteById(id);
    }

    @Override
    public VehiculeAgence getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<VehiculeAgence> getAll() {
        return repository.findAll();
    }

    @Override
    public List<VehiculeAgence> getByAgence(Long agenceId) {
        return repository.findByAgence_IdAgence(agenceId);
    }
}