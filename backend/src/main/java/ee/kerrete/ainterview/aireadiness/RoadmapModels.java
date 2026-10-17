package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.GeneralStageResult;
import ee.kerrete.ainterview.aireadiness.CatalogModels.RoadmapPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** POST /api/ai-readiness/roadmap (FP-60): the full roadmap behind the paywall. */
public final class RoadmapModels {

    private RoadmapModels() {
    }

    public record RoadmapRequest(@NotBlank @Size(max = 128) String professionId, @NotNull Map<String, String> answers) {
    }

    public record RoadmapResponse(List<GeneralStageResult> generalRoadmap, List<RoadmapPhase> professionRoadmap,
                                  int totalWeeks) {
    }
}
