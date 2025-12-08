package ee.kerrete.ainterview.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingPlanRequest {
    private String email;
    private String targetRole;
    private List<RoadmapTaskDto> tasks;
}













