package ee.kerrete.ainterview.interview.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class InterviewQuestionResponseDto {
    UUID sessionId;
    int questionNumber;
    int totalQuestions;
    String question;
    String modelAnswerHint;
}

