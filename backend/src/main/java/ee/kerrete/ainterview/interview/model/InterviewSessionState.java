package ee.kerrete.ainterview.interview.model;

import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class InterviewSessionState {
    UUID sessionId;
    String candidateEmail;
    String companyName;
    String roleTitle;
    String seniority;
    InterviewerStyle interviewerStyle;
    int totalQuestions;
    @Builder.Default
    List<InterviewQuestion> questions = new ArrayList<>();
    @Builder.Default
    List<InterviewAnswer> answers = new ArrayList<>();
    Instant createdAt;
    Instant lastUpdatedAt;

    public InterviewSessionState appendAnswer(InterviewAnswer answer) {
        List<InterviewAnswer> updatedAnswers = new ArrayList<>(answers);
        updatedAnswers.add(answer);
        return this.toBuilder()
            .answers(updatedAnswers)
            .lastUpdatedAt(answer.getAnsweredAt())
            .build();
    }
}

