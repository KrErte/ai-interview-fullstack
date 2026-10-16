package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void hiddenOnetTitlesRedirectToTheHandWrittenPage() {
        assertEquals(Optional.of("software-developer"), PAGES.redirect("software-developers"));
        assertEquals(Optional.of("healthcare"), PAGES.redirect("registered-nurses"));
        assertEquals(24, CATALOG.hiddenTitles().size());
        CATALOG.hiddenTitles().keySet().forEach(title -> {
            String slug = JobPages.slugify(title);
            assertTrue(PAGES.bySlug(slug).isEmpty(), slug);
            assertTrue(PAGES.bySlug(PAGES.redirect(slug).orElseThrow()).isPresent(), slug);
        });
    }

    @Test
    void pagesAndUnknownSlugsAreNotRedirected() {
        assertTrue(PAGES.redirect("accountant").isEmpty());
        assertTrue(PAGES.redirect("cooks-restaurant").isEmpty());
        assertTrue(PAGES.redirect("no-such-job").isEmpty());
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

    @Test
    void summaryNamesNoLeadTaskWhenNoLargeTaskIsAutomatable() {
        // A cook's largest tasks are hands-on; the summary must not say AI does "mostly" the cooking.
        JobPages.JobPage cook = PAGES.page(PAGES.bySlug("cooks-restaurant").orElseThrow());
        assertNull(cook.leadTask());
    }

    @Test
    void summaryNamesTheMostAutomatableLargeTask() {
        JobPages.JobPage page = PAGES.page(PAGES.bySlug("data-entry-keyers").orElseThrow());
        assertNotNull(page.leadTask());
        assertTrue(page.leadTask().automationPercent() >= JobPages.LEAD_TASK_MIN_AUTOMATION);
        assertEquals(page.topTasks().get(0), page.leadTask());
    }

    @Test
    void faqAnswerUsesTheSentencesOnThePage() {
        JobPages.JobPage cook = PAGES.page(PAGES.bySlug("cooks-restaurant").orElseThrow());
        String answer = PAGES.faqAnswer(cook);
        assertTrue(answer.startsWith("Not entirely. Today's AI tools can take on about "
                + cook.result().taskExposureScore() + "% of the typical work in this occupation."));
        assertTrue(answer.contains("None of the largest tasks is highly automatable"));
        assertTrue(answer.contains("The parts that stay human include: "));
        assertTrue(answer.endsWith(JobPages.METHOD_SENTENCE));
    }

    @Test
    void jsonLdHasFaqAndBreadcrumbs() throws Exception {
        JobPages.JobPage page = PAGES.page(PAGES.bySlug("data-entry-keyers").orElseThrow());
        var json = new ObjectMapper().readTree(PAGES.jsonLd(page));
        assertEquals(2, json.size());
        assertEquals("FAQPage", json.get(0).get("@type").asText());
        var question = json.get(0).get("mainEntity").get(0);
        assertEquals("Will AI replace Data Entry Keyers?", question.get("name").asText());
        assertEquals(PAGES.faqAnswer(page), question.get("acceptedAnswer").get("text").asText());
        assertEquals("BreadcrumbList", json.get(1).get("@type").asText());
        var items = json.get(1).get("itemListElement");
        assertEquals("https://example.org/jobs", items.get(0).get("item").asText());
        assertEquals(2, items.get(1).get("position").asInt());
        assertEquals("https://example.org/jobs/data-entry-keyers", items.get(1).get("item").asText());
    }

    @Test
    void jsonLdCannotCloseTheScriptTag() throws Exception {
        JobPages.JobPage real = PAGES.page(PAGES.bySlug("cooks-restaurant").orElseThrow());
        Profession p = real.profession();
        Profession evil = new Profession(p.id(), "Cook </script><img src=x> & co", p.category(), p.tasks(), p.humanEdge(),
                p.aiTools(), p.pivotRoles(), p.roadmap(), p.onetCode(), p.onetCodes(), p.alternateTitles());
        JobPages.JobPage page = new JobPages.JobPage(evil, real.slug(), real.result(), real.topTasks(), real.moreTasks(),
                real.moreShare(), real.related(), real.group(), real.leadTask());
        String ld = PAGES.jsonLd(page);
        assertFalse(ld.contains("<"));
        assertFalse(ld.contains(">"));
        assertFalse(ld.contains("&"));
        assertEquals("Will AI replace Cook </script><img src=x> & co?",
                new ObjectMapper().readTree(ld).get(0).get("mainEntity").get(0).get("name").asText());
    }
}
