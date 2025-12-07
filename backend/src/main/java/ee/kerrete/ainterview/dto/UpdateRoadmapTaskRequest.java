package ee.kerrete.ainterview.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoadmapTaskRequest {
    private String email;
    private String taskKey;
    private boolean completed;
}

