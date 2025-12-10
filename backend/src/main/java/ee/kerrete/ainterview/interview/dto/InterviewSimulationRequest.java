package ee.kerrete.ainterview.interview.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for Ghost Interviewer LITE simulation.
 * <p>
 * Matches the Angular `InterviewSimulationRequest` interface used in `MvpApiService`.
 */
public record InterviewSimulationRequest(
        @NotBlank String companyName,
        @NotBlank String roleTitle,
        String seniority
) {
}

