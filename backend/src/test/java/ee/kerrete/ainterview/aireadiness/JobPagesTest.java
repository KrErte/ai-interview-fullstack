package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobPagesTest {

    private static final AiReadinessCatalog CATALOG = new AiReadinessCatalog(new ObjectMapper());
    private static final JobPages PAGES = new JobPages(CATALOG, new AiReadinessScorer(CATALOG), "https://example.org/");

    @Test
    void slugify() {
        assertEquals("cooks-restaurant", JobPages.slugify("Cooks, Restaurant"));
        assertEquals("helpers-roofers", JobPages.slugify("Helpers--Roofers"));
        assertEquals("computer-systems-engineers-architects", JobPages.slugify("Computer Systems Engineers/Architects"));
    }

    @Test
    void handWrittenProfessionsUseTheirIdAndOnetUseTheirName() {
        assertEquals("accountant", PAGES.slugOf("accountant"));
        assertEquals("cooks-restaurant", PAGES.slugOf("onet-35-2014-00"));
        assertEquals("onet-35-2014-00", PAGES.bySlug("cooks-restaurant").orElseThrow().id());
    }

    @Test
    void everyPageHasAUniqueSlugAndOtherIsLeftOut() {
        List<Profession> all = PAGES.all();
        assertEquals(CATALOG.professions().size() - 1, all.size());
        assertTrue(all.stream().noneMatch(p -> p.id().equals("other")));
        Set<String> slugs = new HashSet<>();
        all.forEach(p -> assertTrue(slugs.add(PAGES.slugOf(p.id())), "duplicate slug for " + p.id()));
        slugs.forEach(s -> assertTrue(s.matches("[a-z0-9]+(-[a-z0-9]+)*"), s));
    }

    @Test
    void groupsCoverEveryPageAndAreSorted() {
        Map<String, List<Profession>> groups = PAGES.groups();
        assertEquals(22, groups.size());
        assertEquals(PAGES.all().size(), groups.values().stream().mapToInt(List::size).sum());
        assertEquals(groups.keySet().stream().sorted().toList(), List.copyOf(groups.keySet()));
    }

    @Test
    void baseUrlHasNoTrailingSlash() {
        assertEquals("https://example.org/jobs/accountant", PAGES.url("/jobs/accountant"));
    }

    @Test
    void pageModelUsesTheNeutralScoreAndTopTasks() {
        JobPages.JobPage page = PAGES.page(PAGES.bySlug("cooks-restaurant").orElseThrow());
        assertEquals(page.result().taskExposureScore(), page.result().replaceabilityScore());
        assertEquals(6, page.topTasks().size());
        assertEquals(page.result().tasks().size() - 6, page.moreTasks());
        assertFalse(page.related().isEmpty());
    }
}
