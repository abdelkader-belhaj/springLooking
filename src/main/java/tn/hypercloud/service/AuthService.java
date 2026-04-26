package tn.hypercloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.user.PasswordResetToken;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.payload.request.FaceLoginRequest;
import tn.hypercloud.payload.request.FaceRegisterRequest;
import tn.hypercloud.payload.request.ForgotPasswordRequest;
import tn.hypercloud.payload.request.GoogleLoginRequest;
import tn.hypercloud.payload.request.LoginRequest;
import tn.hypercloud.payload.request.RegisterRequest;
import tn.hypercloud.payload.request.ResetPasswordRequest;
import tn.hypercloud.payload.request.TwoFactorCodeRequest;
import tn.hypercloud.payload.response.AuthResponse;
import tn.hypercloud.payload.response.TwoFactorSetupResponse;
import tn.hypercloud.payload.response.face.ExtractEmbeddingResponse;
import tn.hypercloud.payload.response.face.VerifyFaceResponse;
import tn.hypercloud.payload.response.UserResponse;
import tn.hypercloud.repository.user.PasswordResetTokenRepository;
import tn.hypercloud.repository.user.UserRepository;
import tn.hypercloud.security.JwtUtils;
import tn.hypercloud.service.event.PhoneNumberUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final FaceAiClientService faceAiClientService;
    private final TwoFactorService twoFactorService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ObjectMapper objectMapper;

    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email deja utilise : " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username deja utilise : " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(normalizePhoneOrThrow(request.getPhone()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setLocalPasswordSet(true);
        user.setRole(request.getRole());
        user.setEnabled(!requiresAdminApproval(request.getRole()));

        User savedUser = userRepository.save(user);

        if (!savedUser.isEnabled()) {
            return buildPendingApprovalResponse(savedUser);
        }

        String token = jwtUtils.generateToken(savedUser);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtils.getExpirationMs());
        response.setUser(UserResponse.fromEntity(savedUser));
        openSessionForUser(savedUser, httpRequest);
        return response;
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException ex) {
            throw new DisabledException("Votre compte est en attente de validation par l administrateur");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        if (!canUserLogin(user)) {
            throw new DisabledException("Votre compte est en attente de validation par l administrateur");
        }

        String token = jwtUtils.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtils.getExpirationMs());
        response.setUser(UserResponse.fromEntity(user));
        openSessionForUser(user, httpRequest);
        return response;
    }

    public AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        var payload = googleTokenVerifierService.verify(request.getIdToken());

        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email Google introuvable dans le token");
        }

        String displayName = (String) payload.get("name");
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, displayName));

        if (!canUserLogin(user)) {
            throw new DisabledException("Votre compte est en attente de validation par l administrateur");
        }

        String token = jwtUtils.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtils.getExpirationMs());
        response.setUser(UserResponse.fromEntity(user));
        openSessionForUser(user, httpRequest);
        return response;
    }

    public AuthResponse registerWithFace(FaceRegisterRequest request, HttpServletRequest httpRequest) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email deja utilise : " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username deja utilise : " + request.getUsername());
        }

        ExtractEmbeddingResponse embeddingResponse = faceAiClientService.extractEmbedding(request.getImageBase64());
        String embeddingJson = toJson(embeddingResponse.getEmbedding());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(normalizePhoneOrThrow(request.getPhone()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setLocalPasswordSet(true);
        user.setRole(request.getRole());
        user.setEnabled(!requiresAdminApproval(request.getRole()));
        user.setFaceEmbedding(embeddingJson);
        user.setFaceModelName(embeddingResponse.getModelName());
        user.setFaceDetectorBackend(embeddingResponse.getDetectorBackend());
        user.setFaceThreshold(0.75);

        User savedUser = userRepository.save(user);

        if (!savedUser.isEnabled()) {
            return buildPendingApprovalResponse(savedUser);
        }

        String token = jwtUtils.generateToken(savedUser);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtils.getExpirationMs());
        response.setUser(UserResponse.fromEntity(savedUser));
        openSessionForUser(savedUser, httpRequest);
        return response;
    }

    public AuthResponse loginWithFace(FaceLoginRequest request, HttpServletRequest httpRequest) {

        User user = findByLogin(request.getLogin())
                .orElseThrow(() -> new RuntimeException("Login invalide"));

        if (!canUserLogin(user)) {
            throw new DisabledException("Votre compte est en attente de validation par l administrateur");
        }

        if (user.getFaceEmbedding() == null || user.getFaceEmbedding().isBlank()) {
            throw new RuntimeException("Aucune empreinte faciale enregistree pour cet utilisateur");
        }

        List<Double> enrolledEmbedding = fromJson(user.getFaceEmbedding());
        double threshold = request.getThreshold() != null
                ? request.getThreshold()
                : (user.getFaceThreshold() != null ? user.getFaceThreshold() : 0.75);

        String detectorBackend = user.getFaceDetectorBackend() != null ? user.getFaceDetectorBackend() : "opencv";
        String modelName = user.getFaceModelName() != null ? user.getFaceModelName() : "SFace";

        VerifyFaceResponse verifyResponse = faceAiClientService.verifyFace(
                request.getImageBase64(),
                enrolledEmbedding,
                threshold,
                detectorBackend,
                modelName
        );

        if (!Boolean.TRUE.equals(verifyResponse.getMatched())) {
            throw new RuntimeException("Authentification faciale invalide");
        }

        String token = jwtUtils.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtils.getExpirationMs());
        response.setUser(UserResponse.fromEntity(user));
        openSessionForUser(user, httpRequest);
        return response;
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    public TwoFactorSetupResponse setupTwoFactor() {
        User user = requireCurrentUser();

        if (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isBlank()) {
            user.setTwoFactorSecret(twoFactorService.generateSecret());
            userRepository.save(user);
        }

        return twoFactorService.buildSetupResponse(user.getEmail(), user.getTwoFactorSecret(), user.isTwoFactorEnabled());
    }

    public UserResponse verifyTwoFactor(TwoFactorCodeRequest request) {
        User user = requireCurrentUser();

        if (user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isBlank()) {
            throw new RuntimeException("Aucun secret 2FA n est disponible pour ce compte");
        }

        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), request.getCode())) {
            throw new RuntimeException("Code 2FA invalide");
        }

        user.setTwoFactorEnabled(true);
        user.setTwoFactorActivatedAt(LocalDateTime.now());
        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

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

    private Optional<User> findByLogin(String login) {
        return userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login));
    }

    private String normalizePhoneOrThrow(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String normalized = PhoneNumberUtil.normalizeForTwilio(rawPhone);
        if (normalized == null) {
            throw new RuntimeException("Numero de telephone invalide. Utilisez un format comme +216XXXXXXXX.");
        }
        return normalized;
    }

    private User createGoogleUser(String email, String displayName) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(buildUniqueUsername(email, displayName));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.CLIENT_TOURISTE);
        user.setEnabled(true);
        user.setLocalPasswordSet(false);
        return userRepository.save(user);
    }

    private String buildUniqueUsername(String email, String displayName) {
        String rawBase = (displayName != null && !displayName.isBlank()) ? displayName : email;
        String base = rawBase
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();

        if (base.isBlank()) base = "googleuser";
        if (base.length() > 20) base = base.substring(0, 20);

        String candidate = base;
        int attempt = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffix = String.valueOf(attempt++);
            int maxBaseLength = Math.max(1, 20 - suffix.length());
            candidate = base.substring(0, Math.min(base.length(), maxBaseLength)) + suffix;
        }
        return candidate;
    }

    private boolean requiresAdminApproval(Role role) {
        return role != Role.CLIENT_TOURISTE;
    }

    private boolean canUserLogin(User user) {
        return !requiresAdminApproval(user.getRole()) || user.isEnabled();
    }

    private AuthResponse buildPendingApprovalResponse(User user) {
        AuthResponse response = new AuthResponse();
        response.setUser(UserResponse.fromEntity(user));
        return response;
    }

    private void openSessionForUser(User user, HttpServletRequest request) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("role", user.getRole().name());
    }

    private User requireCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new RuntimeException("Utilisateur non connecte");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));
    }

    private String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Impossible de sauvegarder l embedding");
        }
    }

    private List<Double> fromJson(String embeddingJson) {
        try {
            return objectMapper.readValue(embeddingJson, new TypeReference<List<Double>>() {});
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Empreinte faciale invalide en base");
        }
    }
}