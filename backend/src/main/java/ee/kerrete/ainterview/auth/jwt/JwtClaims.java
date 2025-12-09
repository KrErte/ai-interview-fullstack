package ee.kerrete.ainterview.auth.jwt;

/**
 * Constants for JWT claim names.
 */
public final class JwtClaims {
    private JwtClaims() {
        // Utility class - prevent instantiation
    }

    /** User's role (e.g., "CANDIDATE", "ADMIN") */
    public static final String ROLE = "role";

    /** User's database ID */
    public static final String USER_ID = "uid";

    /** Token type discriminator (e.g., "access", "refresh") */
    public static final String TOKEN_TYPE = "type";

    /** Token type values */
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
}
