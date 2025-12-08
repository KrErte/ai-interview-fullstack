package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachAnswerResponse {
    private String nextQuestion;
    private String feedback;
    private Integer tasksCompleted;
    private Integer totalTasks;
    private Integer trainingProgressPercent;
    private Integer questionsAsked;
    private Integer maxQuestions;
    private Boolean sessionComplete;
    private String lastFeedback;
}













