package ee.krerte.aiinterview.dto;

import ee.krerte.aiinterview.model.TrainingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingProgressResponse {

    private String email;
    private int totalTasks;
    private int completedTasks;
    private int totalJobAnalyses;
    private int totalTrainingSessions;

    /**
     * Progress protsentides, lubame komakoha (nt 62.5).
     */
    private Double trainingProgressPercent;

    /**
     * Viimane aktiivsus (treening või tööanalüüs).
     */
    private LocalDateTime lastActivityAt;

    /**
     * Üldine treeningu staatus.
     */
    private TrainingStatus status;
}
