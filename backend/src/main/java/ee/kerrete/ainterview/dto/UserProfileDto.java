package ee.kerrete.ainterview.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private String email;
    private String fullName;
    private String currentRole;
    private String targetRole;
    private Integer yearsOfExperience;
    private String skills;
    private String bio;
    private Integer profileCompleteness;
}













