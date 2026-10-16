package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.billing.BillingSettings;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.team.TeamService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FP-60: public /assess, catalog GETs and job pages with billing off (unchanged) and on (teaser only). */
class AiReadinessBillingWebMvcTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ASSESS = "{\"professionId\":\"translator\",\"answers\":{},\"personalize\":false}";
    private static final List<String> CATALOG_GETS = List.of("/api/ai-readiness/professions",
            "/api/ai-readiness/professions/search?q=translator", "/api/ai-readiness/professions/translator",
            "/api/ai-readiness/professions/categories", "/api/ai-readiness/features", "/api/ai-readiness/questions");

    @WebMvcTest(controllers = {AiReadinessController.class, JobPageController.class})
    @Import({SecurityConfig.class, BillingSettings.class, AiReadinessCatalog.class, AiReadinessScorer.class,
            AiReadinessService.class, AiReadinessCoach.class, LlmRateLimiter.class, JobPages.class})
    @TestPropertySource(properties = "app.public-base-url=https://futureproof.example")
    abstract static class Base {
        @Autowired
        MockMvc mvc;
        @Autowired
        AiReadinessCatalog catalog;
        @MockBean
        JwtService jwtService;
        @MockBean
        AppUserRepository appUserRepository;
        @MockBean
        TeamService teamService;

        JsonNode assess() throws Exception {
            return JSON.readTree(mvc.perform(post("/api/ai-readiness/assess")
                            .contentType(MediaType.APPLICATION_JSON).content(ASSESS))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }

        void catalogHasNoRoadmap() throws Exception {
            for (String url : CATALOG_GETS) {
                mvc.perform(get(url)).andExpect(status().isOk())
                        .andExpect(content().string(not(containsString("oadmap"))));
            }
        }
    }

    @Nested
    @TestPropertySource(properties = "billing.enabled=false")
    class BillingOff extends Base {

        @Test
        void assessKeepsTheFullRoadmap() throws Exception {
            JsonNode r = assess();
            assertThat(r.get("professionRoadmap").size()).isEqualTo(3);
            assertThat(r.get("generalRoadmap").isArray()).isTrue();
            assertThat(r.get("roadmapLocked").asBoolean()).isFalse();
            assertThat(r.has("roadmapTeaser")).isFalse();
        }

        @Test
        void catalogUnchanged() throws Exception {
            catalogHasNoRoadmap();
        }

        @Test
        void jobPageShowsAllPhases() throws Exception {
            String phase3 = catalog.profession("translator").orElseThrow().roadmap().get(2).project();
            mvc.perform(get("/jobs/translator")).andExpect(status().isOk())
                    .andExpect(content().string(containsString(phase3)))
                    .andExpect(content().string(not(containsString("Phases 2 and 3 are part of"))));
        }
    }

    @Nested
    @TestPropertySource(properties = "billing.enabled=true")
    class BillingOn extends Base {

        @Test
        void assessDropsTheRoadmapAndAddsTheFirstPhaseAsTeaser() throws Exception {
            JsonNode r = assess();
            JsonNode full = JSON.valueToTree(new AiReadinessScorer(catalog)
                    .score(catalog.profession("translator").orElseThrow(), java.util.Map.of()));
            assertThat(r.has("generalRoadmap")).isFalse();
            assertThat(r.has("professionRoadmap")).isFalse();
            assertThat(r.get("roadmapLocked").asBoolean()).isTrue();
            assertThat(r.get("roadmapTeaser")).isEqualTo(full.get("professionRoadmap").get(0));
            // everything else is the same as the unlocked result
            ObjectNode rest = r.deepCopy();
            rest.remove(List.of("roadmapTeaser", "roadmapLocked"));
            ObjectNode fullRest = ((ObjectNode) full).deepCopy();
            fullRest.remove(List.of("generalRoadmap", "professionRoadmap", "roadmapTeaser", "roadmapLocked", "team"));
            assertThat(rest).isEqualTo(fullRest);
        }

        @Test
        void catalogCarriesNoRoadmap() throws Exception {
            catalogHasNoRoadmap();
        }

        @Test
        void jobPageShowsOnlyTheFirstPhase() throws Exception {
            var roadmap = catalog.profession("translator").orElseThrow().roadmap();
            mvc.perform(get("/jobs/translator")).andExpect(status().isOk())
                    .andExpect(content().string(containsString(roadmap.get(0).title())))
                    .andExpect(content().string(not(containsString(roadmap.get(1).project()))))
                    .andExpect(content().string(not(containsString(roadmap.get(2).project()))))
                    .andExpect(content().string(containsString("Phases 2 and 3 are part of")));
        }
    }
}
