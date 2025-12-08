package ee.kerrete.ainterview.auth.dto;

import ee.kerrete.ainterview.model.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

    /**
     * Optional client-side confirmation; if present it must match password.
     */
    private String confirmPassword;

    // Role is optional: if not provided, defaults to CANDIDATE in service.
    private UserRole role;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        if (confirmPassword == null || confirmPassword.isBlank()) {
            return true;
        }
        return password != null && password.equals(confirmPassword);
    }
}
