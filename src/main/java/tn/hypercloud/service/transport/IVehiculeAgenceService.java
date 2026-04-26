package tn.hypercloud.service.transport;

import org.springframework.web.multipart.MultipartFile;
import tn.hypercloud.entity.transport.VehiculeAgence;
import java.util.List;

public interface IVehiculeAgenceService {
    VehiculeAgence addVehiculeAgence(VehiculeAgence vehicule);
    VehiculeAgence updateVehiculeAgence(VehiculeAgence vehicule);
    void deleteVehiculeAgence(Long id);
    VehiculeAgence getById(Long id);
    List<VehiculeAgence> getAll();
    List<VehiculeAgence> getByAgence(Long agenceId);
    VehiculeAgence uploadVehiculeAgencePhotos(Long id, List<MultipartFile> files);
    VehiculeAgence removeVehiculeAgencePhoto(Long id, String photoUrl);
}