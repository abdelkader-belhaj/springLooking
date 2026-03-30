package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.Matching;
import tn.hypercloud.entity.transport.enums.MatchingStatut;

import java.util.List;
public interface IMatchingService {
    Matching addMatching(Matching matching);
    Matching updateMatching(Matching matching);
    void deleteMatching(Long id);
    Matching getMatchingById(Long id);
    List<Matching> getAllMatchings();
    List<Matching> getMatchingsByChauffeur(Chauffeur chauffeur);
    List<Matching> getMatchingsByStatut(MatchingStatut statut);
    Matching acceptMatching(Long id);
    Matching rejectMatching(Long id);
    void proposeMatchingsToAvailableDrivers(DemandeCourse demande);
}
