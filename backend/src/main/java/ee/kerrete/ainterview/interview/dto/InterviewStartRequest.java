package ee.kerrete.ainterview.interview.dto;

import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterviewStartRequest {
    @NotBlank
    String companyName;
    @NotBlank
    String roleTitle;
    @NotBlank
    String seniority;
    @NotNull
    InterviewerStyle interviewerStyle;
}

