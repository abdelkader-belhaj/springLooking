package tn.hypercloud.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;


import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactiver CSRF: l'application utilise un flux hybride JWT + session
                .csrf(AbstractHttpConfigurer::disable)
                //Session :
                //Login==>Serveur crée une session et la stocke en mémoire==>Navigateur reçoit un cookie
                //JWT(JSON Web Token)
                //Login==>Serveur génère un token et l'envoie au client ==> Client stocke le token
                // Autoriser les appels front (Angular) vers l'API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth
                        // Routes publiques — pas besoin de token
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/login-google",
                                "/api/auth/register",
                                "/api/auth/login-face",
                                "/api/auth/register-face",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/categories",
                                "/api/categories/**",
                                "/api/logements",
                                "/api/logements/public",
                                "/api/logements/categorie/**",
                                "/api/logements/{id}"

                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/forums/**").permitAll()
                        .requestMatchers("/api/forums/**").permitAll()
                        .requestMatchers("/api/communities/**").permitAll()
                        .requestMatchers("/api/comments/**").permitAll()
                        .requestMatchers("/api/reactions/**").permitAll()
                        .requestMatchers("/api/reviews/**").permitAll()
                        .requestMatchers("/api/moderation/**").permitAll()
                        .requestMatchers("/api/ai/**").permitAll()
                        .requestMatchers("/api/auth/2fa/**", "/api/auth/logout").authenticated()


                        // ✅ Catalogue événements public (sans login)
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/published",
                                "/api/events/published/*",
                                "/api/events/type/**",
                                "/api/events/city/**",
                                "/api/events/category/**",
                                "/api/events/weather/**",
                                "/api/events/*/weather",
                                "/api/categories",
                                "/api/categories/**"
                        ).permitAll()

                        // Routes admin uniquement
                        //Token ==> accès routes normales Mais : accès routes admin " YES "
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/ws-transport/**").permitAll()
                        .requestMatchers("/hypercloud/uploads/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()


                        // Toutes les autres routes -> token obligatoire
                        //Token ==> accès routes normales Mais : accès routes admin " NO "
                        .anyRequest().authenticated()
                )

                // Activer une session serveur par utilisateur
                .sessionManagement(session ->
                        session
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false)
                                .sessionRegistry(sessionRegistry()))

                .authenticationProvider(authenticationProvider())

                // Ajouter notre filtre JWT avant le filtre Spring par defaut
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt hash le mot de passe automatiquement
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Accepter localhost sur n'importe quel port en développement
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}