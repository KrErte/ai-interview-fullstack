package ee.kerrete.ainterview.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for token refresh.
 */
public record RefreshRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
