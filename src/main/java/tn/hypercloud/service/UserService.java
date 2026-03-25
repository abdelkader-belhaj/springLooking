package tn.hypercloud.service;



import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.payload.request.ChangePasswordRequest;
import tn.hypercloud.payload.request.UpdateUserRequest;
import tn.hypercloud.payload.response.UserResponse;
import tn.hypercloud.repository.user.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));
        return UserResponse.fromEntity(user);
    }

    /**
     * UPDATE — Modifier un user
     * Postman : PUT /api/users/1
     * Body    : { "username": "nouveau_nom" }  <- seuls les champs envoyes changent
     */
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));

        // Modifier seulement les champs non null
        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getEmail()    != null) user.setEmail(request.getEmail());
        if (request.getRole()     != null) user.setRole(request.getRole());
        if (request.getEnabled()  != null) user.setEnabled(request.getEnabled());

        return UserResponse.fromEntity(userRepository.save(user));
    }

    /**
     * DELETE — Supprimer un user
     * Postman : DELETE /api/users/1
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User non trouve avec id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * CHANGE PASSWORD — Changer le mot de passe
     * Postman : PATCH /api/users/1/password
     * Body    : { "oldPassword":"123456", "newPassword":"newpass", "confirmPassword":"newpass" }
     */
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));

        // Verifier ancien mot de passe
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        // Verifier confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * CHANGE ROLE — Changer le role d'un user (ADMIN seulement)
     * Postman : PATCH /api/users/1/role?role=HEBERGEUR
     */
    public UserResponse changeRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));
        user.setRole(role);
        return UserResponse.fromEntity(userRepository.save(user));
    }

    /**
     * TOGGLE ENABLED — Activer / desactiver un user
     * Postman : PATCH /api/users/1/toggle
     */
    public UserResponse toggleEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouve avec id: " + id));
        user.setEnabled(!user.isEnabled());
        return UserResponse.fromEntity(userRepository.save(user));
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
}