package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.RiskBand;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.StageStatus;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.ProfessionTask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReadinessScorerTest {

    private final AiReadinessCatalog catalog = new AiReadinessCatalog(new ObjectMapper());
    private final AiReadinessScorer scorer = new AiReadinessScorer(catalog);

    @Test
    void catalogLoadsAndEveryProfessionHasRoadmap() {
        assertFalse(catalog.professions().isEmpty());
        assertFalse(catalog.questions().isEmpty());
        assertEquals(5, catalog.generalRoadmap().size());
        catalog.professions().forEach(p -> {
            assertFalse(p.roadmap().isEmpty(), p.id());
            assertFalse(p.humanEdge().isEmpty(), p.id());
        });
    }

    @Test
    void readinessMaxIsHundred() {
        int max = catalog.questions().stream()
                .mapToInt(q -> q.options().stream().mapToInt(CatalogModels.QuestionOption::readiness).max().orElse(0))
                .sum();
        assertEquals(100, max);
    }

    @Test
    void withoutAnswersScoreEqualsTaskExposure() {
        Profession translator = catalog.profession("translator").orElseThrow();
        AssessmentResult result = scorer.score(translator, Map.of());

        assertEquals(70, result.taskExposureScore());
        assertEquals(0, result.personalAdjustment());
        assertEquals(70, result.replaceabilityScore());
        assertEquals(RiskBand.VERY_HIGH, result.band());
        assertEquals(0, result.readinessScore());
        assertEquals(StageStatus.CURRENT, result.generalRoadmap().get(0).status());
    }

    @Test
    void humanCentricAnswersLowerTheScoreAndAiUsageRaisesReadiness() {
        Profession dev = catalog.profession("software-developer").orElseThrow();
        AssessmentResult baseline = scorer.score(dev, Map.of());
        AssessmentResult result = scorer.score(dev, Map.of(
                "routine", "lt20",
                "people", "lot",
                "judgment", "role",
                "experience", "gt10",
                "ai_usage", "daily",
                "ai_depth", "builder",
                "learning", "fast",
                "hours", "10"
        ));

        assertTrue(result.replaceabilityScore() < baseline.replaceabilityScore());
        assertEquals(90, result.readinessScore());
        assertTrue(result.generalRoadmap().stream().limit(4).allMatch(s -> s.status() == StageStatus.DONE));
        assertEquals(StageStatus.CURRENT, result.generalRoadmap().get(4).status());
        // 40h / 8h per week
        assertEquals(5, result.totalWeeks());
    }

    @Test
    void adjustmentIsCapped() {
        Profession p = new Profession("x", "X", "c",
                List.of(new ProfessionTask("t", 100, 0.5)), List.of(), List.of(), List.of(), List.of());
        AssessmentResult result = scorer.score(p, Map.of(
                "routine", "gt80", "people", "little", "physical", "no", "judgment", "rarely",
                "experience", "lt2", "domain", "no", "ai_usage", "never", "ai_depth", "none",
                "learning", "slow", "org", "yes"
        ));
        assertEquals(AiReadinessScorer.MAX_ADJUSTMENT, result.personalAdjustment());
        assertEquals(80, result.replaceabilityScore());
    }

    @Test
    void tasksAreSortedByAutomation() {
        AssessmentResult result = scorer.score(catalog.profession("healthcare").orElseThrow(), Map.of());
        assertEquals(75, result.tasks().get(0).automationPercent());
        assertEquals(5, result.tasks().get(result.tasks().size() - 1).automationPercent());
    }

    @Test
    void unknownAnswersAreRejected() {
        Profession dev = catalog.profession("software-developer").orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> scorer.score(dev, Map.of("routine", "nope")));
        assertThrows(IllegalArgumentException.class, () -> scorer.score(dev, Map.of("nope", "lt20")));
    }

    @Test
    void bandsHaveExpectedBoundaries() {
        assertEquals(RiskBand.LOW, AiReadinessScorer.band(29));
        assertEquals(RiskBand.MODERATE, AiReadinessScorer.band(30));
        assertEquals(RiskBand.HIGH, AiReadinessScorer.band(50));
        assertEquals(RiskBand.VERY_HIGH, AiReadinessScorer.band(70));
    }
}
