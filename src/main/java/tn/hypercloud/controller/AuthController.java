package tn.hypercloud.controller;




import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.payload.request.ForgotPasswordRequest;
import tn.hypercloud.payload.request.LoginRequest;
import tn.hypercloud.payload.request.RegisterRequest;
import tn.hypercloud.payload.request.ResetPasswordRequest;
import tn.hypercloud.payload.response.ApiResponse;
import tn.hypercloud.payload.response.AuthResponse;
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
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse auth = authService.register(request);
        return ResponseEntity.ok(
                ApiResponse.success("Inscription reussie", auth));
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
            @Valid @RequestBody LoginRequest request) {

        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Connexion reussie", auth));
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
}
