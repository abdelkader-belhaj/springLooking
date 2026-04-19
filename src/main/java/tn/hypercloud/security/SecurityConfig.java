package tn.hypercloud.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth

                        // ── PUBLIC ──────────────────────────────
                        .requestMatchers("/api/auth/**", "/api/smart-access/**")
                        .permitAll()

                        // GET public — voir sans token
                        .requestMatchers(HttpMethod.GET,
                                "/api/categories/**",
                                "/api/logements/**")
                        .permitAll()

                        // ── ADMIN ou HEBERGEUR (catégories) ──────
                        .requestMatchers(HttpMethod.POST,
                                "/api/categories/**")
                        .hasAnyRole("ADMIN","HEBERGEUR")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/categories/**")
                        .hasAnyRole("ADMIN","HEBERGEUR")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/categories/**")
                        .hasAnyRole("ADMIN","HEBERGEUR")

                        // ── ADMIN ou HEBERGEUR ───────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/logements/**")
                        .hasAnyRole("ADMIN","HEBERGEUR")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/logements/**")
                        .hasAnyRole("ADMIN","HEBERGEUR")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/logements/**")
                        .hasAnyRole("ADMIN","HEBERGEUR")

                        // ── AUTHENTIFIÉ obligatoire ──────────────
                        .requestMatchers("/api/reservations/**")
                        .authenticated()
                        .requestMatchers("/api/avis/**")
                        .authenticated()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // ── Tout le reste ────────────────────────
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(
                List.of("http://localhost:*"));
        config.setAllowedMethods(List.of(
                "GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization","Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}