package ee.kerrete.ainterview.interview.dto;

import java.util.List;

/**
 * Response payload for Ghost Interviewer LITE simulation.
 */
public record InterviewSimulationResponse(
        String sessionId,
        String companyName,
        String roleTitle,
        String seniority,
        List<InterviewQuestionDto> questions,
        String summary,
        List<String> suggestedNextSteps
) {
}

