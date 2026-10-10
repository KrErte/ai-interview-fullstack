package ee.kerrete.ainterview.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

/**
 * Issues and validates JWTs. The HMAC-SHA256 signing key comes from {@code JWT_SECRET}
 * ({@code app.jwt.secret}); see {@link JwtSecretResolver} for the accepted format and rules (FP-28).
 */
@Slf4j
@Service
public class JwtService {

    // 24h
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000;

    private final Key signingKey;

    public JwtService(@Value("${app.jwt.secret:}") String secret, Environment environment) {
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        JwtSecretResolver.Resolved resolved = JwtSecretResolver.resolve(secret, prod);
        if (resolved.random()) {
            log.warn("JWT_SECRET is not set: using a random JWT signing key for this run. "
                    + "Tokens become invalid on restart. Set JWT_SECRET to keep sessions across restarts.");
        }
        this.signingKey = Keys.hmacShaKeyFor(resolved.keyBytes());
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
