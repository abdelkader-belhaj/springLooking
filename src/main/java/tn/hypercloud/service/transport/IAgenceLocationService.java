package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.AgenceLocation;
import java.util.List;

public interface IAgenceLocationService {

    AgenceLocation createAgence(AgenceLocation agence);
    AgenceLocation updateAgence(AgenceLocation agence);
    void deleteAgence(Long id);
    AgenceLocation getById(Long id);
    List<AgenceLocation> getAllAgences();
    List<AgenceLocation> getActiveAgences();

    // Approbation par l'admin
    AgenceLocation approveAgence(Long id);
    AgenceLocation deactivateAgence(Long id);
}