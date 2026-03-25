package tn.hypercloud.payload.request;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Ancien mot de passe obligatoire")
    private String oldPassword;

    @NotBlank(message = "Nouveau mot de passe obligatoire")
    @Size(min = 6, message = "Minimum 6 caracteres")
    private String newPassword;

    @NotBlank(message = "Confirmation obligatoire")
    private String confirmPassword;
}
