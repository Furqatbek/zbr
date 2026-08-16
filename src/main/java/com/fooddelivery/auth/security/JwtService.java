package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token generation and validation.
 */
@Service
@Slf4j
public class JwtService {

    /**
     * The signing key that used to be committed to the repository as a
     * ${JWT_SECRET:...} default. It is now permanently banned — if it ever
     * appears (i.e. the env var was not set), the application must refuse to start.
     */
    private static final String BANNED_COMMITTED_SECRET = "K8mX2sP4vQ9wE1rT6yU0iO3aS7dF5gH2jL4zN6bV8cM0";

    /** HS256 needs at least 256 bits of key material. */
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    /**
     * Fail fast at startup if the JWT secret is missing, too weak, or the
     * old committed default. A forgeable signing key is a full-compromise
     * vector, so we refuse to boot rather than run insecurely.
     */
    @PostConstruct
    void validateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Provide a strong, unique secret via the JWT_SECRET " +
                    "environment variable (at least " + MIN_SECRET_LENGTH + " characters). Refusing to start.");
        }
        if (BANNED_COMMITTED_SECRET.equals(secretKey.trim())) {
            throw new IllegalStateException(
                    "JWT_SECRET is set to the old committed default value, which is public and forgeable. " +
                    "Generate a new secret (e.g. `openssl rand -base64 48`) and set JWT_SECRET. Refusing to start.");
        }
        if (secretKey.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + secretKey.length() + " chars). Use at least " +
                    MIN_SECRET_LENGTH + " characters of high-entropy material. Refusing to start.");
        }
        log.info("JWT secret validated: length={} chars", secretKey.length());
    }

    /**
     * Extract username from JWT token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract a specific claim from JWT token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate access token for user.
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    /**
     * Generate access token with extra claims.
     */
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        extraClaims.put("type", "access");
        return buildToken(extraClaims, userDetails, accessTokenExpiration);
    }

    /**
     * Generate refresh token for user.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails, refreshTokenExpiration);
    }

    /**
     * Generate access token for User entity.
     */
    public String generateToken(User user) {
        return generateAccessToken(UserPrincipal.create(user));
    }

    /**
     * Generate refresh token for User entity.
     */
    public String generateRefreshToken(User user) {
        return generateRefreshToken(UserPrincipal.create(user));
    }

    /**
     * Validate token against user details.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate refresh token signature and username without checking JWT expiration.
     * Database-stored refresh tokens track their own expiration separately.
     */
    public boolean isRefreshTokenValidIgnoreExpiration(String token, UserDetails userDetails) {
        try {
            final Claims claims = extractAllClaimsIgnoreExpiration(token);
            final String username = claims.getSubject();
            final String type = claims.get("type", String.class);

            if (!"refresh".equals(type)) {
                log.warn("Token type mismatch: expected 'refresh' but got '{}'. " +
                        "This usually means the client is sending an access token instead of a refresh token.", type);
                return false;
            }

            if (!username.equals(userDetails.getUsername())) {
                log.warn("Token username mismatch: token has '{}' but expected '{}'", username, userDetails.getUsername());
                return false;
            }

            return true;
        } catch (JwtException e) {
            log.warn("Invalid refresh token JWT: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaimsIgnoreExpiration(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // Return the claims even if the token is expired - we validate expiration via DB
            return e.getClaims();
        }
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Get access token expiration in milliseconds.
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get refresh token expiration in milliseconds.
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * Validate token and return claims.
     */
    public Claims validateAndExtractClaims(String token) {
        try {
            return extractAllClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is an access token.
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "access".equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Check if token is a refresh token.
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            // Preferred: a Base64-encoded secret (e.g. `openssl rand -base64 48`),
            // but only if it decodes to a strong-enough key for HS256 (>=256 bits).
            byte[] decoded = Decoders.BASE64.decode(secretKey);
            keyBytes = decoded.length >= 32 ? decoded : secretKey.getBytes(StandardCharsets.UTF_8);
        } catch (DecodingException e) {
            // Not Base64 (e.g. a human-readable dev secret with '-') — use the raw
            // UTF-8 bytes. validateSecret() guarantees >=32 chars, so this is >=256 bits.
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
