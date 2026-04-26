package tn.hypercloud.security;



import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ce filtre s'execute sur CHAQUE requete HTTP
 * Il lit le header : Authorization: Bearer eyJhbGci...
 * Si le token est valide -> il connecte l'utilisateur automatiquement
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
                 String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/uploads/")) {
            filterChain.doFilter(request, response);
            return;
        }
               try {
            // 1. Lire le token depuis le header
            String jwt = parseJwt(request);

            // 2. Si token present et valide
            if (jwt != null && jwtUtils.validateToken(jwt)) {

                // 3. Extraire l'email du token
                String email = jwtUtils.extractUsername(jwt);

                // 4. Charger l'utilisateur depuis la BDD
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 5. Connecter l'utilisateur dans Spring Security
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            System.err.println("Erreur authentification: " + e.getMessage());
        }

        // 6. Continuer la chaine de filtres
        filterChain.doFilter(request, response);
    }

    // Extrait le token du header "Authorization: Bearer <token>"
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}