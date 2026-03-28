package tn.hypercloud.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.user.UserRepository;

@Component("userAuthorizationService")
@RequiredArgsConstructor
public class UserAuthorizationService {

    private final UserRepository userRepository;

    public boolean isSelf(Long targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || targetUserId == null) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User currentUser) {
            return currentUser.getId() != null && currentUser.getId().equals(targetUserId);
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> user.getId() != null && user.getId().equals(targetUserId))
                .orElse(false);
    }
}
