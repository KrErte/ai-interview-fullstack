package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachStateResponse {
    private String currentSkillKey;
    private String currentQuestion;
    private int questionsAsked;
    private int maxQuestions;
    private int tasksCompleted;
    private int totalTasks;
    private Integer trainingProgressPercent;
    private String lastFeedback;
}













