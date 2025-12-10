package ee.kerrete.ainterview.story.dto;

import java.time.LocalDateTime;

public record BehavioralStoryResponse(
        Long id,
        String title,
        String situation,
        String task,
        String action,
        String result,
        String tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

