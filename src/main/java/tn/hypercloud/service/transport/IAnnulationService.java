package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.AnnulationTransport;
import tn.hypercloud.entity.transport.enums.AnnulePar;

import java.util.List;

public interface IAnnulationService {

    // CRUD de base
    AnnulationTransport addAnnulation(AnnulationTransport annulationTransport);
    AnnulationTransport updateAnnulation(AnnulationTransport annulationTransport);
    void deleteAnnulation(Long id);
    AnnulationTransport getAnnulationById(Long id);
    List<AnnulationTransport> getAllAnnulations();
    List<AnnulationTransport> getAnnulationsByType(AnnulePar annulePar);

    /**
     * POINT 5 DU PDF : Annulation avec calcul automatique des pénalités
     * @param courseId     ID de la course à annuler
     * @param annulePar    Qui annule (CLIENT / CHAUFFEUR / SYSTEM)
     * @param raison       Raison textuelle
     * @return L'annulation créée avec pénalité et montant remboursement calculés
     */
    AnnulationTransport annulerCourse(Long courseId, AnnulePar annulePar, String raison);
}
