package tn.hypercloud.exception;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tn.hypercloud.payload.response.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepte toutes les exceptions et retourne un JSON propre dans Postman
 * Au lieu d'une page HTML d'erreur, Postman recoit :
 * { "success": false, "message": "Email deja utilise", "data": null }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Erreurs de validation (@NotBlank, @Email, @Size ...)
    // Postman recoit la liste des champs invalides
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        return ResponseEntity.badRequest().body(
                ApiResponse.error("Erreur de validation"));
    }

    // Email/password incorrect au login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error("Email ou mot de passe incorrect"));
    }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<ApiResponse<Void>> handleDisabledAccount(
            DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiResponse.error("Votre compte est en attente de validation par l administrateur"));
        }

    // User non trouve, email deja utilise, etc.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error(ex.getMessage()));
    }

    // Toute autre erreur inattendue
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Erreur serveur: " + ex.getMessage()));
    }
}