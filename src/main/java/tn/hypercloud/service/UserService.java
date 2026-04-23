package tn.hypercloud.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.payload.request.ChangePasswordRequest;
import tn.hypercloud.payload.request.UpdateFaceIdRequest;
import tn.hypercloud.payload.request.UpdateUserRequest;
import tn.hypercloud.payload.response.UserResponse;
import tn.hypercloud.payload.response.face.ExtractEmbeddingResponse;
import tn.hypercloud.repository.user.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FaceAiClientService faceAiClientService;
    private final PasswordResetEmailService passwordResetEmailService;
    private final ObjectMapper objectMapper;

    /**
     * GET ALL — Liste tous les users
     * Postman : GET /api/users
     * Header  : Authorization: Bearer <token>
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * GET BY ID — Recuperer un user
     * Postman : GET /api/users/1
     */
    public UserResponse getUserById(Long id) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        User user = userRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));
        return UserResponse.fromEntity(user);
    }

    /**
     * UPDATE — Modifier un user
     * Postman : PUT /api/users/1
     * Body    : { "username": "nouveau_nom" }  <- seuls les champs envoyes changent
     */
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        User user = userRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));

        if (request.getUsername() != null && !request.getUsername().equals(user.getFullName())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username deja utilise : " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email deja utilise : " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        if (request.getProfileImage() != null) {
            user.setProfileImage(request.getProfileImage().trim());
        }

        User userToSave = Objects.requireNonNull(user, "user must not be null");
        return UserResponse.fromEntity(userRepository.save(userToSave));
    }

    /**
     * UPDATE FACE ID — Re-enroler une empreinte faciale
     * Postman : PATCH /api/users/1/face-id
     * Body    : { "imageBase64": "data:image/jpeg;base64,..." }
     */
    public UserResponse updateFaceId(Long id, UpdateFaceIdRequest request) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        User user = userRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));

        ExtractEmbeddingResponse embeddingResponse = faceAiClientService.extractEmbedding(request.getImageBase64());
        user.setFaceEmbedding(toJson(embeddingResponse.getEmbedding()));
        user.setFaceModelName(embeddingResponse.getModelName());
        user.setFaceDetectorBackend(embeddingResponse.getDetectorBackend());

        if (user.getFaceThreshold() == null) {
            user.setFaceThreshold(0.75);
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }

    /**
     * DELETE — Supprimer un user
     * Postman : DELETE /api/users/1
     */
    public void deleteUser(Long id) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        if (!userRepository.existsById(safeId)) {
            throw new RuntimeException("User non trouve avec id: " + id);
        }
        userRepository.deleteById(safeId);
    }

    /**
     * CHANGE PASSWORD — Changer le mot de passe
     * Postman : PATCH /api/users/1/password
     * Body    : { "oldPassword":"123456", "newPassword":"newpass", "confirmPassword":"newpass" }
     */
    public void changePassword(Long id, ChangePasswordRequest request) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        User user = userRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));

        String oldPassword = request.getOldPassword();
        boolean oldPasswordProvided = oldPassword != null && !oldPassword.isBlank();
        boolean oldPasswordRequired = user.isLocalPasswordSet() || oldPasswordProvided;

        // Exiger l'ancien mot de passe pour un changement classique.
        // Pour un compte Google en premier setup, il peut etre absent.
        if (oldPasswordRequired) {
            if (!oldPasswordProvided) {
                throw new RuntimeException("Ancien mot de passe obligatoire");
            }

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("Ancien mot de passe incorrect");
            }
        }

        // Verifier confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLocalPasswordSet(true);
        userRepository.save(user);
    }

    /**
     * CHANGE ROLE — Changer le role d'un user (ADMIN seulement)
     * Postman : PATCH /api/users/1/role?role=HEBERGEUR
     */
    public UserResponse changeRole(Long id, Role role) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        User user = userRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));
        user.setRole(role);
        return UserResponse.fromEntity(userRepository.save(user));
    }

    /**
     * TOGGLE ENABLED — Activer / desactiver un user
     * Postman : PATCH /api/users/1/toggle
     */
    public UserResponse toggleEnabled(Long id) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        User user = userRepository.findById(safeId)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));

        boolean wasEnabled = user.isEnabled();
        user.setEnabled(!wasEnabled);
        User savedUser = userRepository.save(user);

        // Envoyer un email seulement lors de l'acceptation admin (false -> true).
        if (!wasEnabled && savedUser.isEnabled()) {
            try {
                passwordResetEmailService.sendAccountApprovedEmail(savedUser);
            } catch (RuntimeException ex) {
                log.warn("Compte active mais email d'activation non envoye pour userId={}", savedUser.getId(), ex);
            }
        }

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * GET BY ROLE — Filtrer par role
     * Postman : GET /api/users/role/ADMIN
     */
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Impossible de sauvegarder l embedding");
        }
    }
}