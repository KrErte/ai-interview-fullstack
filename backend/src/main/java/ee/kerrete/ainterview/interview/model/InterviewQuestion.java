package ee.kerrete.ainterview.interview.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterviewQuestion {
    String question;
    String modelAnswerHint;
}

