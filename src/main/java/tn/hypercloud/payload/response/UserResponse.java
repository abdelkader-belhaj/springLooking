package tn.hypercloud.payload.response;



import lombok.Builder;
import lombok.Data;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;



import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private boolean enabled;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getFullName())
                .email(user.getUsername())
                .enabled(user.isEnabled())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
