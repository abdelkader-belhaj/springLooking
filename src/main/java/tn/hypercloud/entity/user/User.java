package tn.hypercloud.entity.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "google_sub", unique = true, length = 100)
    private String googleSub;

    @Column(name = "face_embedding", columnDefinition = "LONGTEXT")
    private String faceEmbedding;

    @Column(name = "face_model_name", length = 50)
    private String faceModelName;

    @Column(name = "face_detector_backend", length = 50)
    private String faceDetectorBackend;

    @Column(name = "face_threshold")
    private Double faceThreshold;

    @Builder.Default
    @Column(name = "two_factor_enabled", columnDefinition = "boolean default false")
    private Boolean twoFactorEnabled = false;

    public boolean isTwoFactorEnabled() {
        return Boolean.TRUE.equals(this.twoFactorEnabled);
    }

    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;

    @Column(name = "two_factor_activated_at")
    private LocalDateTime twoFactorActivatedAt;

    @Builder.Default
    @Column(name = "local_password_set", columnDefinition = "boolean default false")
    private Boolean localPasswordSet = false;

    public boolean isLocalPasswordSet() {
        return Boolean.TRUE.equals(this.localPasswordSet);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFullName() {
        return username;
    }

    // ============================================
    //  UserDetails - Spring Security
    // ============================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired()     { return true; }
    @Override
    public boolean isAccountNonLocked()      { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // ============================================
    //  Cycle de vie JPA
    // ============================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}