package ee.kerrete.ainterview.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import ee.kerrete.ainterview.model.UserRole;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String fullName;

    // Role is optional: if not provided, defaults to CANDIDATE in service.
    private UserRole role;
}
