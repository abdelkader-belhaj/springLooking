package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.enums.DisponibiliteStatut;

import java.util.List;

/**
 * Interface Service Chauffeur - Pattern classe
 */
public interface IChauffeurService {

    // ========== CRUD DE BASE ==========
    Chauffeur addChauffeur(Chauffeur chauffeur);

    Chauffeur updateChauffeur(Chauffeur chauffeur);

    void deleteChauffeur(Long id);

    Chauffeur getChauffeurById(Long id);

    List<Chauffeur> getAllChauffeurs();

    // ========== RECHERCHES SPÉCIFIQUES ==========
    List<Chauffeur> getAvailableChauffeurs();

    List<Chauffeur> getActiveChauffeurs();

    // ========== CHANGEMENTS DE STATUT ==========
    Chauffeur updateDisponibilite(Long id, DisponibiliteStatut disponibilite);

    Chauffeur approveChauffeur(Long id);

    Chauffeur suspendChauffeur(Long id);

    // ========== GESTION ONLINE/OFFLINE ==========
    Chauffeur goOnline(Long id);

    Chauffeur goOffline(Long id);

    Chauffeur setOnRide(Long id);

    // ========== ASSOCIATIONS ==========
    Chauffeur affecterChauffeurAVehicule(Long idChauffeur, Long idVehicule);
}