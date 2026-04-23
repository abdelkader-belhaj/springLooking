package tn.hypercloud.entity.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.Offre;
import tn.hypercloud.entity.reservation.dto.VolRequest;
import tn.hypercloud.entity.reservation.dto.VolResponse;
import tn.hypercloud.entity.reservation.dto.OffreResponse;
import tn.hypercloud.entity.reservation.Vol;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.reservation.VolRepository;
import tn.hypercloud.repository.user.UserRepository;

import tn.hypercloud.entity.reservation.dto.EscaleResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolService {

    private final VolRepository volRepo;
    private final UserRepository userRepo;
    private final tn.hypercloud.repository.reservation.OffreRepository offreRepo;
    private final tn.hypercloud.repository.reservation.ReservationVolRepository resRepo;
    private final EmailService emailService;

    // ==============================
    // CREATE
    // ==============================

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
                .offre(req.getOffreId() != null ? offreRepo.findById(req.getOffreId()).orElse(null) : null)
                .build();

        if (req.getEscales() != null) {
            final Vol finalVol = vol;
            vol.setEscales(req.getEscales().stream()
                .map(er -> tn.hypercloud.entity.reservation.Escale.builder()
                    .ville(er.getVille())
                    .duree(er.getDuree())
                    .vol(finalVol)
                    .build())
                .collect(java.util.stream.Collectors.toList()));
        }

        return toResponse(volRepo.save(vol));
    }

    // ==============================
    // UPDATE sécurisé 🔐
    // ==============================

    public VolResponse update(Integer id, String email, VolRequest req) {

        Vol vol = volRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vol introuvable"));

        if (!vol.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Accès refusé");
        }

        vol.setNumero(req.getNumero());
        vol.setDepart(req.getDepart());
        vol.setArrivee(req.getArrivee());
        vol.setDateDepart(req.getDateDepart());
        vol.setHeureDepart(req.getHeureDepart());
        vol.setPrix(req.getPrix());
        vol.setPlaces(req.getPlaces());
        vol.setOffre(req.getOffreId() != null ? offreRepo.findById(req.getOffreId()).orElse(null) : null);

        if (req.getEscales() != null) {
            vol.getEscales().clear();
            final Vol finalVol = vol;
            vol.getEscales().addAll(req.getEscales().stream()
                .map(er -> tn.hypercloud.entity.reservation.Escale.builder()
                    .ville(er.getVille())
                    .duree(er.getDuree())
                    .vol(finalVol)
                    .build())
                .collect(java.util.stream.Collectors.toList()));
        } else {
            vol.getEscales().clear();
        }

        return toResponse(volRepo.save(vol));
    }

    // ==============================
    // DELETE sécurisé 🔐
    // ==============================

    public void delete(Integer id, String email) {

        Vol vol = volRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vol introuvable"));

        if (!vol.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Accès refusé");
        }

        volRepo.delete(vol);
    }

    public VolResponse updateRetard(Integer id, String email, int minutes) {
        Vol vol = volRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vol introuvable"));

        if (!vol.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Accès refusé");
        }

        vol.setRetard(minutes);
        Vol saved = volRepo.save(vol);

        // Envoyer des emails aux clients impactés (seulement ceux dont la réservation est active)
        resRepo.findByVol(saved).stream()
            .filter(r -> r.getStatutReservation() == tn.hypercloud.entity.reservation.ReservationVol.StatutReservation.active)
            .forEach(res -> {
                emailService.envoyerEmailRetard(res, saved, minutes);
            });

        return toResponse(saved);
    }

    // ==============================
    // GET
    // ==============================

    public VolResponse getById(Integer id) {
        return toResponse(volRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vol introuvable")));
    }

    public List<VolResponse> getAll() {
        return volRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<VolResponse> getByUser(Long userId) {
        return volRepo.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<VolResponse> search(String depart, String arrivee, LocalDate date) {
        return volRepo.findByDepartAndArriveeAndDateDepart(depart, arrivee, date)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==============================
    // MAPPER
    // ==============================

    public VolResponse convertToResponse(Vol v) {
        return toResponse(v);
    }

    public VolResponse toResponse(Vol v) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Offre o = v.getOffre();
        OffreResponse offreRes = null;

        if (o != null && Boolean.TRUE.equals(o.getActif()) && 
            now.isAfter(o.getDateDebut()) && now.isBefore(o.getDateFin())) {
            offreRes = OffreResponse.builder()
                    .id(o.getId())
                    .code(o.getCode())
                    .pourcentage(o.getPourcentage())
                    .actif(o.getActif())
                    .dateDebut(o.getDateDebut())
                    .dateFin(o.getDateFin())
                    .build();
        }

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
                .escales(v.getEscales() != null ? v.getEscales().stream()
                        .map(e -> {
                            EscaleResponse er = new EscaleResponse();
                            er.setId(e.getId());
                            er.setVille(e.getVille());
                            er.setDuree(e.getDuree());
                            return er;
                        }).collect(java.util.stream.Collectors.toList()) : null)
                .offre(offreRes)
                .retard(v.getRetard())
                .build();
    }
}