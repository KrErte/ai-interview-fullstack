package ee.kerrete.ainterview.auth.dto.request;

import ee.kerrete.ainterview.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for user registration.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Full name is required")
    String fullName,

    /** Optional: defaults to CANDIDATE if not provided */
    UserRole role
) {
    /**
     * Get role with default fallback.
     */
    public UserRole roleOrDefault() {
        return role != null ? role : UserRole.CANDIDATE;
    }
}
