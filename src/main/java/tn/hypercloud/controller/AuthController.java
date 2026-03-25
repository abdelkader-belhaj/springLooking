package tn.esprit.m_user.controller;



import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.m_user.payload.request.LoginRequest;
import tn.esprit.m_user.payload.request.RegisterRequest;
import tn.esprit.m_user.payload.response.ApiResponse;
import tn.esprit.m_user.payload.response.AuthResponse;
import tn.esprit.m_user.service.AuthService;

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
}
