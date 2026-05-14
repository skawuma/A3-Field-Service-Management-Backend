package com.a3solutions.fsm.security;

import com.a3solutions.fsm.auth.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final Key key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.secret-file:}") String secretFile,
            @Value("${jwt.expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        String resolvedSecret = resolveSecret(secret, secretFile);
        this.key = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    private String resolveSecret(String secret, String secretFile) {
        String trimmedSecret = secret == null ? "" : secret.trim();
        if (StringUtils.hasText(trimmedSecret)) {
            return validateSecret(trimmedSecret);
        }

        if (StringUtils.hasText(secretFile)) {
            try {
                String fileSecret = Files.readString(Path.of(secretFile.trim()), StandardCharsets.UTF_8).trim();
                return validateSecret(fileSecret);
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to read JWT secret file.", ex);
            }
        }

        throw new IllegalStateException("JWT secret must be provided via JWT_SECRET or JWT_SECRET_FILE.");
    }

    private String validateSecret(String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256 signing.");
        }

        return secret;
    }

    // ======================================================
    // TOKEN GENERATION WITH EXTRA CLAIMS (id, role, names)
    // ======================================================

    public String generateAccessToken(UserEntity user) {
        Map<String, Object> claims = Map.of(
                "type", "access",
                "id", user.getId(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "role", user.getRole().name()
        );

        return generateToken(claims, user.getEmail(), accessExpiration);
    }

    public String generateRefreshToken(UserEntity user) {
        Map<String, Object> claims = Map.of(
                "type", "refresh",
                "id", user.getId(),
                "role", user.getRole().name()
        );

        return generateToken(claims, user.getEmail(), refreshExpiration);
    }

    private String generateToken(Map<String, Object> claims, String subject, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)   // email
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ======================================================
    // VALIDATION
    // ======================================================

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, c -> c.get("type", String.class)));
    }

    public boolean isAccessToken(String token) {
        return "access".equals(extractClaim(token, c -> c.get("type", String.class)));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ======================================================
    // CLAIM EXTRACTION
    // ======================================================

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims,T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
