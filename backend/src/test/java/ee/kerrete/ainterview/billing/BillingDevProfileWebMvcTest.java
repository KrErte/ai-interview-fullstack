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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.ROADMAP_BODY;
import static ee.kerrete.ainterview.billing.BillingWebMvcSupport.user;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dev entitlement (billing on) depends only on billing.dev-entitlement: false (default) means nobody, also under the
 * local profile; true means ADMIN users and BILLING_DEV_ENTITLED_EMAILS, under any profile.
 */
class BillingDevProfileWebMvcTest {

    @WebMvcTest(controllers = {BillingController.class, AiReadinessRoadmapController.class})
    @Import({SecurityConfig.class, BillingSettings.class, EntitlementService.class, BillingExceptionHandler.class,
            AiReadinessCatalog.class, AiReadinessScorer.class, AiReadinessService.class, AiReadinessCoach.class,
            LlmRateLimiter.class})
    abstract static class Base {
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

        void known(long id, String email, UserRole role) {
            when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user(id, email, role)));
        }
    }

    @Nested
    @ActiveProfiles("local")
    @TestPropertySource(properties = "billing.enabled=true")
    class DefaultOffEvenUnderLocal extends Base {
        @Test
        @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
        void adminIsNotEntitled() throws Exception {
            known(1L, "admin@demo.local", UserRole.ADMIN);
            mvc.perform(get("/api/billing/entitlement")).andExpect(jsonPath("$.roadmap").value(false));
            mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                    .andExpect(status().isPaymentRequired());
        }
    }

    @Nested
    @ActiveProfiles("prod-like")
    @TestPropertySource(properties = {"billing.enabled=true", "billing.dev-entitlement=true",
            "billing.dev-entitled-emails=tester@example.com"})
    class SwitchedOnUnderAnyProfile extends Base {
        @Test
        @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
        void adminIsEntitled() throws Exception {
            known(1L, "admin@demo.local", UserRole.ADMIN);
            mvc.perform(get("/api/billing/entitlement")).andExpect(jsonPath("$.roadmap").value(true));
            mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "tester@example.com")
        void listedEmailIsEntitled() throws Exception {
            known(3L, "tester@example.com", UserRole.CANDIDATE);
            mvc.perform(get("/api/billing/entitlement")).andExpect(jsonPath("$.roadmap").value(true));
        }

        @Test
        @WithMockUser(username = "candidate@demo.local")
        void otherCandidateStillNeedsToPay() throws Exception {
            known(2L, "candidate@demo.local", UserRole.CANDIDATE);
            mvc.perform(post("/api/ai-readiness/roadmap").contentType(MediaType.APPLICATION_JSON).content(ROADMAP_BODY))
                    .andExpect(status().isPaymentRequired());
        }
    }
}
