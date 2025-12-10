package ee.kerrete.ainterview.interview.dto;

import lombok.Builder;

/**
 * Single simulated interview question with a suggested answer and simple metadata.
 */
@Builder
public record InterviewQuestionDto(
        String id,
        String question,
        String answer,
        String suggestedAnswer,
        String category,
        String difficulty
) {
}


