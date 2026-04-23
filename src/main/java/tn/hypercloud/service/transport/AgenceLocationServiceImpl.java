package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.AgenceLocation;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.AgenceLocationRepository;
import tn.hypercloud.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenceLocationServiceImpl implements IAgenceLocationService {

    private final AgenceLocationRepository agenceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AgenceLocation createAgence(AgenceLocation agence) {
        User user = userRepository.findById(agence.getUtilisateur().getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Protection contre role null
        if (user.getRole() == null || !user.getRole().equals(tn.hypercloud.entity.user.Role.TRANSPORTEUR)) {
            throw new IllegalStateException("Seul un utilisateur avec rôle TRANSPORTEUR peut créer une agence");
        }

        return agenceRepository.save(agence);
    }

    @Override
    @Transactional
    public AgenceLocation updateAgence(AgenceLocation agence) {
        return agenceRepository.save(agence);
    }

    @Override
    public void deleteAgence(Long id) {
        agenceRepository.deleteById(id);
    }

    @Override
    public AgenceLocation getById(Long id) {
        return agenceRepository.findById(id).orElse(null);
    }

    @Override
    public List<AgenceLocation> getAllAgences() {
        return agenceRepository.findAll();
    }

    @Override
    public List<AgenceLocation> getActiveAgences() {
        return agenceRepository.findByStatut(true);
    }

    @Override
    @Transactional
    public AgenceLocation approveAgence(Long id) {
        AgenceLocation agence = getById(id);
        if (agence == null) throw new RuntimeException("Agence non trouvée");
        agence.setStatut(true);
        return agenceRepository.save(agence);
    }

    @Override
    @Transactional
    public AgenceLocation deactivateAgence(Long id) {
        AgenceLocation agence = getById(id);
        if (agence == null) throw new RuntimeException("Agence non trouvée");
        agence.setStatut(false);
        return agenceRepository.save(agence);
    }

    @Override
    @Transactional(readOnly = true)
    public AgenceLocation getAgenceByUtilisateurId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId ne peut pas être nul");
        }

        return agenceRepository.findByUtilisateur_Id(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Agence non trouvée pour l'utilisateur " + userId
                ));
    }
}