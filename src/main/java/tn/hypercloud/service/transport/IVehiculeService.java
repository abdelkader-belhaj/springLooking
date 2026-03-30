package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.entity.transport.enums.TypeVehicule;

import java.util.List;
public interface IVehiculeService {
    Vehicule addVehicule(Vehicule vehicule);
    Vehicule updateVehicule(Vehicule vehicule);
    void deleteVehicule(Long id);
    Vehicule getVehiculeById(Long id);
    List<Vehicule> getAllVehicules();
    List<Vehicule> getVehiculesByChauffeur(Chauffeur chauffeur);
    List<Vehicule> getVehiculesByType(TypeVehicule type);
    List<Vehicule> getActiveVehicules();
    Vehicule activateVehicule(Long id);
    Vehicule deactivateVehicule(Long id);
}