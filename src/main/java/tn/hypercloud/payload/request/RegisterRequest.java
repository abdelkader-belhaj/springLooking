package tn.hypercloud.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.hypercloud.entity.user.Role;

public class RegisterRequest {

    @NotBlank(message = "Username obligatoire")
    @Size(min = 3, max = 50, message = "Username entre 3 et 50 caracteres")
    private String username;

    @NotBlank(message = "Email obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Mot de passe obligatoire")
    @Size(min = 6, message = "Minimum 6 caracteres")
    private String password;

    @NotNull(message = "Role obligatoire")
    private Role role;

    private String phone;

    public RegisterRequest() {}

    public RegisterRequest(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public RegisterRequest(String username, String email, String password, Role role, String phone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
