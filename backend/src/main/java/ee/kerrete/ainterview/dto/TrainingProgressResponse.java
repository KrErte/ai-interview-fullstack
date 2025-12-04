package ee.kerrete.ainterview.dto;

import ee.kerrete.ainterview.model.TrainingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingProgressResponse {

    private String email;

    private Integer totalTasks;

    private Integer completedTasks;

    private Integer totalJobAnalyses;

    private Integer totalTrainingSessions;

    /**
     * Üldine progress protsentides (0–100).
     */
    private Integer trainingProgressPercent;

    /**
     * Treeningu staatus (NOT_STARTED, IN_PROGRESS, COMPLETED).
     */
    private TrainingStatus status;

    /**
     * Viimane aktiivsus (viimane training task või job analysis).
     */
    private LocalDateTime lastActivityAt;

    /**
     * Viimase Job Matcheri analüüsi match score.
     */
    private Double lastMatchScore;

    /**
     * Viimase Job Matcheri analüüsi lühikokkuvõte.
     */
    private String lastMatchSummary;
}
