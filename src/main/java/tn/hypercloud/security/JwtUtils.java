package tn.hypercloud.security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Utilitaire JWT :
 *  - generateToken()   : cree un token apres login
 *  - isTokenValid()    : verifie le token dans chaque requete
 *  - extractUsername() : lit l'email depuis le token
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // ---- Generer le token ----
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())        // email
                .setIssuedAt(new Date())                      // date creation
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ---- Valider le token ----
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractUsername(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e)      { System.err.println("Token expire: " + e.getMessage()); }
        catch (MalformedJwtException e)      { System.err.println("Token invalide: " + e.getMessage()); }
        catch (UnsupportedJwtException e)    { System.err.println("Token non supporte: " + e.getMessage()); }
        catch (IllegalArgumentException e)   { System.err.println("Token vide: " + e.getMessage()); }
        return false;
    }

    // ---- Lire les infos du token ----
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public long getExpirationMs() {
        return jwtExpiration;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}