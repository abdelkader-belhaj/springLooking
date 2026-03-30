package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.transport.enums.VehiculeStatut;
import tn.hypercloud.repository.transport.VehiculeRepository;

import java.util.List;

/**
 * Implémentation Service Vehicule - Pattern classe
 */
@Service
@AllArgsConstructor
public class VehiculeServiceImpl implements IVehiculeService {

    private final VehiculeRepository vehiculeRepository;

    @Override
    public Vehicule addVehicule(Vehicule vehicule) {
        if (vehiculeRepository.existsByNumeroPlaque(vehicule.getNumeroPlaque())) {
            throw new IllegalArgumentException("Ce numéro de plaque existe déjà!");
        }
        return vehiculeRepository.save(vehicule);
    }

    @Override
    public Vehicule updateVehicule(Vehicule vehicule) {
        return vehiculeRepository.save(vehicule);
    }

    @Override
    public void deleteVehicule(Long id) {
        vehiculeRepository.deleteById(id);
    }

    @Override
    public Vehicule getVehiculeById(Long id) {
        return vehiculeRepository.findById(id).orElse(null);
    }

    @Override
    public List<Vehicule> getAllVehicules() {
        return vehiculeRepository.findAll();
    }

    @Override
    public List<Vehicule> getVehiculesByChauffeur(Chauffeur chauffeur) {
        return vehiculeRepository.findByChauffeur(chauffeur);
    }

    @Override
    public List<Vehicule> getVehiculesByType(TypeVehicule type) {
        return vehiculeRepository.findByTypeVehicule(type);
    }

    @Override
    public List<Vehicule> getActiveVehicules() {
        return vehiculeRepository.findByStatut(VehiculeStatut.ACTIVE);
    }

    @Override
    public Vehicule activateVehicule(Long id) {
        Vehicule vehicule = getVehiculeById(id);
        if (vehicule == null) return null;
        vehicule.setStatut(VehiculeStatut.ACTIVE);
        return vehiculeRepository.save(vehicule);
    }

    @Override
    public Vehicule deactivateVehicule(Long id) {
        Vehicule vehicule = getVehiculeById(id);
        if (vehicule == null) return null;
        vehicule.setStatut(VehiculeStatut.INACTIVE);
        return vehiculeRepository.save(vehicule);
    }
}