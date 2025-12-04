package ee.kerrete.ainterview.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Ühe mindset-roadmapi taski DTO.
 */
@Data
@Builder
public class MindsetTaskDto {

    private String taskKey;
    private boolean completed;
    private Integer score;
    private String updatedAt;
}
