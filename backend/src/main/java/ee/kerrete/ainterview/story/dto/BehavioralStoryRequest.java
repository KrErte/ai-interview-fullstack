package ee.kerrete.ainterview.story.dto;

import jakarta.validation.constraints.NotBlank;

public record BehavioralStoryRequest(
        @NotBlank String title,
        String situation,
        String task,
        String action,
        String result,
        String tags
) {
}

