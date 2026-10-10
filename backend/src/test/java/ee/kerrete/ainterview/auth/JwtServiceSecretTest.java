package ee.kerrete.ainterview.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-28: the JWT signing key comes from JWT_SECRET; prod fails fast on a missing, short or placeholder
 * secret, other profiles fall back to a random key, and the old hardcoded key is never accepted.
 */
@ExtendWith(OutputCaptureExtension.class)
class JwtServiceSecretTest {

    private static final String GOOD_SECRET = "q8Vx1oT0n2Zr7yKc5mWb3sLp9dHf4gJa6uNe0iRtXwYzA1Bc"; // 48 chars
    private static final String OLD_KEY = "super-secret-key-change-me-please-1234567890";

    private static MockEnvironment env(String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void prodFailsFastWithoutSecret(String secret) {
        assertThatThrownBy(() -> new JwtService(secret, env("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is not set")
                .hasMessageContaining("openssl rand -base64 48");
    }

    @Test
    void prodFailsFastWithShortSecret() {
        assertThatThrownBy(() -> new JwtService("only-31-bytes-long-secret-value", env("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short: 31 bytes")
                .hasMessageNotContaining("only-31-bytes-long-secret-value");
    }

    @ParameterizedTest
    @ValueSource(strings = {OLD_KEY, "change-me-change-me-change-me-change-me", "CHANGEME-please-0123456789abcdefghijkl"})
    void prodFailsFastWithPlaceholderSecret(String secret) {
        assertThatThrownBy(() -> new JwtService(secret, env("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    /** QA (FP-28 review): base64:<base64 of the old key> used to start prod and accept forged tokens. */
    @ParameterizedTest
    @ValueSource(strings = {OLD_KEY, OLD_KEY + "\n", "  " + OLD_KEY + "  ", "change-me-change-me-change-me-change-me"})
    void base64OfOldKeyOrPlaceholderIsRefusedInEveryProfile(String plain) {
        for (String b64 : new String[]{
                Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8)),
                Base64.getEncoder().withoutPadding().encodeToString(plain.getBytes(StandardCharsets.UTF_8))}) {
            for (String profile : new String[]{"prod", "dev"}) {
                assertThatThrownBy(() -> new JwtService("base64:" + b64, env(profile)))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("placeholder")
                        .hasMessageNotContaining(b64);
            }
        }
    }

    @Test
    void prodContextRefusesBase64OfOldKey() {
        String b64 = Base64.getEncoder().encodeToString(OLD_KEY.getBytes(StandardCharsets.UTF_8));
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod", "app.jwt.secret=base64:" + b64)
                .withBean(JwtService.class)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("placeholder or the old public default key"));
    }

    @Test
    void prodAcceptsStrongRawAndBase64Secrets() {
        JwtService raw = new JwtService(GOOD_SECRET, env("prod"));
        assertThat(raw.extractEmail(raw.generateToken("a@b.test"))).isEqualTo("a@b.test");

        String b64 = "base64:" + Base64.getEncoder().encodeToString(new byte[32]).replace('A', 'B');
        JwtService decoded = new JwtService(b64, env("prod"));
        assertThat(decoded.extractEmail(decoded.generateToken("c@d.test"))).isEqualTo("c@d.test");
    }

    /** The documented format: JWT_SECRET=base64:<base64 of 48 random bytes> (openssl / PowerShell output). */
    @Test
    void base64Of48RandomBytesGives48ByteKey() {
        byte[] random = new byte[48];
        new java.security.SecureRandom().nextBytes(random);
        String generated = Base64.getEncoder().encodeToString(random); // 64 chars, like openssl rand -base64 48
        assertThat(generated).hasSize(64);

        JwtSecretResolver.Resolved resolved = JwtSecretResolver.resolve("base64:" + generated + "\r", true);
        assertThat(resolved.keyBytes()).hasSize(48).isEqualTo(random);
        assertThat(resolved.random()).isFalse();
    }

    @Test
    void base64SecretMustDecodeToAtLeast32Bytes() {
        String b64 = "base64:" + Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new JwtService(b64, env("prod"))).hasMessageContaining("too short: 16 bytes");
    }

    @Test
    void devGeneratesRandomKeyWhenUnsetAndNeverLogsIt(CapturedOutput output) {
        JwtService first = new JwtService("", env("dev"));
        JwtService second = new JwtService(null, env("local"));
        JwtService noProfile = new JwtService("", env());

        String token = first.generateToken("dev@b.test");
        assertThat(first.extractEmail(token)).isEqualTo("dev@b.test");
        // different random key per instance/run
        assertThatThrownBy(() -> second.extractEmail(token)).isInstanceOf(io.jsonwebtoken.JwtException.class);
        assertThat(noProfile.extractEmail(noProfile.generateToken("x@y.test"))).isEqualTo("x@y.test");

        assertThat(output.getAll()).contains("using a random JWT signing key");
    }

    @Test
    void tokenSignedWithOldHardcodedKeyIsRejected() {
        String forged = Jwts.builder()
                .setSubject("admin@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(OLD_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        for (JwtService service : new JwtService[]{new JwtService(GOOD_SECRET, env("prod")), new JwtService("", env("dev"))}) {
            assertThatThrownBy(() -> service.extractEmail(forged))
                    .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
        }
    }

    @Test
    void prodApplicationContextFailsToStartWithoutSecret() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod")
                .withBean(JwtService.class)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("JWT_SECRET is not set"));

        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod", "app.jwt.secret=" + GOOD_SECRET)
                .withBean(JwtService.class)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtService.class));
    }
}
