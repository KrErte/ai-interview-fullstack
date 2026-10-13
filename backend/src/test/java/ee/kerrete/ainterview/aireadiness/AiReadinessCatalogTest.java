package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.ProfessionTask;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReadinessCatalogTest {

    private static final AiReadinessCatalog CATALOG = new AiReadinessCatalog(new ObjectMapper());

    @Test
    void loadsCuratedAndOnetProfessions() {
        assertEquals(19, CATALOG.featured().size());
        assertEquals("software-developer", CATALOG.featured().get(0).id());
        assertTrue(CATALOG.professions().size() > 900, "got " + CATALOG.professions().size());
    }

    @Test
    void hidesOnetCodesCoveredByCuratedProfessions() {
        // Software Developers and Registered Nurses are covered by software-developer and healthcare
        assertTrue(CATALOG.profession("onet-15-1252-00").isEmpty());
        assertTrue(CATALOG.profession("onet-29-1141-00").isEmpty());
        Set<String> hidden = new HashSet<>();
        CATALOG.featured().forEach(p -> hidden.addAll(p.onetCodes() == null ? Set.of() : p.onetCodes()));
        assertFalse(hidden.isEmpty());
        CATALOG.professions().forEach(p -> assertFalse(p.onetCode() != null && hidden.contains(p.onetCode()), p.id()));
    }

    @Test
    void onetProfessionIsAvailable() {
        Profession cook = CATALOG.profession("onet-35-2014-00").orElseThrow();
        assertEquals("Cooks, Restaurant", cook.name());
        assertEquals("35-2014.00", cook.onetCode());
        assertFalse(cook.alternateTitles().isEmpty());
    }

    @Test
    void everyProfessionValidates() {
        for (Profession p : CATALOG.professions()) {
            assertEquals(100, p.tasks().stream().mapToInt(ProfessionTask::share).sum(), p.id());
            p.tasks().forEach(t -> assertTrue(t.automation() >= 0 && t.automation() <= 1, p.id()));
            assertFalse(p.humanEdge().isEmpty(), p.id());
            assertEquals(3, p.roadmap().size(), p.id());
        }
    }

    @Test
    void curatedTasksDefaultToNotObserved() {
        CATALOG.featured().forEach(p -> p.tasks().forEach(t -> assertFalse(t.observedUse(), p.id())));
    }
}
