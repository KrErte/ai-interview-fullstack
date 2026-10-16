package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/ai-readiness/assess with and without taskShares, through JSON and the real SecurityConfig. */
@WebMvcTest(controllers = AiReadinessController.class)
@Import({SecurityConfig.class, ee.kerrete.ainterview.billing.BillingSettings.class, ee.kerrete.ainterview.billing.EntitlementService.class, AiReadinessCatalog.class, AiReadinessScorer.class, AiReadinessService.class,
        AiReadinessCoach.class, LlmRateLimiter.class})
class AiReadinessAssessWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;
    @MockBean
    ee.kerrete.ainterview.billing.PurchaseRepository purchaseRepository;
    @MockBean
    ee.kerrete.ainterview.team.TeamService teamService;

    @Autowired
    AiReadinessCatalog catalog;

    private String request(String taskShares) {
        return "{\"professionId\":\"translator\",\"answers\":{},\"personalize\":false"
                + (taskShares == null ? "" : ",\"taskShares\":" + taskShares) + "}";
    }

    /** All translator tasks at 0 except the named one at 100, as JSON. */
    private String allOn(String taskName) {
        StringBuilder json = new StringBuilder("{");
        catalog.profession("translator").orElseThrow().tasks().forEach(t -> json
                .append(json.length() > 1 ? "," : "")
                .append('"').append(t.name().replace("\"", "\\\"")).append("\":")
                .append(t.name().equals(taskName) ? 100 : 0));
        return json.append('}').toString();
    }

    @Test
    void withoutTaskSharesTheScoreIsTheCatalogScore() throws Exception {
        mvc.perform(post("/api/ai-readiness/assess").contentType(MediaType.APPLICATION_JSON).content(request(null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskExposureScore").value(70))
                .andExpect(jsonPath("$.personalAdjustment").value(0));
    }

    @Test
    void taskSharesChangeTheExposure() throws Exception {
        var tasks = catalog.profession("translator").orElseThrow().tasks();
        var most = tasks.stream().max(java.util.Comparator.comparingDouble(CatalogModels.ProfessionTask::automation)).orElseThrow();
        int expected = (int) Math.round(100 * most.automation());
        mvc.perform(post("/api/ai-readiness/assess").contentType(MediaType.APPLICATION_JSON)
                        .content(request(allOn(most.name()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskExposureScore").value(expected))
                .andExpect(jsonPath("$.personalAdjustment").value(0));
    }

    @Test
    void badTaskSharesAreA400() throws Exception {
        String first = catalog.profession("translator").orElseThrow().tasks().get(0).name().replace("\"", "\\\"");
        mvc.perform(post("/api/ai-readiness/assess").contentType(MediaType.APPLICATION_JSON)
                        .content(request("{\"" + first + "\":100}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Missing share")));
        mvc.perform(post("/api/ai-readiness/assess").contentType(MediaType.APPLICATION_JSON)
                        .content(request("{\"no such task\":100}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Unknown task")));
    }
}
