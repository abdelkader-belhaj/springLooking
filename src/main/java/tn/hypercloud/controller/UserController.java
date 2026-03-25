package tn.esprit.m_user.controller;



import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.m_user.entity.Role;
import tn.esprit.m_user.payload.request.ChangePasswordRequest;
import tn.esprit.m_user.payload.request.UpdateUserRequest;
import tn.esprit.m_user.payload.response.ApiResponse;
import tn.esprit.m_user.payload.response.UserResponse;
import tn.esprit.m_user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET ALL USERS
     * Postman : GET http://localhost:8080/api/users
     * Header  : Authorization: Bearer <token>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success("Liste des users", userService.getAllUsers()));
    }

    /**
     * GET USER BY ID
     * Postman : GET http://localhost:8080/api/users/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("User trouve", userService.getUserById(id)));
    }

    /**
     * UPDATE USER
     * Postman : PUT http://localhost:8080/api/users/1
     * Body    : { "username": "nouveau_nom" }
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("User modifie", userService.updateUser(id, request)));
    }

    /**
     * DELETE USER  (ADMIN seulement)
     * Postman : DELETE http://localhost:8080/api/users/1
     * Header  : Authorization: Bearer <token_admin>
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User supprime", null));
    }

    /**
     * CHANGE PASSWORD
     * Postman : PATCH http://localhost:8080/api/users/1/password
     * Body    : { "oldPassword":"123456", "newPassword":"newpass", "confirmPassword":"newpass" }
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Mot de passe modifie", null));
    }

    /**
     * CHANGE ROLE  (ADMIN seulement)
     * Postman : PATCH http://localhost:8080/api/users/1/role?role=HEBERGEUR
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        return ResponseEntity.ok(
                ApiResponse.success("Role modifie", userService.changeRole(id, role)));
    }

    /**
     * TOGGLE ENABLED — Activer / desactiver
     * Postman : PATCH http://localhost:8080/api/users/1/toggle
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleEnabled(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Statut modifie", userService.toggleEnabled(id)));
    }

    /**
     * GET BY ROLE
     * Postman : GET http://localhost:8080/api/users/role/ADMIN
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(
            @PathVariable Role role) {
        return ResponseEntity.ok(
                ApiResponse.success("Users par role", userService.getUsersByRole(role)));
    }
}