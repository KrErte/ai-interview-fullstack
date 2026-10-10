package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.service.CandidatePlanService;
import ee.kerrete.ainterview.service.OpenAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-24: /api/plan with the real SecurityConfig and the real (in-memory) CandidatePlanService;
 * only OpenAI is stubbed. Each user only ever sees and replaces their own last plan.
 * <p>
 * Isolation GETs pass {@code ?email=<other user>} so that against the pre-FP-24 controller
 * (required email param + global last plan) these tests fail for the cross-account leak,
 * not a 400 from a missing required parameter.
 */
@WebMvcTest(CandidatePlanController.class)
@Import({SecurityConfig.class, CandidatePlanService.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CandidatePlanWebMvcTest {

    private static final String ALICE = "alice@example.com";
    private static final String BOB = "bob@example.com";
    private static final String ALICE_LOWER = "alice@qa.test";
    private static final String ALICE_UPPER = "ALICE@qa.test";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAiClient openAiClient;
    // dependencies of JwtAuthenticationFilter
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserRepository appUserRepository;

    @BeforeEach
    void stubOpenAi() {
        when(openAiClient.createChatCompletion(anyString(), contains("Alice")))
                .thenReturn("{\"headline\":\"Plan for Alice, alice@example.com, +372 5555 0000\",\"summary\":\"a\",\"steps\":[\"a1\"]}");
        when(openAiClient.createChatCompletion(anyString(), contains("Bob")))
                .thenReturn("{\"headline\":\"Plan for Bob\",\"summary\":\"b\",\"steps\":[\"b1\"]}");
        when(openAiClient.createChatCompletion(anyString(), contains("lower-alice")))
                .thenReturn("{\"headline\":\"Plan for alice@qa.test\",\"summary\":\"a\",\"steps\":[\"a1\"]}");
        when(openAiClient.createChatCompletion(anyString(), contains("upper-alice")))
                .thenReturn("{\"headline\":\"Plan for ALICE@qa.test\",\"summary\":\"A\",\"steps\":[\"A1\"]}");
    }

    private void buildPlan(String user, String cv) throws Exception {
        mockMvc.perform(post("/api/plan").with(user(user).roles("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cvText\":\"" + cv + "\",\"goal\":\"Java developer\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotSeeAnotherUsersPlan() throws Exception {
        buildPlan(ALICE, "CV of Alice");

        mockMvc.perform(get("/api/plan/last").param("email", BOB).with(user(ALICE).roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Plan for Alice, alice@example.com, +372 5555 0000"));

        // Bob has no plan: 404, not Alice's plan, even when he passes Alice's email like old clients do
        mockMvc.perform(get("/api/plan/last").param("email", ALICE).with(user(BOB).roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotOverwriteAnotherUsersPlan() throws Exception {
        buildPlan(ALICE, "CV of Alice");
        buildPlan(BOB, "CV of Bob");

        mockMvc.perform(get("/api/plan/last").param("email", BOB).with(user(ALICE).roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Plan for Alice, alice@example.com, +372 5555 0000"));
        mockMvc.perform(get("/api/plan/last").param("email", ALICE).with(user(BOB).roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Plan for Bob"));
    }

    @Test
    void differentlyCasedEmailsAreDifferentAccounts() throws Exception {
        buildPlan(ALICE_LOWER, "CV of lower-alice");

        // ALICE@qa.test must not see alice@qa.test's plan (?email= so old code fails for leak, not 400)
        mockMvc.perform(get("/api/plan/last").param("email", ALICE_LOWER).with(user(ALICE_UPPER).roles("CANDIDATE")))
                .andExpect(status().isNotFound());

        // ALICE@qa.test's POST must not overwrite alice@qa.test's plan
        buildPlan(ALICE_UPPER, "CV of upper-alice");

        mockMvc.perform(get("/api/plan/last").param("email", ALICE_UPPER).with(user(ALICE_LOWER).roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Plan for alice@qa.test"));
        mockMvc.perform(get("/api/plan/last").param("email", ALICE_LOWER).with(user(ALICE_UPPER).roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Plan for ALICE@qa.test"));
    }

    @Test
    void noPlanGives404() throws Exception {
        mockMvc.perform(get("/api/plan/last").param("email", ALICE).with(user(ALICE).roles("CANDIDATE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedGets401() throws Exception {
        mockMvc.perform(get("/api/plan/last")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/plan/last").param("email", ALICE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cvText\":\"CV of Alice\",\"goal\":\"g\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(openAiClient);
    }
}
