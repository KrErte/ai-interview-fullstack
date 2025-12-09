package ee.kerrete.ainterview.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Unified response for authentication endpoints (login, register, refresh).
 *
 * <p>Includes both 'token' (for backward compatibility) and 'accessToken' fields
 * that contain the same value.</p>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    /** Access token - backward compatible field name */
    String token,

    /** Access token - new field name */
    String accessToken,

    /** Refresh token for obtaining new access tokens */
    String refreshToken,

    /** User's email address */
    String email,

    /** User's display name */
    String fullName,

    /** User's role as string (e.g., "CANDIDATE", "ADMIN") */
    String role,

    /** User's database ID */
    Long userId
) {
    /**
     * Builder helper that sets both token fields for backward compatibility.
     */
    public static AuthResponseBuilder builderWithToken(String accessToken) {
        return AuthResponse.builder()
            .token(accessToken)
            .accessToken(accessToken);
    }
}
