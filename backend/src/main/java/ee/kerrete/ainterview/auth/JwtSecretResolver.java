package ee.kerrete.ainterview.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Turns the configured {@code JWT_SECRET} ({@code app.jwt.secret}) into HMAC-SHA256 key bytes (FP-28).
 * <p>
 * Format: the value is used as a raw string (its UTF-8 bytes), e.g. the output of
 * {@code openssl rand -base64 48}. To supply exact key bytes instead, prefix base64 with {@code base64:}
 * (e.g. {@code base64:3q2+7w...}); the decoded bytes are used. Either way the key must be at least
 * 32 bytes (256 bits). The placeholder / old-key checks run on the raw value AND on the final key bytes
 * (decoded base64 included, also read as UTF-8 text), so the old key cannot come back base64-encoded.
 * <p>
 * Strict mode (prod profile): a missing, too short or placeholder secret is a startup error.
 * Lenient mode (local/dev/test): a missing secret is replaced by a random key for this run.
 */
public final class JwtSecretResolver {

    public static final int MIN_KEY_BYTES = 32;
    static final String BASE64_PREFIX = "base64:";
    /** The signing key that used to be hardcoded in JwtService; it is public, so never accept it. */
    static final String OLD_HARDCODED_SECRET = "super-secret-key-change-me-please-1234567890";

    private JwtSecretResolver() {
    }

    public record Resolved(byte[] keyBytes, boolean random) {
    }

    public static Resolved resolve(String configured, boolean strict) {
        if (configured == null || configured.isBlank()) {
            if (strict) {
                throw new IllegalStateException("JWT_SECRET is not set. The prod profile requires a signing key of at "
                        + "least " + MIN_KEY_BYTES + " bytes; generate one with: openssl rand -base64 48");
            }
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            return new Resolved(random, true);
        }

        String value = configured.trim();
        if (isPlaceholder(value)) {
            throw placeholder();
        }

        byte[] key;
        if (value.startsWith(BASE64_PREFIX)) {
            try {
                key = Base64.getDecoder().decode(value.substring(BASE64_PREFIX.length()));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("JWT_SECRET starts with 'base64:' but the rest is not valid base64");
            }
        } else {
            key = value.getBytes(StandardCharsets.UTF_8);
        }

        // Check the bytes that would actually sign tokens, so the old key (or a placeholder) cannot be
        // smuggled in as base64:<base64 of it>. The decoded bytes are also checked as UTF-8 text.
        if (isPlaceholderKey(key)) {
            throw placeholder();
        }

        if (key.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET is too short: " + key.length + " bytes, at least "
                    + MIN_KEY_BYTES + " bytes (256 bits) are required. Generate one with: openssl rand -base64 48");
        }
        return new Resolved(key, false);
    }

    private static IllegalStateException placeholder() {
        return new IllegalStateException("JWT_SECRET is a placeholder or the old public default key. "
                + "Generate a real one with: openssl rand -base64 48");
    }

    /** True for the old hardcoded key's bytes, or key bytes whose UTF-8 text is a placeholder. */
    static boolean isPlaceholderKey(byte[] key) {
        if (MessageDigest.isEqual(key, OLD_HARDCODED_SECRET.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        String text = new String(key, StandardCharsets.UTF_8);
        return isPlaceholder(text) || isPlaceholder(text.trim());
    }

    static boolean isPlaceholder(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return value.equals(OLD_HARDCODED_SECRET)
                || lower.contains("change-me")
                || lower.contains("changeme")
                || lower.contains("change_me");
    }
}
