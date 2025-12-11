package ee.kerrete.ainterview.training.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TrainingStatusDto {
    int totalTasks;
    int completedTasks;
    int progressPercent;
    List<TrainingTaskDto> tasks;
}

