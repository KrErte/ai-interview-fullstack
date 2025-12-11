package ee.kerrete.ainterview.softskills.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class SoftSkillMergeResponse {

    SoftSkillMergedProfileDto mergedProfile;
    boolean saved;
    UUID savedProfileId;
    LocalDateTime createdAt;
}

