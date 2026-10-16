package ee.kerrete.ainterview.billing;

import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.AiReadinessCatalog;
import ee.kerrete.ainterview.aireadiness.AiReadinessCoach;
import ee.kerrete.ainterview.aireadiness.AiReadinessRoadmapController;
import ee.kerrete.ainterview.aireadiness.AiReadinessScorer;
import ee.kerrete.ainterview.aireadiness.AiReadinessService;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.team.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.ME;
import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.ROADMAP_BODY;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FP-60 with billing.enabled unset (default false): no gating, but the roadmap still needs a login. */
@WebMvcTest(controllers = {BillingController.class, AiReadinessRoadmapController.class})
@Import({SecurityConfig.class, BillingSettings.class, EntitlementService.class, BillingExceptionHandler.class,
        AiReadinessCatalog.class, AiReadinessScorer.class, AiReadinessService.class, AiReadinessCoach.class,
        LlmRateLimiter.class})
class BillingDisabledWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;
    @MockBean
    PurchaseRepository purchaseRepository;
    @MockBean
    TeamService teamService;

    @Test
    @WithMockUser(username = ME)
    void entitlementIsTrueWithoutAnyPurchase() throws Exception {
        mvc.perform(get("/api/billing/entitlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadmap").value(true));
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void entitlementStillNeedsLogin() throws Exception {
        mvc.perform(get("/api/billing/entitlement")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = ME)
    void roadmapIsFreeForAnyLoggedInUser() throws Exception {
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionRoadmap.length()").value(3));
    }

    @Test
    void roadmapStillNeedsLogin() throws Exception {
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                .andExpect(status().isUnauthorized());
    }
}
