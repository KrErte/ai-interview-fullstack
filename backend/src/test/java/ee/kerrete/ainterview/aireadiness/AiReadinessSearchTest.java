package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentRequest;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.ProfessionSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReadinessSearchTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AiReadinessCatalog CATALOG = new AiReadinessCatalog(MAPPER);
    private static final AiReadinessService SERVICE = new AiReadinessService(
            CATALOG, new AiReadinessScorer(CATALOG), new AiReadinessCoach(MAPPER));

    private static List<String> ids(List<ProfessionSummary> results) {
        return results.stream().map(ProfessionSummary::id).toList();
    }

    @Test
    void professionsEndpointReturnsOnlyFeatured() {
        List<ProfessionSummary> all = SERVICE.professions();
        assertEquals(19, all.size());
        assertTrue(all.stream().allMatch(ProfessionSummary::featured));
    }

    @Test
    void emptyOrBlankQueryReturnsFeatured() {
        assertEquals(ids(SERVICE.professions()), ids(SERVICE.search("", 50)));
        assertEquals(ids(SERVICE.professions()), ids(SERVICE.search("   ", 50)));
        assertEquals(ids(SERVICE.professions()), ids(SERVICE.search(null, 50)));
    }

    @Test
    void softwareDeveloperFindsCuratedFirst() {
        List<ProfessionSummary> results = SERVICE.search("software developer", 10);
        assertEquals("software-developer", results.get(0).id());
        assertFalse(ids(results).contains("onet-15-1252-00"));
    }

    @Test
    void coderFindsAProgrammingOccupationViaAlternateTitle() {
        List<String> top = ids(SERVICE.search("coder", 5));
        assertTrue(top.stream().anyMatch(id -> id.startsWith("onet-15-") || id.equals("software-developer")),
                top.toString());
    }

    @Test
    void caseInsensitive() {
        assertEquals(ids(SERVICE.search("nurse", 20)), ids(SERVICE.search("NURSE", 20)));
        assertFalse(SERVICE.search("nurse", 20).isEmpty());
    }

    @Test
    void namePrefixBeatsContains() {
        List<ProfessionSummary> results = SERVICE.search("cook", 20);
        assertTrue(results.get(0).name().toLowerCase().startsWith("cook"), results.get(0).name());
    }

    @Test
    void limitIsClamped() {
        assertEquals(1, SERVICE.search("a", 0).size());
        assertEquals(50, SERVICE.search("a", 500).size());
    }

    @Test
    void assessWorksForOnetProfessionAndRejectsHiddenOne() {
        AssessmentResult result = SERVICE.assess(new AssessmentRequest("onet-35-2014-00", Map.of(), false));
        assertEquals("Cooks, Restaurant", result.professionName());
        assertTrue(result.replaceabilityScore() >= 3 && result.replaceabilityScore() <= 97);
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.assess(new AssessmentRequest("onet-15-1252-00", Map.of(), false)));
    }

    @Test
    void onetResultCarriesObservedUse() {
        AssessmentResult result = SERVICE.assess(new AssessmentRequest("onet-43-4171-00", Map.of(), false));
        assertTrue(result.tasks().stream().anyMatch(AssessmentModels.TaskExposure::observedUse));
        AssessmentResult curated = SERVICE.assess(new AssessmentRequest("translator", Map.of(), false));
        assertTrue(curated.tasks().stream().noneMatch(AssessmentModels.TaskExposure::observedUse));
    }
}
