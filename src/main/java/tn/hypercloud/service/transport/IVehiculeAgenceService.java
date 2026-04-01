package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.VehiculeAgence;
import java.util.List;

public interface IVehiculeAgenceService {
    VehiculeAgence addVehiculeAgence(VehiculeAgence vehicule);
    VehiculeAgence updateVehiculeAgence(VehiculeAgence vehicule);
    void deleteVehiculeAgence(Long id);
    VehiculeAgence getById(Long id);
    List<VehiculeAgence> getAll();
    List<VehiculeAgence> getByAgence(Long agenceId);
}