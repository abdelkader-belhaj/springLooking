package tn.hypercloud.controller;




import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.payload.request.FaceLoginRequest;
import tn.hypercloud.payload.request.FaceRegisterRequest;
import tn.hypercloud.payload.request.ForgotPasswordRequest;
import tn.hypercloud.payload.request.GoogleLoginRequest;
import tn.hypercloud.payload.request.LoginRequest;
import tn.hypercloud.payload.request.RegisterRequest;
import tn.hypercloud.payload.request.ResetPasswordRequest;
import tn.hypercloud.payload.request.TwoFactorCodeRequest;
import tn.hypercloud.payload.response.ApiResponse;
import tn.hypercloud.payload.response.AuthResponse;
import tn.hypercloud.payload.response.TwoFactorSetupResponse;
import tn.hypercloud.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * REGISTER
     * Postman : POST http://localhost:8080/api/auth/register
     * Body (JSON) :
     * {
     *   "username": "ali",
     *   "email": "ali@test.com",
     *   "password": "123456",
     *   "role": "ADMIN"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse auth = authService.register(request, httpRequest);
        String message = auth.getUser() != null && !auth.getUser().isEnabled()
            ? "Compte cree en attente de validation admin"
            : "Inscription reussie";
        return ResponseEntity.ok(
            ApiResponse.success(message, auth));
    }

    /**
     * LOGIN
     * Postman : POST http://localhost:8080/api/auth/login
     * Body (JSON) :
     * {
     *   "email": "ali@test.com",
     *   "password": "123456"
     * }
     * -> Recopier le token recu dans Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse auth = authService.login(request, httpRequest);
        return ResponseEntity.ok(
                ApiResponse.success("Connexion reussie", auth));
    }

    @PostMapping("/login-google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse auth = authService.loginWithGoogle(request, httpRequest);
        return ResponseEntity.ok(
                ApiResponse.success("Connexion Google reussie", auth));
    }

    @PostMapping("/register-face")
    public ResponseEntity<ApiResponse<AuthResponse>> registerWithFace(
            @Valid @RequestBody FaceRegisterRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse auth = authService.registerWithFace(request, httpRequest);
        String message = auth.getUser() != null && !auth.getUser().isEnabled()
            ? "Compte Face ID cree en attente de validation admin"
            : "Inscription Face ID reussie";
        return ResponseEntity.ok(
            ApiResponse.success(message, auth));
    }

    @PostMapping("/login-face")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithFace(
            @Valid @RequestBody FaceLoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse auth = authService.loginWithFace(request, httpRequest);
        return ResponseEntity.ok(
                ApiResponse.success("Connexion Face ID reussie", auth));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(
                ApiResponse.success("Deconnexion reussie", null));
    }

        /**
         * DEMANDE DE REINITIALISATION
         * Postman : POST http://localhost:8080/api/auth/forgot-password
         * Body (JSON) : { "email": "ali@test.com" }
         */
        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
            ApiResponse.success("Si cet email existe, un lien de reinitialisation a ete envoye", null));
        }

        /**
         * REINITIALISATION DU MOT DE PASSE
         * Postman : POST http://localhost:8080/api/auth/reset-password
         * Body (JSON) :
         * {
         *   "token": "...",
         *   "newPassword": "newpass123",
         *   "confirmPassword": "newpass123"
         * }
         */
        @PostMapping("/reset-password")
        public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
            ApiResponse.success("Mot de passe reinitialise", null));
        }

        @PostMapping("/2fa/setup")
        public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setupTwoFactor() {
            return ResponseEntity.ok(
                    ApiResponse.success("Configuration 2FA generee", authService.setupTwoFactor()));
        }

        @PostMapping("/2fa/verify")
        public ResponseEntity<ApiResponse<tn.hypercloud.payload.response.UserResponse>> verifyTwoFactor(
                @Valid @RequestBody TwoFactorCodeRequest request) {
            return ResponseEntity.ok(
                    ApiResponse.success("2FA active avec succes", authService.verifyTwoFactor(request)));
        }
}
