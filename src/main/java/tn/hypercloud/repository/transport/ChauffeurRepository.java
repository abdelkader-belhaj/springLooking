package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.enums.ChauffeurStatut;
import tn.hypercloud.entity.transport.enums.DisponibiliteStatut;
import tn.hypercloud.entity.user.User;

import java.util.List;
import java.util.Optional;

public interface ChauffeurRepository extends JpaRepository<Chauffeur, Long> {
    Optional<Chauffeur> findByNumeroLicence(String numeroLicence);
    Optional<Chauffeur> findByUtilisateur(User utilisateur);
    List<Chauffeur> findByStatut(ChauffeurStatut statut);
    List<Chauffeur> findByDisponibilite(DisponibiliteStatut disponibilite);
    boolean existsByNumeroLicence(String numeroLicence);
    List<Chauffeur> findByDisponibiliteAndStatut(DisponibiliteStatut disponibilite, ChauffeurStatut statut);
    Optional<Chauffeur> findByUtilisateur_Id(Long userId);
}
