package tn.hypercloud.service;



import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.PasswordResetToken;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.payload.request.ForgotPasswordRequest;
import tn.hypercloud.payload.request.LoginRequest;
import tn.hypercloud.payload.request.RegisterRequest;
import tn.hypercloud.payload.request.ResetPasswordRequest;
import tn.hypercloud.payload.response.AuthResponse;
import tn.hypercloud.payload.response.UserResponse;
import tn.hypercloud.repository.user.PasswordResetTokenRepository;
import tn.hypercloud.repository.user.UserRepository;
import tn.hypercloud.security.JwtUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * REGISTER — Inscription
     * Postman : POST /api/auth/register
     * Body    : { "username":"ali", "email":"ali@test.com", "password":"123456", "role":"ADMIN" }
     */
    public AuthResponse register(RegisterRequest request) {

        // 1. Verifier si email deja utilise
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email deja utilise : " + request.getEmail());
        }

        // 2. Verifier si username deja utilise
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username deja utilise : " + request.getUsername());
        }

        // 3. Creer l'utilisateur (password encode avec BCrypt)
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEnabled(true);

        // 4. Sauvegarder en BDD
        User savedUser = userRepository.save(user);

        // 5. Generer le token JWT
        String token = jwtUtils.generateToken(savedUser);

        // 6. Retourner token + infos user
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtils.getExpirationMs());
        response.setUser(UserResponse.fromEntity(savedUser));
        return response;
    }

    /**
     * LOGIN — Connexion
     * Postman : POST /api/auth/login
     * Body    : { "email":"ali@test.com", "password":"123456" }
     */
    public AuthResponse login(LoginRequest request) {

        // 1. Spring Security verifie email + password automatiquement
        //    Si incorrect -> leve une exception automatiquement
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Charger l'utilisateur depuis la BDD
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        // 3. Generer le token JWT
        String token = jwtUtils.generateToken(user);

        // 4. Retourner token + infos user
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtils.getExpirationMs());
        response.setUser(UserResponse.fromEntity(user));
        return response;
    }

    /**
     * DEMANDE DE MOT DE PASSE OUBLIE
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString().replace("-", "");
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

            PasswordResetToken resetToken = passwordResetTokenRepository.findByUser_Id(user.getId())
                    .map(existing -> {
                        existing.setToken(token);
                        existing.setExpiryDate(expiry);
                        return existing;
                    })
                    .orElseGet(() -> PasswordResetToken.builder()
                            .token(token)
                            .user(user)
                            .expiryDate(expiry)
                            .build());

            passwordResetTokenRepository.save(resetToken);
            passwordResetEmailService.sendResetPasswordEmail(user, token);
        });
    }

    /**
     * REINITIALISATION DU MOT DE PASSE
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Token expire");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
    }
}
