package tn.hypercloud.entity.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.dto.VolRequest;
import tn.hypercloud.entity.reservation.dto.VolResponse;
import tn.hypercloud.entity.reservation.Vol;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.reservation.VolRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolService {

    private final VolRepository volRepo;
    private final UserRepository userRepo;
    // SocieteRepository supprimé — plus nécessaire

    public VolResponse create(String email, VolRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        Vol vol = Vol.builder()
                .user(user)
                .numero(req.getNumero())
                .depart(req.getDepart())
                .arrivee(req.getArrivee())
                .dateDepart(req.getDateDepart())
                .heureDepart(req.getHeureDepart())
                .prix(req.getPrix())
                .places(req.getPlaces())
                .build();
        return toResponse(volRepo.save(vol));
    }

    public VolResponse update(Integer id, VolRequest req) {
        Vol vol = volRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vol introuvable"));
        vol.setNumero(req.getNumero());
        vol.setDepart(req.getDepart());
        vol.setArrivee(req.getArrivee());
        vol.setDateDepart(req.getDateDepart());
        vol.setHeureDepart(req.getHeureDepart());
        vol.setPrix(req.getPrix());
        vol.setPlaces(req.getPlaces());
        return toResponse(volRepo.save(vol));
    }

    public void delete(Integer id) {
        volRepo.deleteById(id);
    }

    public VolResponse getById(Integer id) {
        return toResponse(volRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vol introuvable")));
    }

    public List<VolResponse> getAll() {
        return volRepo.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Remplace getBySociete — maintenant par userId
    public List<VolResponse> getByUser(Long userId) {
        return volRepo.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<VolResponse> search(String depart, String arrivee, LocalDate date) {
        return volRepo.findByDepartAndArriveeAndDateDepart(depart, arrivee, date)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public VolResponse toResponse(Vol v) {
        return VolResponse.builder()
                .id(v.getId())
                .societeNom(v.getUser().getUsername())
                .numero(v.getNumero())
                .depart(v.getDepart())
                .arrivee(v.getArrivee())
                .dateDepart(v.getDateDepart())
                .heureDepart(v.getHeureDepart())
                .prix(v.getPrix())
                .places(v.getPlaces())
                .build();
    }
}