package ee.kerrete.ainterview.auth.dto;

import ee.kerrete.ainterview.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String email;
    private String fullName;
    private UserRole userRole;

}
