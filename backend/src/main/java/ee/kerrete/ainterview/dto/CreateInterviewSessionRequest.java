package ee.kerrete.ainterview.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewSessionRequest {

    @NotBlank
    @Email
    private String email;

    // soovi korral saad hiljem lisada nt:
    // private String targetRole;
    // private String seniority;
}
