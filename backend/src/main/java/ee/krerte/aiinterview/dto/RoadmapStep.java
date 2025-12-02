package ee.krerte.aiinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapStep {

    private String label;
    private String focus;
    private String details;
}
