package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapTaskDto {
    private String taskKey;
    private String title;
    private String description;
    private boolean completed;
    private Integer dayNumber;
}

