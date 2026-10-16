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

    /**
     * answers: questionId -> optionId. Puuduvad vastused loetakse neutraalseks.
     * taskShares: valikuline, ülesande nimi -> osakaal nädalast (0..100, summa 100), kui inimese nädal
     * erineb kataloogist. Puudumisel kasutatakse kataloogi osakaale.
     */
    public record AssessmentRequest(
            @NotBlank String professionId,
            @NotNull Map<String, String> answers,
            boolean personalize,
            Map<String, Integer> taskShares
    ) {
        public AssessmentRequest(String professionId, Map<String, String> answers, boolean personalize) {
            this(professionId, answers, personalize, null);
        }
    }

    /** featured: käsitsi kirjutatud amet (näidatakse ilma otsinguta). */
    public record ProfessionSummary(String id, String name, String category, boolean featured) {
    }

    /** Valikulised funktsioonid, mis sõltuvad serveri seadistusest (nt AI kommentaar vajab API võtit). */
    public record Features(boolean aiComment) {
    }

    /** SOC peagrupp ja selle ametite arv (ametite sirvimiseks). */
    public record CategorySummary(String name, int count) {
    }

    public enum RiskBand { LOW, MODERATE, HIGH, VERY_HIGH }

    public enum StageStatus { DONE, CURRENT, UPCOMING }

    public record TaskExposure(String name, int share, int automationPercent, boolean observedUse) {
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
