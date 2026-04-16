package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.hypercloud.entity.transport.AgenceLocation;
import tn.hypercloud.entity.transport.VehiculeAgence;
import tn.hypercloud.repository.transport.AgenceLocationRepository;
import tn.hypercloud.repository.transport.VehiculeAgenceRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VehiculeAgenceServiceImpl implements IVehiculeAgenceService {

    private final VehiculeAgenceRepository repository;
    private final AgenceLocationRepository agenceRepository;
    private static final Path VEHICULE_AGENCE_UPLOAD_DIR = Path.of("uploads", "vehicules-agence");
    private static final Logger LOGGER = LoggerFactory.getLogger(VehiculeAgenceServiceImpl.class);

    @Override
    @Transactional
    public VehiculeAgence addVehiculeAgence(VehiculeAgence v) {
        if (repository.existsByNumeroPlaque(v.getNumeroPlaque())) {
            throw new IllegalArgumentException("Ce numéro de plaque existe déjà!");
        }

        if (v.getAgenceId() != null) {
            AgenceLocation agence = agenceRepository.findById(v.getAgenceId())
                    .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
            v.setAgence(agence);
        }

        return repository.save(v);
    }

    @Override
    @Transactional
    public VehiculeAgence updateVehiculeAgence(VehiculeAgence input) {
        VehiculeAgence existing = repository.findById(input.getIdVehiculeAgence())
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé"));

        if (input.getAgenceId() != null) {
            AgenceLocation agence = agenceRepository.findById(input.getAgenceId())
                    .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
            existing.setAgence(agence);
        } else if (input.getAgence() != null && input.getAgence().getIdAgence() != null) {
            AgenceLocation agence = agenceRepository.findById(input.getAgence().getIdAgence())
                    .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
            existing.setAgence(agence);
        }
// sinon: on garde existing.getAgence() tel quel

        existing.setMarque(input.getMarque());
        existing.setModele(input.getModele());
        existing.setNumeroPlaque(input.getNumeroPlaque());
        existing.setTypeVehicule(input.getTypeVehicule());
        existing.setCapacitePassagers(input.getCapacitePassagers());
        existing.setPrixKm(input.getPrixKm());
        existing.setPrixMinute(input.getPrixMinute());
        existing.setPrixVehicule(input.getPrixVehicule());
        existing.setStatut(input.getStatut());

        return repository.save(existing);
    }
    @Override
    public void deleteVehiculeAgence(Long id) {
        VehiculeAgence vehicule = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Véhicule introuvable"));

        if (vehicule.getPhotoUrls() != null) {
            for (String photoPath : vehicule.getPhotoUrls()) {
                try {
                    deletePhotoFileIfExists(normalizePhotoPath(photoPath));
                } catch (RuntimeException ex) {
                    LOGGER.warn("[VehiculeAgence][Delete] photo cleanup skipped idVehiculeAgence={}, photoPath={}", id, photoPath, ex);
                }
            }
        }

        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Impossible de supprimer ce véhicule car il est lié à des réservations. Désactivez-le à la place.");
        }

        try {
            Path vehiculeDir = VEHICULE_AGENCE_UPLOAD_DIR.resolve(String.valueOf(id)).toAbsolutePath().normalize();
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
    public VehiculeAgence getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<VehiculeAgence> getAll() {
        return repository.findAll();
    }

    @Override
    public List<VehiculeAgence> getByAgence(Long agenceId) {
        return repository.findByAgence_IdAgence(agenceId);
    }

    @Override
    @Transactional
    public VehiculeAgence uploadVehiculeAgencePhotos(Long id, List<MultipartFile> files) {
        LOGGER.info("[VehiculeAgence][Photos] upload start idVehiculeAgence={}, filesCount={}",
                id,
                files != null ? files.size() : 0);

        VehiculeAgence vehicule = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicule agence introuvable: " + id));

        if (files == null || files.isEmpty()) {
            return vehicule;
        }

        Path vehiculeDir = VEHICULE_AGENCE_UPLOAD_DIR.resolve(String.valueOf(id));
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

                String relativePath = ("vehicules-agence/" + id + "/" + filename).replace("\\", "/");
                existingPhotos.add(relativePath);
                LOGGER.info("[VehiculeAgence][Photos] stored file idVehiculeAgence={}, originalName={}, relativePath={}",
                        id,
                        originalName,
                        relativePath);
            }

            vehicule.setPhotoUrls(existingPhotos);
            vehicule.setPhotoUrlsSerialized(String.join("||", existingPhotos));
            vehicule.setDateModification(LocalDateTime.now());

            VehiculeAgence saved = repository.save(vehicule);
            LOGGER.info("[VehiculeAgence][Photos] persisted idVehiculeAgence={}, serializedPhotos={}",
                    id,
                    saved.getPhotoUrlsSerialized());
            return saved;
        } catch (IOException e) {
            LOGGER.error("[VehiculeAgence][Photos] upload error idVehiculeAgence={}", id, e);
            throw new RuntimeException("Erreur lors de l'upload des photos du vehicule agence", e);
        }
    }

    @Override
    @Transactional
    public VehiculeAgence removeVehiculeAgencePhoto(Long id, String photoUrl) {
        VehiculeAgence vehicule = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicule agence introuvable: " + id));

        String normalizedTarget = normalizePhotoPath(photoUrl);
        List<String> currentPhotos = vehicule.getPhotoUrls() != null
                ? new ArrayList<>(vehicule.getPhotoUrls())
                : new ArrayList<>();

        boolean removed = currentPhotos.removeIf(path ->
                normalizePhotoPath(path).equals(normalizedTarget)
        );

        if (!removed) {
            throw new IllegalArgumentException("Photo introuvable pour ce vehicule agence");
        }

        deletePhotoFileIfExists(normalizedTarget);

        vehicule.setPhotoUrls(currentPhotos);
        vehicule.setPhotoUrlsSerialized(
                currentPhotos.isEmpty() ? null : String.join("||", currentPhotos)
        );
        vehicule.setDateModification(LocalDateTime.now());

        return repository.save(vehicule);
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

        if (!isSafeRelativePath(normalizedPath)) {
            throw new IllegalArgumentException("Chemin de photo invalide");
        }

        try {
            Path uploadsRoot = Path.of("uploads").toAbsolutePath().normalize();
            Path target = uploadsRoot.resolve(normalizedPath).normalize();

            if (!target.startsWith(uploadsRoot)) {
                throw new IllegalArgumentException("Chemin de photo invalide");
            }

            Files.deleteIfExists(target);
        } catch (Exception e) {
            throw new RuntimeException("Suppression du fichier photo impossible", e);
        }
    }

    private boolean isSafeRelativePath(String path) {
        if (path.contains("..") || path.contains(":") || path.startsWith("/") || path.startsWith("\\")) {
            return false;
        }

        return !path.matches(".*[<>\"|?*].*");
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return ".jpg";
        }
        return filename.substring(dotIndex).toLowerCase();
    }
}