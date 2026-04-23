package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.entity.transport.enums.ChauffeurStatut;
import tn.hypercloud.entity.transport.enums.DisponibiliteStatut;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.ChauffeurRepository;
import tn.hypercloud.repository.transport.VehiculeRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation Service Chauffeur - Pattern classe
 */
@Service
@AllArgsConstructor
public class ChauffeurServiceImpl implements IChauffeurService {

    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final UserRepository userRepository;
    // ========== CRUD DE BASE ==========
    @Override
    public Chauffeur addChauffeur(Chauffeur chauffeur) {
        // Check license uniqueness
        if (chauffeurRepository.existsByNumeroLicence(chauffeur.getNumeroLicence())) {
            throw new IllegalArgumentException("Ce numéro de licence existe déjà!");
        }

        // If the transient ID is provided, fetch the user
        if (chauffeur.getUtilisateurId() != null) {
            User user = userRepository.findById(chauffeur.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec id: " + chauffeur.getUtilisateurId()));
            chauffeur.setUtilisateur(user);
        } else {
            throw new IllegalArgumentException("L'ID de l'utilisateur est obligatoire");
        }

        return chauffeurRepository.save(chauffeur);
    }
    @Override
    public Chauffeur updateChauffeur(Chauffeur chauffeur) {
        return chauffeurRepository.save(chauffeur);
    }

    @Override
    public void deleteChauffeur(Long id) {
        chauffeurRepository.deleteById(id);
    }

    @Override
    public Chauffeur getChauffeurById(Long id) {
        Chauffeur chauffeur = chauffeurRepository.findById(id).orElse(null);
        return enrichChauffeurIdentity(chauffeur);
    }

    @Override
    public List<Chauffeur> getAllChauffeurs() {
        return chauffeurRepository.findAll().stream()
                .map(this::enrichChauffeurIdentity)
                .collect(Collectors.toList());
    }

    // ========== RECHERCHES SPÉCIFIQUES ==========
    @Override
    public List<Chauffeur> getAvailableChauffeurs() {
        return chauffeurRepository.findByDisponibilite(DisponibiliteStatut.AVAILABLE)
                .stream()
                .map(this::enrichChauffeurIdentity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chauffeur> getActiveChauffeurs() {
        return chauffeurRepository.findByStatut(ChauffeurStatut.ACTIVE)
                .stream()
                .map(this::enrichChauffeurIdentity)
                .collect(Collectors.toList());
    }

    // ========== CHANGEMENTS DE STATUT ==========
    @Override
    public Chauffeur updateDisponibilite(Long id, DisponibiliteStatut disponibilite) {
        Chauffeur chauffeur = getChauffeurById(id);
        if (chauffeur == null) {
            return null;
        }
        chauffeur.setDisponibilite(disponibilite);
        return chauffeurRepository.save(chauffeur);
    }

    @Override
    public Chauffeur approveChauffeur(Long id) {
        Chauffeur chauffeur = getChauffeurById(id);
        if (chauffeur == null) {
            return null;
        }
        chauffeur.setStatut(ChauffeurStatut.ACTIVE);
        return chauffeurRepository.save(chauffeur);
    }

    @Override
    public Chauffeur suspendChauffeur(Long id) {
        Chauffeur chauffeur = getChauffeurById(id);
        if (chauffeur == null) {
            return null;
        }
        chauffeur.setStatut(ChauffeurStatut.SUSPENDED);
        return chauffeurRepository.save(chauffeur);
    }

    // ========== GESTION ONLINE/OFFLINE ==========
    @Override
    public Chauffeur goOnline(Long id) {
        return updateDisponibilite(id, DisponibiliteStatut.AVAILABLE);
    }

    @Override
    public Chauffeur goOffline(Long id) {
        return updateDisponibilite(id, DisponibiliteStatut.UNAVAILABLE);
    }

    @Override
    public Chauffeur setOnRide(Long id) {
        return updateDisponibilite(id, DisponibiliteStatut.ON_RIDE);
    }

    // ========== ASSOCIATIONS ==========
    @Override
    public Chauffeur affecterChauffeurAVehicule(Long idChauffeur, Long idVehicule) {
        Chauffeur chauffeur = chauffeurRepository.findById(idChauffeur).orElse(null);
        Vehicule vehicule = vehiculeRepository.findById(idVehicule).orElse(null);

        if (chauffeur == null || vehicule == null) {
            return null;
        }

        vehicule.setChauffeur(chauffeur);
        vehiculeRepository.save(vehicule);
        return chauffeur;
    }
    @Override
    public Chauffeur updatePosition(Long idChauffeur, Localisation position) {
        Chauffeur chauffeur = getChauffeurById(idChauffeur);
        if (chauffeur == null) {
            throw new RuntimeException("Chauffeur non trouvé");
        }

        // Mettre a jour l'entite existante evite de recreer une localisation a chaque ping.
        Localisation current = chauffeur.getPositionActuelle();
        if (current == null) {
            current = Localisation.builder()
                    .latitude(position.getLatitude())
                    .longitude(position.getLongitude())
                    .adresse(position.getAdresse())
                    .build();
            chauffeur.setPositionActuelle(current);
        } else {
            current.setLatitude(position.getLatitude());
            current.setLongitude(position.getLongitude());
            current.setAdresse(position.getAdresse());
        }

        return chauffeurRepository.save(chauffeur);
    }
    @Override
    public Chauffeur save(Chauffeur chauffeur) {
        return chauffeurRepository.save(chauffeur);
    }
    @Override
    public Chauffeur getChauffeurByUtilisateurId(Long userId) {
        Chauffeur c = chauffeurRepository.findByUtilisateur_Id(userId)
                .orElseThrow(() -> new RuntimeException("Chauffeur introuvable pour userId=" + userId));

        return enrichChauffeurIdentity(c);
    }

    private Chauffeur enrichChauffeurIdentity(Chauffeur chauffeur) {
        if (chauffeur == null) {
            return null;
        }

        if (chauffeur.getUtilisateur() != null) {
            chauffeur.setUtilisateurId(chauffeur.getUtilisateur().getId());
            chauffeur.setNomAffichage(chauffeur.getUtilisateur().getUsername());
            chauffeur.setEmailAffichage(chauffeur.getUtilisateur().getEmail());
        }

        return chauffeur;
    }
}