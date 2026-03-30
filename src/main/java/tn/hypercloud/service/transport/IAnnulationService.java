package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Annulation;
import tn.hypercloud.entity.transport.enums.AnnulePar;

import java.util.List;

public interface IAnnulationService {

    // CRUD de base
    Annulation addAnnulation(Annulation annulation);
    Annulation updateAnnulation(Annulation annulation);
    void deleteAnnulation(Long id);
    Annulation getAnnulationById(Long id);
    List<Annulation> getAllAnnulations();
    List<Annulation> getAnnulationsByType(AnnulePar annulePar);

    /**
     * POINT 5 DU PDF : Annulation avec calcul automatique des pénalités
     * @param courseId     ID de la course à annuler
     * @param annulePar    Qui annule (CLIENT / CHAUFFEUR / SYSTEM)
     * @param raison       Raison textuelle
     * @return L'annulation créée avec pénalité et montant remboursement calculés
     */
    Annulation annulerCourse(Long courseId, AnnulePar annulePar, String raison);
}
