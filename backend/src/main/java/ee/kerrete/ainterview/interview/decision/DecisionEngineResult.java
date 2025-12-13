package ee.kerrete.ainterview.interview.decision;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DecisionEngineResult {
    boolean endInterview;
    String questionId;
    InterviewState state;
}

