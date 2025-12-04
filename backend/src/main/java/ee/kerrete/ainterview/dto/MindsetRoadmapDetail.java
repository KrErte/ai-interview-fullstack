package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ühe roadmapi detailne vaade – kokkuvõte + taskid.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindsetRoadmapDetail {

    private MindsetRoadmapSummary summary;

    /**
     * Kõik selle roadmapi taskid antud kasutaja jaoks.
     */
    private List<MindsetTaskDto> tasks;
}
