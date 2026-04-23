package tn.hypercloud.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import tn.hypercloud.entity.user.Role;

public class UpdateUserRequest {

    @Size(min = 3, max = 50)
    private String username;

    @Email(message = "Format email invalide")
    private String email;

    private Role role;
    private Boolean enabled;
    private String phone;
    private String bio;
    private String profileImage;

    public UpdateUserRequest() {}

    public UpdateUserRequest(String username, String email, Role role, Boolean enabled) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
