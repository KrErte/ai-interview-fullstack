package ee.kerrete.ainterview.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Externalized JWT configuration properties.
 * Configure via application.yml or environment variables:
 * - app.jwt.secret or JWT_SECRET
 * - app.jwt.expiration-ms
 * - app.jwt.refresh-expiration-ms
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
    @NotBlank String secret,
    @Positive long expirationMs,
    @Positive long refreshExpirationMs
) {
    public JwtProperties {
        // Apply defaults if not provided
        if (expirationMs <= 0) {
            expirationMs = 86400000L; // 24 hours
        }
        if (refreshExpirationMs <= 0) {
            refreshExpirationMs = 604800000L; // 7 days
        }
    }
}
