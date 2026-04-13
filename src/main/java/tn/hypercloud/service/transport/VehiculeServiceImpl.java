package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.transport.enums.VehiculeStatut;
import tn.hypercloud.repository.transport.VehiculeRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implémentation Service Vehicule - Pattern classe
 */
@Service
@AllArgsConstructor
public class VehiculeServiceImpl implements IVehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private static final Path VEHICULE_UPLOAD_DIR = Path.of("uploads", "vehicules");
    private static final Logger LOGGER = LoggerFactory.getLogger(VehiculeServiceImpl.class);

    @Override
    public Vehicule addVehicule(Vehicule vehicule) {
        LOGGER.info("[Vehicule][Add] request numeroPlaque={}, chauffeurId={}",
                vehicule.getNumeroPlaque(),
                vehicule.getChauffeur() != null ? vehicule.getChauffeur().getIdChauffeur() : null);
        if (vehiculeRepository.existsByNumeroPlaque(vehicule.getNumeroPlaque())) {
            throw new IllegalArgumentException("Ce numéro de plaque existe déjà!");
        }

        Long chauffeurId = vehicule.getChauffeur() != null ? vehicule.getChauffeur().getIdChauffeur() : null;
        if (chauffeurId == null) {
            throw new IllegalArgumentException("Le chauffeur est obligatoire pour ajouter un véhicule");
        }

        boolean hasActiveVehicule = !vehiculeRepository
                .findByChauffeur_IdChauffeurAndStatut(chauffeurId, VehiculeStatut.ACTIVE)
                .isEmpty();

        if (vehicule.getStatut() == null) {
            vehicule.setStatut(hasActiveVehicule ? VehiculeStatut.INACTIVE : VehiculeStatut.ACTIVE);
        } else if (vehicule.getStatut() == VehiculeStatut.ACTIVE) {
            deactivateOtherVehicules(chauffeurId, null);
        }

        Vehicule saved = vehiculeRepository.save(vehicule);
        LOGGER.info("[Vehicule][Add] saved idVehicule={}, numeroPlaque={}",
                saved.getIdVehicule(),
                saved.getNumeroPlaque());
        return saved;
    }

    @Override
    public Vehicule updateVehicule(Vehicule vehicule) {
        Vehicule saved = vehiculeRepository.save(vehicule);
        if (saved.getStatut() == VehiculeStatut.ACTIVE
                && saved.getChauffeur() != null
                && saved.getChauffeur().getIdChauffeur() != null) {
            deactivateOtherVehicules(saved.getChauffeur().getIdChauffeur(), saved.getIdVehicule());
        }
        return saved;
    }

    @Override
    public void deleteVehicule(Long id) {
        Vehicule vehicule = getVehiculeById(id);
        if (vehicule != null && vehicule.getPhotoUrls() != null) {
            for (String photoPath : vehicule.getPhotoUrls()) {
                deletePhotoFileIfExists(normalizePhotoPath(photoPath));
            }
        }

        vehiculeRepository.deleteById(id);

        try {
            Path vehiculeDir = VEHICULE_UPLOAD_DIR.resolve(String.valueOf(id)).toAbsolutePath().normalize();
            if (Files.exists(vehiculeDir) && Files.isDirectory(vehiculeDir)) {
                try (var paths = Files.list(vehiculeDir)) {
                    paths.forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
                }
                Files.deleteIfExists(vehiculeDir);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public Vehicule getVehiculeById(Long id) {
        return vehiculeRepository.findById(id).orElse(null);
    }

    @Override
    public List<Vehicule> getAllVehicules() {
        return vehiculeRepository.findAll();
    }

    @Override
    public List<Vehicule> getVehiculesByChauffeurId(Long chauffeurId) {
        return vehiculeRepository.findByChauffeur_IdChauffeur(chauffeurId);
    }

    @Override
    public List<Vehicule> getVehiculesByChauffeur(Chauffeur chauffeur) {
        return vehiculeRepository.findByChauffeur(chauffeur);
    }

    @Override
    public List<Vehicule> getVehiculesByType(TypeVehicule type) {
        return vehiculeRepository.findByTypeVehicule(type);
    }

    @Override
    public List<Vehicule> getActiveVehicules() {
        return vehiculeRepository.findByStatut(VehiculeStatut.ACTIVE);
    }

    @Override
    public Vehicule activateVehicule(Long id) {
        Vehicule vehicule = getVehiculeById(id);
        if (vehicule == null) return null;

        if (vehicule.getChauffeur() == null || vehicule.getChauffeur().getIdChauffeur() == null) {
            throw new IllegalArgumentException("Le véhicule n'est pas associé à un chauffeur");
        }

        deactivateOtherVehicules(vehicule.getChauffeur().getIdChauffeur(), vehicule.getIdVehicule());
        vehicule.setStatut(VehiculeStatut.ACTIVE);
        return vehiculeRepository.save(vehicule);
    }

    @Override
    public Vehicule deactivateVehicule(Long id) {
        Vehicule vehicule = getVehiculeById(id);
        if (vehicule == null) return null;
        vehicule.setStatut(VehiculeStatut.INACTIVE);
        return vehiculeRepository.save(vehicule);
    }

    @Override
    public Vehicule uploadVehiculePhotos(Long id, List<MultipartFile> files) {
        LOGGER.info("[Vehicule][Photos] upload start idVehicule={}, filesCount={}",
                id,
                files != null ? files.size() : 0);
        Vehicule vehicule = getVehiculeById(id);
        if (vehicule == null) {
            throw new IllegalArgumentException("Vehicule introuvable: " + id);
        }

        if (files == null || files.isEmpty()) {
            return vehicule;
        }

        Path vehiculeDir = VEHICULE_UPLOAD_DIR.resolve(String.valueOf(id));
        List<String> existingPhotos = vehicule.getPhotoUrls() != null
                ? new ArrayList<>(vehicule.getPhotoUrls())
                : new ArrayList<>();

        try {
            Files.createDirectories(vehiculeDir);

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    throw new IllegalArgumentException("Seuls les fichiers image sont autorises");
                }

                String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo";
                String ext = getExtension(originalName);
                String filename = UUID.randomUUID() + ext;
                Path target = vehiculeDir.resolve(filename).normalize();

                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                String relativePath = ("vehicules/" + id + "/" + filename).replace("\\", "/");
                existingPhotos.add(relativePath);
                LOGGER.info("[Vehicule][Photos] stored file idVehicule={}, originalName={}, relativePath={}",
                        id,
                        originalName,
                        relativePath);
            }

            vehicule.setPhotoUrls(existingPhotos);
            vehicule.setPhotoUrlsSerialized(String.join("||", existingPhotos));
            vehicule.setDateModification(LocalDateTime.now());
            Vehicule saved = vehiculeRepository.save(vehicule);
            LOGGER.info("[Vehicule][Photos] persisted idVehicule={}, serializedPhotos={}",
                    id,
                    saved.getPhotoUrlsSerialized());
            return saved;
        } catch (IOException e) {
            LOGGER.error("[Vehicule][Photos] upload error idVehicule={}", id, e);
            throw new RuntimeException("Erreur lors de l'upload des photos du vehicule", e);
        }
    }

    @Override
    public Vehicule removeVehiculePhoto(Long id, String photoUrl) {
        Vehicule vehicule = getVehiculeById(id);
        if (vehicule == null) {
            throw new IllegalArgumentException("Vehicule introuvable: " + id);
        }

        String normalizedTarget = normalizePhotoPath(photoUrl);
        List<String> currentPhotos = vehicule.getPhotoUrls() != null
                ? new ArrayList<>(vehicule.getPhotoUrls())
                : new ArrayList<>();

        boolean removed = currentPhotos.removeIf(path ->
                normalizePhotoPath(path).equals(normalizedTarget)
        );

        if (!removed) {
            throw new IllegalArgumentException("Photo introuvable pour ce vehicule");
        }

        deletePhotoFileIfExists(normalizedTarget);

        vehicule.setPhotoUrls(currentPhotos);
        vehicule.setPhotoUrlsSerialized(
                currentPhotos.isEmpty() ? null : String.join("||", currentPhotos)
        );
        vehicule.setDateModification(LocalDateTime.now());

        return vehiculeRepository.save(vehicule);
    }

    private String normalizePhotoPath(String path) {
        if (path == null) {
            return "";
        }

        String normalized = path.replace("\\", "/").trim();
        if (normalized.startsWith("uploads/")) {
            return normalized.substring("uploads/".length());
        }
        return normalized;
    }

    private void deletePhotoFileIfExists(String normalizedPath) {
        if (normalizedPath.isBlank()) {
            return;
        }

        try {
            Path uploadsRoot = Path.of("uploads").toAbsolutePath().normalize();
            Path target = uploadsRoot.resolve(normalizedPath).normalize();

            if (!target.startsWith(uploadsRoot)) {
                throw new IllegalArgumentException("Chemin de photo invalide");
            }

            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Suppression du fichier photo impossible", e);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return ".jpg";
        }
        return filename.substring(dotIndex).toLowerCase();
    }

    private void deactivateOtherVehicules(Long chauffeurId, Long vehiculeToKeepActiveId) {
        List<Vehicule> vehicules = vehiculeRepository.findByChauffeur_IdChauffeur(chauffeurId);
        for (Vehicule other : vehicules) {
            boolean shouldKeepActive = vehiculeToKeepActiveId != null
                    && vehiculeToKeepActiveId.equals(other.getIdVehicule());
            if (shouldKeepActive) {
                continue;
            }

            if (other.getStatut() == VehiculeStatut.ACTIVE) {
                other.setStatut(VehiculeStatut.INACTIVE);
                vehiculeRepository.save(other);
            }
        }
    }
}