package ee.kerrete.ainterview.dejavu.dto;

import jakarta.validation.constraints.NotBlank;

public record DejaVuPredictionRequest(@NotBlank String jobDescription) {
}

