package tn.hypercloud.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.hypercloud.entity.user.User;

public class SecurityUtils {

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        Object principal = authentication.getPrincipal();

        System.out.println("========================================");
        System.out.println("PRINCIPAL TYPE  : " + principal.getClass().getName());
        System.out.println("PRINCIPAL VALUE : " + principal.toString());
        System.out.println("========================================");

        return (User) principal;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static boolean isAdmin() {
        return getCurrentUser().getRole().name().equals("ADMIN");
    }
}