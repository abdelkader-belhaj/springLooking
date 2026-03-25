package tn.hypercloud.payload.request;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.hypercloud.entity.user.Role;


@Data
public class UpdateUserRequest {

    @Size(min = 3, max = 50)
    private String username;

    @Email(message = "Format email invalide")
    private String email;

    private Role role;

    private Boolean enabled;
}
