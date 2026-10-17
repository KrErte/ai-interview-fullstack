package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.RoadmapTeaserPhase;
import ee.kerrete.ainterview.aireadiness.CatalogModels.RoadmapPhase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** FP-60: exact JSON of the roadmap teaser, also with fewer than 3 phases. */
class RoadmapTeaserPhaseTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void threePhasesGivePhaseOneInFullAndTitlesAfter() throws Exception {
        List<RoadmapPhase> phases = List.of(new RoadmapPhase("A", List.of("a1", "a2"), "pA"),
                new RoadmapPhase("B", List.of("b1"), "pB"), new RoadmapPhase("C", List.of("c1"), "pC"));
        assertThat(JSON.writeValueAsString(RoadmapTeaserPhase.of(phases))).isEqualTo(
                "[{\"phase\":1,\"title\":\"A\",\"items\":[\"a1\",\"a2\"],\"project\":\"pA\"},"
                        + "{\"phase\":2,\"title\":\"B\"},{\"phase\":3,\"title\":\"C\"}]");
    }

    @Test
    void fewerPhasesAndMissingDataAreHandled() throws Exception {
        assertThat(JSON.writeValueAsString(RoadmapTeaserPhase.of(List.of(new RoadmapPhase("Only", null, null)))))
                .isEqualTo("[{\"phase\":1,\"title\":\"Only\",\"items\":[]}]");
        assertThat(RoadmapTeaserPhase.of(List.of())).isEmpty();
        assertThat(RoadmapTeaserPhase.of(null)).isEmpty();
    }
}
