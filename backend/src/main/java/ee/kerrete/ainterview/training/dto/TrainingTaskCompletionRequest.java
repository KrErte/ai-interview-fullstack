package ee.kerrete.ainterview.training.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainingTaskCompletionRequest {
    @NotNull
    private Boolean completed;
}

