package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.RoadmapPhase;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.billing.BillingSettings;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.billing.PurchaseRepository;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.team.TeamService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * FP-66: with billing on, no anonymous surface carries roadmap items or projects beyond the /assess teaser
 * (asserted with the real catalog strings); with billing off the job HTML is byte-identical to origin/main
 * (fixtures in src/test/resources/fp66-golden, rendered from origin/main c4e4edc).
 */
class PublicRoadmapGatingTest {

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    private static final List<String> JOB_SLUGS = List.of("translator", "cooks-restaurant", "software-developer");
    private static final List<String> CATALOG_GETS = List.of("/api/ai-readiness/professions",
            "/api/ai-readiness/professions/search?q=translator", "/api/ai-readiness/professions/search?q=&category=Management&limit=200",
            "/api/ai-readiness/professions/translator", "/api/ai-readiness/professions/categories",
            "/api/ai-readiness/features", "/api/ai-readiness/questions");
    /** Classpath data files must never be served as static content. */
    private static final List<String> DATA_FILES = List.of("/ai-readiness/professions.json",
            "/ai-readiness/professions-onet.json", "/ai-readiness/general-roadmap.json", "/general-roadmap.json",
            "/professions-onet.json", "/static/ai-readiness/general-roadmap.json", "/softskills-roadmaps.json");

    @WebMvcTest(controllers = {AiReadinessController.class, JobPageController.class})
    @Import({SecurityConfig.class, BillingSettings.class, EntitlementService.class, AiReadinessCatalog.class,
            AiReadinessScorer.class, AiReadinessService.class, AiReadinessCoach.class, LlmRateLimiter.class,
            JobPages.class})
    @TestPropertySource(properties = "app.public-base-url=https://futureproof.example")
    abstract static class Base {
        @Autowired
        MockMvc mvc;
        @Autowired
        AiReadinessCatalog catalog;
        @Autowired
        JobPages pages;
        @MockBean
        JwtService jwtService;
        @MockBean
        AppUserRepository appUserRepository;
        @MockBean
        PurchaseRepository purchaseRepository;
        @MockBean
        TeamService teamService;

        String body(MockHttpServletResponse r) throws Exception {
            return r.getContentAsString(StandardCharsets.UTF_8);
        }

        String page(String slug) throws Exception {
            return body(mvc.perform(get("/jobs/" + slug)).andReturn().getResponse());
        }

        Profession profession(String slug) {
            return pages.bySlug(slug).orElseThrow();
        }

        /** As Thymeleaf th:text escapes (only the five markup characters). */
        static String esc(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        }

        /** Items and projects of the given phases (HTML-escaped too, as Thymeleaf renders them). */
        static Set<String> secrets(List<RoadmapPhase> phases) {
            Set<String> out = new LinkedHashSet<>();
            for (RoadmapPhase ph : phases) {
                List<String> raw = new ArrayList<>(ph.items());
                if (ph.project() != null) {
                    raw.add(ph.project());
                }
                for (String s : raw) {
                    out.add(s);
                    out.add(esc(s));
                }
            }
            return out;
        }

        /** Every profession's phase items/projects and every general-roadmap item (long enough to be distinctive). */
        Set<String> allSecrets() {
            Set<String> out = new LinkedHashSet<>();
            catalog.professions().forEach(p -> out.addAll(secrets(p.roadmap() == null ? List.of() : p.roadmap())));
            catalog.generalRoadmap().forEach(s -> out.addAll(s.items()));
            out.removeIf(s -> s.length() < 20);
            return out;
        }
    }

    @Nested
    @TestPropertySource(properties = "billing.enabled=true")
    class BillingOn extends Base {

        @Test
        void jobPagesShowPhaseOneInFullLaterPhasesAsTitlesAndTheLockedCard() throws Exception {
            for (String slug : JOB_SLUGS) {
                Profession p = profession(slug);
                String html = page(slug);
                for (RoadmapPhase ph : p.roadmap()) {
                    assertThat(html).as(slug).contains("<h3>" + esc(ph.title()) + "</h3>");
                }
                RoadmapPhase first = p.roadmap().get(0);
                List<String> visible = new ArrayList<>(first.items());
                visible.add(first.project());
                for (String v : visible) {
                    assertThat(html).as("%s phase 1 shows '%s'", slug, v).contains(esc(v));
                }
                assertThat(html).contains("Your full 3-phase plan with projects: €19");
                assertThat(html).contains("href=\"/ai-test?profession=" + p.id() + "\"");
                assertThat(html.split("class=\"project\"", -1).length - 1).as("only phase 1 has a project").isEqualTo(1);
                for (String secret : secrets(p.roadmap().subList(1, p.roadmap().size()))) {
                    assertThat(html).as("%s leaks '%s'", slug, secret).doesNotContain(secret);
                }
            }
        }

