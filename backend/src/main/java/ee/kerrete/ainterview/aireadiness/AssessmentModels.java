package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.CatalogModels.Resource;
import ee.kerrete.ainterview.aireadiness.CatalogModels.RoadmapPhase;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
     * teamCode: valikuline kutselingi kood; tulemus lisatakse anonüümselt meeskonna koondisse.
     * teamEditToken: valikuline; selle brauseri varasema meeskonnavastuse uuendamiseks (nt liuguritega).
     */
    public record AssessmentRequest(
            @NotBlank String professionId,
            @NotNull Map<String, String> answers,
            boolean personalize,
            Map<String, Integer> taskShares,
            @Size(max = 32) String teamCode,
            @Size(max = 128) String teamEditToken
    ) {
        public AssessmentRequest(String professionId, Map<String, String> answers, boolean personalize) {
            this(professionId, answers, personalize, null, null, null);
        }

        public AssessmentRequest(String professionId, Map<String, String> answers, boolean personalize,
                                 Map<String, Integer> taskShares) {
            this(professionId, answers, personalize, taskShares, null, null);
        }
    }

    /**
     * Mis juhtus vastaja anonüümse meeskonnatulemusega: status "added", "updated", "full" või "not-found".
     * editToken on ainult "added" puhul, et see brauser saaks oma vastust hiljem uuendada.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TeamStatus(String status, String teamName, String editToken) {
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
            // FP-60: null (omitted) when the roadmap is locked behind the paywall, see locked()
            @JsonInclude(JsonInclude.Include.NON_NULL) List<GeneralStageResult> generalRoadmap,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<RoadmapPhase> professionRoadmap,
            int totalWeeks,
            String personalNote,
            @JsonInclude(JsonInclude.Include.NON_NULL) TeamStatus team,
            /* FP-60: first professionRoadmap phase, only when locked */
            @JsonInclude(JsonInclude.Include.NON_NULL) RoadmapPhase roadmapTeaser,
            boolean roadmapLocked
    ) {
        public AssessmentResult(String professionId, String professionName, int replaceabilityScore,
                                int taskExposureScore, int personalAdjustment, RiskBand band, String headline,
                                int readinessScore, String readinessLevel, List<TaskExposure> tasks,
                                List<String> humanEdge, List<String> aiTools, List<String> pivotRoles,
                                List<GeneralStageResult> generalRoadmap, List<RoadmapPhase> professionRoadmap,
                                int totalWeeks, String personalNote) {
            this(professionId, professionName, replaceabilityScore, taskExposureScore, personalAdjustment, band,
                    headline, readinessScore, readinessLevel, tasks, humanEdge, aiTools, pivotRoles, generalRoadmap,
                    professionRoadmap, totalWeeks, personalNote, null, null, false);
        }

        public AssessmentResult(String professionId, String professionName, int replaceabilityScore,
                                int taskExposureScore, int personalAdjustment, RiskBand band, String headline,
                                int readinessScore, String readinessLevel, List<TaskExposure> tasks,
                                List<String> humanEdge, List<String> aiTools, List<String> pivotRoles,
                                List<GeneralStageResult> generalRoadmap, List<RoadmapPhase> professionRoadmap,
                                int totalWeeks, String personalNote, TeamStatus team) {
            this(professionId, professionName, replaceabilityScore, taskExposureScore, personalAdjustment, band,
                    headline, readinessScore, readinessLevel, tasks, humanEdge, aiTools, pivotRoles, generalRoadmap,
                    professionRoadmap, totalWeeks, personalNote, team, null, false);
        }

        /**
         * FP-60 (billing enabled): the public result without the roadmap; only the first profession phase stays
         * as a teaser. The full roadmap is POST /api/ai-readiness/roadmap.
         */
        public AssessmentResult locked() {
            RoadmapPhase teaser = professionRoadmap == null || professionRoadmap.isEmpty() ? null : professionRoadmap.get(0);
            return new AssessmentResult(professionId, professionName, replaceabilityScore, taskExposureScore,
                    personalAdjustment, band, headline, readinessScore, readinessLevel, tasks, humanEdge, aiTools,
                    pivotRoles, null, null, totalWeeks, personalNote, team, teaser, true);
        }

        public AssessmentResult withTeam(TeamStatus team) {
            return new AssessmentResult(professionId, professionName, replaceabilityScore, taskExposureScore,
                    personalAdjustment, band, headline, readinessScore, readinessLevel, tasks, humanEdge, aiTools,
                    pivotRoles, generalRoadmap, professionRoadmap, totalWeeks, personalNote, team, roadmapTeaser,
                    roadmapLocked);
        }
    }
}
