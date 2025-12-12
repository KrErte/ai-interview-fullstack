package ee.kerrete.ainterview.interview.dto;

import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InterviewProgressResponseDto {
    String question;
    InterviewerStyle interviewerStyle;
    UsedContext usedContext;

    @Value
    @Builder
    public static class UsedContext {
        String lastAnswer;
        List<String> last3;
        List<String> last5;
    }
}

