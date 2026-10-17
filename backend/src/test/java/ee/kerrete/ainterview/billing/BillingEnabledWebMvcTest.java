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
import org.junit.jupiter.api.BeforeEach;
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

import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.ME;
import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.ROADMAP_BODY;
import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-60 with billing.enabled=true: entitlement endpoint and the gated roadmap, through the real SecurityConfig.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = {BillingController.class, AiReadinessRoadmapController.class},
        properties = "billing.enabled=true")
@Import({SecurityConfig.class, BillingSettings.class, EntitlementService.class, BillingExceptionHandler.class,
        AiReadinessCatalog.class, AiReadinessScorer.class, AiReadinessService.class, AiReadinessCoach.class,
        LlmRateLimiter.class})
class BillingEnabledWebMvcTest {

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

    @BeforeEach
    void knownUser() {
        when(appUserRepository.findByEmail(ME)).thenReturn(Optional.of(user(7L, ME, UserRole.ADMIN)));
    }

    private void paid(boolean paid) {
        when(purchaseRepository.existsByUserIdAndProductAndStatus(7L, "roadmap", PurchaseStatus.PAID)).thenReturn(paid);
    }

    @Test
    void entitlementNeedsLogin() throws Exception {
        mvc.perform(get("/api/billing/entitlement")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = ME)
    void entitlementFalseWithoutPaidPurchase() throws Exception {
        paid(false);
        mvc.perform(get("/api/billing/entitlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadmap").value(false));
    }

    @Test
    @WithMockUser(username = ME)
    void entitlementTrueWithPaidPurchase() throws Exception {
        paid(true);
        mvc.perform(get("/api/billing/entitlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadmap").value(true));
    }

    @Test
    @WithMockUser(username = "nobody@example.com")
    void unknownUserIsNotEntitled() throws Exception {
        mvc.perform(get("/api/billing/entitlement")).andExpect(jsonPath("$.roadmap").value(false));
    }

    @Test
    @WithMockUser(username = ME)
    void adminIsNotEntitledWithoutTheDevEntitlementSwitch() throws Exception {
        // the user is ADMIN, but billing.dev-entitlement is not set: no dev entitlement
        paid(false);
        mvc.perform(get("/api/billing/entitlement")).andExpect(jsonPath("$.roadmap").value(false));
    }

    @Test
    void roadmapNeedsLoginDespitePublicAiReadinessPaths() throws Exception {
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = ME)
    void roadmapWithoutPaidPurchaseIs402() throws Exception {
        paid(false);
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("payment_required"))
                .andExpect(jsonPath("$.professionRoadmap").doesNotExist());
    }

    @Test
    @WithMockUser(username = ME)
    void roadmapWithPaidPurchaseIs200() throws Exception {
        paid(true);
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionRoadmap.length()").value(3))
                .andExpect(jsonPath("$.professionRoadmap[0].title").isString())
                .andExpect(jsonPath("$.generalRoadmap").isArray())
                .andExpect(jsonPath("$.totalWeeks").isNumber());
    }

    @Test
    @WithMockUser(username = ME)
    void unknownProfessionIs400() throws Exception {
        paid(true);
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\":\"no-such-job\",\"answers\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown profession 'no-such-job'"));
    }

    @Test
    @WithMockUser(username = ME)
    void unknownProfessionIs400BeforeThePaymentCheck() throws Exception {
        paid(false);
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\":\"no-such-job\",\"answers\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown profession 'no-such-job'"));
    }

    @Test
    @WithMockUser(username = ME)
    void overlongProfessionIdIsAGeneric400() throws Exception {
        String longId = "x".repeat(129);
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\":\"" + longId + "\",\"answers\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid professionId"));
    }

    @Test
    @WithMockUser(username = ME)
    void sizeLimitCountsUtf16UnitsSo64EmojiPass() throws Exception {
        // 64 emoji = 128 UTF-16 units: within @Size(max=128) -> unknown profession (validated), not "invalid"
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\":\"" + "\uD83D\uDE00".repeat(64) + "\",\"answers\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.startsWith("Unknown profession")));
        // 65 emoji = 130 units: over the limit
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\":\"" + "\uD83D\uDE00".repeat(65) + "\",\"answers\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid professionId"));
    }

    @Test
    @WithMockUser(username = ME)
    void missingFieldsAndBadJsonAre400() throws Exception {
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid professionId"));
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\":\"translator\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid answers"));
        mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("malformed request body"));
        verify(purchaseRepository, never()).existsByUserIdAndProductAndStatus(any(), anyString(), eq(PurchaseStatus.PAID));
    }
}
