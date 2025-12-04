package ee.kerrete.ainterview.dto;

import ee.kerrete.ainterview.model.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatePlanResponse {

    private List<Question> questions;
    private List<PracticeBlock> practiceBlocks;
    private List<RoadmapStep> roadmap;
}
