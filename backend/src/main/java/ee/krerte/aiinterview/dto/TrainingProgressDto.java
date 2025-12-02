package ee.krerte.aiinterview.dto;

import ee.krerte.aiinterview.model.TrainingProgress;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TrainingProgressDto {

    Long id;
    String email;
    long completedTasks;
    long totalTasks;
    double completionRate;
    String status;
    LocalDateTime lastUpdated;

    public static TrainingProgressDto fromEntity(TrainingProgress entity,
                                                 long completedTasks,
                                                 long totalTasks) {

        double completionRate = totalTasks == 0
                ? 0.0
                : (double) completedTasks / (double) totalTasks;

        return TrainingProgressDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .completedTasks(completedTasks)
                .totalTasks(totalTasks)
                .completionRate(completionRate)
                // SIIN OLIGI PROBLEEM – muutsin name() peale
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .lastUpdated(entity.getLastUpdated())
                .build();
    }
}
