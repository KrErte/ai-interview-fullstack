package ee.kerrete.ainterview.softskills.dto;

import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class SoftSkillEvaluationResponse {

    UUID id;
    String email;
    SoftSkillDimension dimension;
    SoftSkillSource source;
    Integer score;
    String comment;
    LocalDateTime createdAt;
}


