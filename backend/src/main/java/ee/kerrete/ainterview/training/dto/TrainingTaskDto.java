package ee.kerrete.ainterview.training.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingTaskDto {
    String taskKey;
    boolean completed;
    Instant completedAt;
}

