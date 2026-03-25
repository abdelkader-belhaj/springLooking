package tn.hypercloud.payload.response;



import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Long expiresIn;

    private UserResponse user;
}
