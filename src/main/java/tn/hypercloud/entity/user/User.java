package tn.hypercloud.entity.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
    @Column(nullable = false)
    private Role role;

    
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

  

    // Champs optionnels pour eCommerce (artisan)
    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image", length = 500)
    private String profileImage;





// ============================================
    //  Methodes du diagramme de classe
    // ============================================

    public Role getRole() {
        return role;
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
