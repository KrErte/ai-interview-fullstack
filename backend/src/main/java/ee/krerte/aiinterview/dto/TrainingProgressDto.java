package ee.krerte.aiinterview.dto;

import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.model.TrainingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingProgressDto {

    private String email;

    private int totalTasks;
    private int completedTasks;

    private int totalJobAnalyses;
    private int totalTrainingSessions;

    private int trainingProgressPercent;

    private TrainingStatus status;

    private LocalDateTime lastActivityAt;
    private LocalDateTime lastUpdated;

    private Integer lastMatchScore;
    private String lastMatchSummary;

    /**
     * Põhimeetod – loeb kõik välja entity pealt.
     */
    public static TrainingProgressDto fromEntity(TrainingProgress entity) {
        if (entity == null) {
            return null;
        }

        return TrainingProgressDto.builder()
                .email(entity.getEmail())
                .totalTasks(entity.getTotalTasks())
                .completedTasks(entity.getCompletedTasks())
                .totalJobAnalyses(entity.getTotalJobAnalyses())
                .totalTrainingSessions(entity.getTotalTrainingSessions())
                .trainingProgressPercent(entity.getTrainingProgressPercent())
                .status(entity.getStatus())
                .lastActivityAt(entity.getLastActivityAt())
                // hetkel kasutame lastUpdated samana mis lastActivityAt
                .lastUpdated(entity.getLastActivityAt())
                .lastMatchScore(entity.getLastMatchScore())
                .lastMatchSummary(entity.getLastMatchSummary())
                .build();
    }

    /**
     * Ülekoormatud variant ProgressService jaoks,
     * kus completedTasks ja totalTasks arvutatakse eraldi.
     */
    public static TrainingProgressDto fromEntity(TrainingProgress entity,
                                                 long completedTasks,
                                                 long totalTasks) {
        TrainingProgressDto dto = fromEntity(entity);
        if (dto == null) {
            return null;
        }

        dto.setCompletedTasks((int) completedTasks);
        dto.setTotalTasks((int) totalTasks);

        return dto;
    }
}
