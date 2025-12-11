package ee.kerrete.ainterview.interview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewNextQuestionResponseDto {
    UUID sessionId;
    @JsonProperty("isFinished")
    @Builder.Default
    boolean finished = false;
    Integer questionNumber;
    Integer totalQuestions;
    String question;
    String modelAnswerHint;
    LocalAnalysisDto localAnalysis;

    @Value
    @Builder
    public static class LocalAnalysisDto {
        List<String> detectedStrengths;
        List<String> detectedRisks;
    }
}

