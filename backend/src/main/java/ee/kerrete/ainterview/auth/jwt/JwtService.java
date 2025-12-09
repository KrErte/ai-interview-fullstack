package ee.kerrete.ainterview.auth.jwt;

import ee.kerrete.ainterview.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Optional;

/**
 * Service for JWT token generation, parsing, and validation.
 * Uses externalized configuration via JwtProperties.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties properties;
    private volatile Key signingKey;

    private Key getSigningKey() {
        if (signingKey == null) {
            synchronized (this) {
                if (signingKey == null) {
                    signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes());
                }
            }
        }
        return signingKey;
    }

    /**
     * Generate an access token with user claims.
     * Access tokens include role and userId for stateless authentication.
     */
    public String generateAccessToken(String email, UserRole role, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.expirationMs());

        return Jwts.builder()
            .setSubject(email)
            .claim(JwtClaims.ROLE, role.name())
            .claim(JwtClaims.USER_ID, userId)
            .claim(JwtClaims.TOKEN_TYPE, JwtClaims.TYPE_ACCESS)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Generate a refresh token with minimal claims.
     * Refresh tokens have longer expiry and are used to obtain new access tokens.
     */
    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.refreshExpirationMs());

        return Jwts.builder()
            .setSubject(email)
            .claim(JwtClaims.TOKEN_TYPE, JwtClaims.TYPE_REFRESH)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use {@link #generateAccessToken(String, UserRole, Long)} instead.
     */
    @Deprecated
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.expirationMs());

        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Parse and validate a token, returning claims if valid.
     * Returns empty if token is invalid, malformed, or expired.
     */
    public Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract email (subject) from token.
     */
    public String extractEmail(String token) {
        return parseToken(token)
            .map(Claims::getSubject)
            .orElse(null);
    }

    /**
     * Extract user role from token claims.
     */
    public UserRole extractRole(String token) {
        return parseToken(token)
            .map(claims -> claims.get(JwtClaims.ROLE, String.class))
            .filter(role -> role != null && !role.isBlank())
            .map(UserRole::valueOf)
            .orElse(null);
    }

    /**
     * Extract user ID from token claims.
     */
    public Long extractUserId(String token) {
        return parseToken(token)
            .map(claims -> claims.get(JwtClaims.USER_ID, Long.class))
            .orElse(null);
    }

    /**
     * Extract token type from claims.
     */
    public String extractTokenType(String token) {
        return parseToken(token)
            .map(claims -> claims.get(JwtClaims.TOKEN_TYPE, String.class))
            .orElse(null);
    }

    /**
     * Check if a token is expired.
     */
    public boolean isTokenExpired(String token) {
        return parseToken(token)
            .map(claims -> claims.getExpiration().before(new Date()))
            .orElse(true);
    }

    /**
     * Check if token is a valid refresh token.
     */
    public boolean isRefreshToken(String token) {
        return JwtClaims.TYPE_REFRESH.equals(extractTokenType(token));
    }

    /**
     * Check if token is a valid access token.
     */
    public boolean isAccessToken(String token) {
        String type = extractTokenType(token);
        // For backward compatibility, tokens without type are considered access tokens
        return type == null || JwtClaims.TYPE_ACCESS.equals(type);
    }
}
