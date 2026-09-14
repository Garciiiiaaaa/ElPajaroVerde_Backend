package es.elpajaroverde.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private volatile SecretKey signingKey;
    private final long expirationMillis;

    public JwtUtil(@Value("${app.jwt.secret:}") String secret,
                   @Value("${app.jwt.expiration-hours:8}") long expirationHours) {
        this.signingKey = resolveKey(secret);
        this.expirationMillis = expirationHours * 3600 * 1000;
    }

    public String generateToken(String nombreUsuario) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, nombreUsuario);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void rotateKey() {
        this.signingKey = Jwts.SIG.HS256.key().build();
    }

    private String createToken(Map<String, Object> claims, String nombreUsuario) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(nombreUsuario)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey resolveKey(String secret) {
        if (secret != null && !secret.isBlank()) {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        }
        return Jwts.SIG.HS256.key().build();
    }
}
