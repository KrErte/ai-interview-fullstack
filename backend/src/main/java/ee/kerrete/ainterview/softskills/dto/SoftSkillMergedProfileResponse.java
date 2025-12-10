package ee.kerrete.ainterview.softskills.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class SoftSkillMergedProfileResponse {

    UUID id;
    String email;
    Integer overallScore;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<SoftSkillMergedDimensionDto> dimensions;
}


