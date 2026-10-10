package ee.kerrete.ainterview.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String fullName;

    // No role field on purpose: self-registration always gets UserRole.CANDIDATE (see AuthService).
    // A "role" property sent by a client is ignored (ignoreUnknown above, independent of global Jackson config).
}
