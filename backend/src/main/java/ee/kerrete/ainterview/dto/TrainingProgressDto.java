package ee.kerrete.ainterview.dto;

import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingProgressDto {

    private String email;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer totalJobAnalyses;
    private Integer totalTrainingSessions;
    private Integer trainingProgressPercent;
    private TrainingStatus status;
    private LocalDateTime lastActivityAt;
    private LocalDateTime lastUpdated;

    /**
     * Lihtne map ainult entitist.
     */
    public static TrainingProgressDto fromEntity(TrainingProgress entity) {
        return TrainingProgressDto.builder()
                .email(entity.getEmail())
                .totalTasks(entity.getTotalTasks())
                .completedTasks(entity.getCompletedTasks())
                .totalJobAnalyses(entity.getTotalJobAnalyses())
                .totalTrainingSessions(entity.getTotalTrainingSessions())
                .trainingProgressPercent(entity.getTrainingProgressPercent())
                .status(entity.getStatus())
                .lastActivityAt(entity.getLastActivityAt())
                .lastUpdated(entity.getLastUpdated())
                .build();
    }

    /**
     * VANA signatuur ProgressService jaoks (koos completed/total komadega).
     */
    public static TrainingProgressDto fromEntity(TrainingProgress entity,
                                                 long completedTasks,
                                                 long totalTasks) {
        int percent = (totalTasks == 0)
                ? 0
                : (int) Math.round(100.0 * completedTasks / totalTasks);

        return TrainingProgressDto.builder()
                .email(entity.getEmail())
                .totalTasks(entity.getTotalTasks())
                .completedTasks(entity.getCompletedTasks())
                .totalJobAnalyses(entity.getTotalJobAnalyses())
                .totalTrainingSessions(entity.getTotalTrainingSessions())
                .trainingProgressPercent(percent)
                .status(entity.getStatus())
                .lastActivityAt(entity.getLastActivityAt())
                .lastUpdated(entity.getLastUpdated())
                .build();
    }
}
