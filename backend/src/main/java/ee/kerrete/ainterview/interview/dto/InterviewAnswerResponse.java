package ee.kerrete.ainterview.interview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewAnswerResponse {
    UUID sessionId;
    @JsonProperty("isFinished")
    boolean finished;
    Integer questionNumber;
    Integer totalQuestions;
    String question;
    String modelAnswerHint;
    InterviewLocalAnalysisDto localAnalysis;
    InterviewSummaryDto summary;
}

