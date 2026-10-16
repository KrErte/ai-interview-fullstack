package ee.kerrete.ainterview.billing;

import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.AiReadinessCatalog;
import ee.kerrete.ainterview.aireadiness.AiReadinessCoach;
import ee.kerrete.ainterview.aireadiness.AiReadinessRoadmapController;
import ee.kerrete.ainterview.aireadiness.AiReadinessScorer;
import ee.kerrete.ainterview.aireadiness.AiReadinessService;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.team.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.ROADMAP_BODY;
import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.user;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Local profile + billing on: application-local.yml makes ADMIN users entitled without a purchase; others still pay. */
@ActiveProfiles("local")
@WebMvcTest(controllers = {BillingController.class, AiReadinessRoadmapController.class},
        properties = "billing.enabled=true")
@Import({SecurityConfig.class, BillingSettings.class, EntitlementService.class, BillingExceptionHandler.class,
        AiReadinessCatalog.class, AiReadinessScorer.class, AiReadinessService.class, AiReadinessCoach.class,
        LlmRateLimiter.class})
class BillingDevProfileWebMvcTest {

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
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void localAdminIsEntitledWithoutPurchase() throws Exception {
        when(appUserRepository.findByEmail("admin@demo.local")).thenReturn(Optional.of(user(1L, "admin@demo.local", UserRole.ADMIN)));
        mvc.perform(get("/api/billing/entitlement")).andExpect(jsonPath("$.roadmap").value(true));
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "candidate@demo.local")
    void localCandidateStillNeedsToPay() throws Exception {
        when(appUserRepository.findByEmail("candidate@demo.local")).thenReturn(Optional.of(user(2L, "candidate@demo.local", UserRole.CANDIDATE)));
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                .andExpect(status().isPaymentRequired());
    }
}
