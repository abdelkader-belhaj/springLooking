package tn.hypercloud.payload.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.hypercloud.entity.user.Role;
@Data
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
}
