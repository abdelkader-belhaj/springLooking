package tn.hypercloud.entity.reservation.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.dto.VolRequest;
import tn.hypercloud.entity.reservation.dto.VolResponse;
import tn.hypercloud.entity.reservation.Societe;
import tn.hypercloud.entity.reservation.Vol;
import tn.hypercloud.repository.reservation.SocieteRepository;
import tn.hypercloud.repository.reservation.VolRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolService {

    private final VolRepository volRepo;
    private final SocieteRepository societeRepo;

    // ---- CRUD SOCIETE ----

    public VolResponse create(VolRequest req) {
        Societe societe = societeRepo.findById(req.getSocieteId())
                .orElseThrow(() -> new RuntimeException("Société introuvable"));
        Vol vol = Vol.builder()
                .societe(societe)
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
        return volRepo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<VolResponse> getBySociete(Integer societeId) {
        return volRepo.findBySocieteId(societeId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ---- RECHERCHE CLIENT ----
    public List<VolResponse> search(String depart, String arrivee, LocalDate date) {
        return volRepo.findByDepartAndArriveeAndDateDepart(depart, arrivee, date)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ---- MAPPER ----
    public VolResponse toResponse(Vol v) {
        return VolResponse.builder()
                .id(v.getId())
                .societeNom(v.getSociete().getNom())
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