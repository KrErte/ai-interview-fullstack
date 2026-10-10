package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.CatalogModels.Resource;
import ee.kerrete.ainterview.aireadiness.CatalogModels.RoadmapPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * API päringud ja vastused.
 */
public final class AssessmentModels {

    private AssessmentModels() {
    }

    /** answers: questionId -> optionId. Puuduvad vastused loetakse neutraalseks. */
    public record AssessmentRequest(
            @NotBlank String professionId,
            @NotNull Map<String, String> answers,
            boolean personalize
    ) {
    }

    public record ProfessionSummary(String id, String name, String category) {
    }

    public enum RiskBand { LOW, MODERATE, HIGH, VERY_HIGH }

    public enum StageStatus { DONE, CURRENT, UPCOMING }

    public record TaskExposure(String name, int share, int automationPercent) {
    }

    public record GeneralStageResult(
            String id,
            String title,
            String goal,
            StageStatus status,
            int weeks,
            List<String> items,
            List<Resource> resources
    ) {
    }

    public record AssessmentResult(
            String professionId,
            String professionName,
            int replaceabilityScore,
            int taskExposureScore,
            int personalAdjustment,
            RiskBand band,
            String headline,
            int readinessScore,
            String readinessLevel,
            List<TaskExposure> tasks,
            List<String> humanEdge,
            List<String> aiTools,
            List<String> pivotRoles,
            List<GeneralStageResult> generalRoadmap,
            List<RoadmapPhase> professionRoadmap,
            int totalWeeks,
            String personalNote
    ) {
    }
}
