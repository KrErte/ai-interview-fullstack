package ee.kerrete.ainterview.interview.dto;

import jakarta.validation.constraints.NotBlank;

public record InterviewNextQuestionRequestDto(
    @NotBlank String answer
) {
}

