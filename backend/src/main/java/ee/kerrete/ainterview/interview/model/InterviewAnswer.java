package ee.kerrete.ainterview.interview.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class InterviewAnswer {
    int questionNumber;
    String question;
    String answer;
    Instant answeredAt;
}

