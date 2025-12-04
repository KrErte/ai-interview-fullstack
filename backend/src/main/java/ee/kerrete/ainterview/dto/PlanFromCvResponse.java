package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanFromCvResponse {

    private java.util.List<PracticeBlock> practiceBlocks;
    private java.util.List<RoadmapStep> roadmap;
}