        @Test
        void sampledJobPagesLeakNothingBeyondPhaseOne() throws Exception {
            List<Profession> all = catalog.professions();
            int checked = 0;
            for (int i = 0; i < all.size(); i += 45) {
                Profession p = all.get(i);
                if (pages.bySlug(pages.slugOf(p.id())).isEmpty() || p.roadmap() == null || p.roadmap().size() < 2) {
                    continue;
                }
                String html = page(pages.slugOf(p.id()));
                Set<String> visible = secrets(p.roadmap().subList(0, 1));
                for (String secret : secrets(p.roadmap().subList(1, p.roadmap().size()))) {
                    if (!visible.contains(secret)) {
                        assertThat(html).as("%s leaks '%s'", p.id(), secret).doesNotContain(secret);
                    }
                }
                checked++;
            }
            assertThat(checked).isGreaterThan(10);
        }

        @Test
        void catalogGetsSitemapAndRobotsCarryNoRoadmapStrings() throws Exception {
            Set<String> secrets = allSecrets();
            List<String> urls = new ArrayList<>(CATALOG_GETS);
            urls.addAll(List.of("/jobs", "/sitemap.xml", "/robots.txt"));
            for (String url : urls) {
                String body = body(mvc.perform(get(url)).andReturn().getResponse());
                assertThat(body).as(url).isNotEmpty();
                for (String secret : secrets) {
                    assertThat(body).as("%s leaks '%s'", url, secret).doesNotContain(secret);
                }
            }
        }

        @Test
        void assessCarriesOnlyTheFirstPhaseTeaser() throws Exception {
            Profession translator = catalog.profession("translator").orElseThrow();
            String body = body(mvc.perform(post("/api/ai-readiness/assess").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"professionId\":\"translator\",\"answers\":{},\"personalize\":false}")).andReturn().getResponse());
            Set<String> allowed = secrets(translator.roadmap().subList(0, 1));
            assertThat(body).contains(translator.roadmap().get(0).items().get(0));
            for (String secret : allSecrets()) {
                if (!allowed.contains(secret)) {
                    assertThat(body).as("/assess leaks '%s'", secret).doesNotContain(secret);
                }
            }
        }

        @Test
        void dataFilesAreNotServed() throws Exception {
            // the app ships no static web folders of its own (META-INF/resources exists only in the swagger-ui webjar)
            for (String dir : List.of("static", "public", "resources", "META-INF/resources")) {
                assertThat(java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources", dir))).as(dir).isFalse();
            }
            for (String url : DATA_FILES) {
                MockHttpServletResponse r = mvc.perform(get(url)).andReturn().getResponse();
                assertThat(r.getStatus()).as(url).isNotEqualTo(200);
                assertThat(body(r)).as(url).doesNotContain("items");
                // not public, and no handler serves it even with a login
                MockHttpServletResponse authed = mvc.perform(get(url).with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("someone@example.com")))
                        .andReturn().getResponse();
                assertThat(authed.getStatus()).as(url + " with login").isEqualTo(404);
            }
        }
    }

    @Nested
    @TestPropertySource(properties = "billing.enabled=false")
    class BillingOff extends Base {

        /**
         * Byte-identical outside the JSON-LD script. Inside it origin/main itself is not byte-stable: JobPages builds
         * the FAQ with Map.of, whose key order changes per JVM start, so the JSON-LD is compared as parsed JSON.
         */
        @Test
        void jobHtmlIsByteIdenticalToOriginMain() throws Exception {
            for (String slug : JOB_SLUGS) {
                String expected = new String(new ClassPathResource("fp66-golden/" + slug + ".html").getContentAsByteArray(), StandardCharsets.UTF_8);
                String actual = new String(mvc.perform(get("/jobs/" + slug)).andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
                assertThat(withoutJsonLd(actual)).as(slug).isEqualTo(withoutJsonLd(expected));
                assertThat(JSON.readTree(jsonLd(actual))).as(slug).isEqualTo(JSON.readTree(jsonLd(expected)));
            }
        }

        private static final String LD_OPEN = "<script type=\"application/ld+json\">";

        private static String jsonLd(String html) {
            int start = html.indexOf(LD_OPEN) + LD_OPEN.length();
            return html.substring(start, html.indexOf("</script>", start));
        }

        private static String withoutJsonLd(String html) {
            return html.replace(jsonLd(html), "");
        }

        @Test
        void assessKeepsTheFullRoadmap() throws Exception {
            Profession translator = catalog.profession("translator").orElseThrow();
            String body = body(mvc.perform(post("/api/ai-readiness/assess").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"professionId\":\"translator\",\"answers\":{},\"personalize\":false}")).andReturn().getResponse());
            assertThat(body).contains(translator.roadmap().get(2).project());
        }
    }
}
