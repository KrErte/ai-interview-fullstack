package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.ai.AiUnavailableException;
import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.OpenAiProperties;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.service.AIExampleAnswerService;
import ee.kerrete.ainterview.service.AiAdaptiveService;
import ee.kerrete.ainterview.service.CandidatePlanService;
import ee.kerrete.ainterview.service.EvaluationService;
import ee.kerrete.ainterview.service.OpenAiClient;
import ee.kerrete.ainterview.service.QuestionService;
import ee.kerrete.ainterview.service.SoftSkillQuestionService;
import ee.kerrete.ainterview.service.SoftSkillsCoachService;
import ee.kerrete.ainterview.service.TrainingService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-20 (c): key-dependent endpoints answer 503 {"error":"ai_unavailable"} when there is no OpenAI key
 * or the AI call failed (AiUnavailableException + AiUnavailableAdvice), never 500 or error text.
 * Real SecurityConfig, authenticated user.
 */
class AiUnavailableWebMvcTest {

    @TestConfiguration
    @EnableConfigurationProperties(OpenAiProperties.class)
    static class OpenAiPropertiesConfig {
    }

    static final String BODY = "{\"error\":\"ai_unavailable\"}";

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Nested
    @WebMvcTest(controllers = {StarAnswerController.class, TrainerController.class, SkillCoachController.class,
            AdaptiveTrainerController.class, SoftSkillsCoachController.class, CandidatePlanController.class,
            QuestionController.class})
    @Import(SecurityConfig.class)
    @WithMockUser(username = "u@example.com", roles = "USER")
    class WhenTheServiceReportsAiUnavailable {

        @Autowired
        MockMvc mvc;

        @MockBean
        OpenAiClient openAiClient;
        @MockBean
        EvaluationService evaluationService;
        @MockBean
        AIExampleAnswerService aiExampleAnswerService;
        @MockBean
        AiAdaptiveService aiAdaptiveService;
        @MockBean
        TrainingService trainingService;
        @MockBean
        SoftSkillQuestionService softSkillQuestionService;
        @MockBean
        SoftSkillsCoachService softSkillsCoachService;
        @MockBean
        CandidatePlanService candidatePlanService;
        @MockBean
        QuestionService questionService;
        @MockBean
        JwtService jwtService;
        @MockBean
        AppUserRepository appUserRepository;

        private final AiUnavailableException unavailable = AiUnavailableException.noApiKey();

        @Test
        void star() throws Exception {
            when(openAiClient.complete(anyString())).thenThrow(unavailable);
            mvc.perform(json(post("/api/star"), "{\"question\":\"q\",\"cvText\":\"cv\",\"jobDescription\":\"jd\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
        }

        @Test
        void trainerGenerateExample() throws Exception {
            when(aiExampleAnswerService.generateExampleStarAnswer(any())).thenThrow(
                    new AiUnavailableException("OpenAI example answer failed: 500", new RuntimeException("boom")));
            mvc.perform(json(post("/api/trainer/generate-example"), "{\"question\":\"q\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
        }

        @Test
        void skillCoachAnalyzeAndAnswer() throws Exception {
            when(aiAdaptiveService.analyzeAnswer(any())).thenThrow(unavailable);
            for (String path : new String[]{"/api/skill-coach/analyze", "/api/skill-coach/answer"}) {
                mvc.perform(json(post(path), "{\"question\":\"q\",\"answer\":\"a\"}"))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(content().json(BODY, true));
            }
        }

        @Test
        void adaptiveTrainerAnalyze() throws Exception {
            when(aiAdaptiveService.analyzeAnswer(any())).thenThrow(unavailable);
            mvc.perform(json(post("/api/trainer/analyze"), "{\"question\":\"q\",\"answer\":\"a\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
        }

        @Test
        void softSkillsCoachStateAndAnswer() throws Exception {
            SoftSkillsCoachService.OpenAiCoachException coachDown =
                    new SoftSkillsCoachService.OpenAiCoachException("OpenAI API võti puudub.", new IllegalStateException());
            when(softSkillsCoachService.getState(anyString())).thenThrow(coachDown);
            when(softSkillsCoachService.submitAnswer(anyString(), any())).thenThrow(coachDown);

            mvc.perform(get("/api/coach/state"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
            mvc.perform(json(post("/api/coach/answer"), "{\"skillKey\":\"ownership\",\"question\":\"q\",\"answer\":\"a\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
        }

        @Test
        void candidatePlan() throws Exception {
            when(candidatePlanService.buildPlan(any(), any())).thenThrow(unavailable);
            mvc.perform(json(post("/api/plan"), "{\"cvText\":\"cv\",\"goal\":\"dev\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
        }

        @Test
        void questionsFromCv() throws Exception {
            when(questionService.generateFromCv(any(), anyInt(), anyInt())).thenThrow(unavailable);
            mvc.perform(json(post("/api/questions/from-cv"), "{\"cvText\":\"cv\",\"techCount\":2,\"softCount\":1}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
        }
    }

    /** End to end through the real OpenAiClient with an empty key: 503 without any request. */
    @Nested
    @WebMvcTest(controllers = StarAnswerController.class, properties = {
            "openai.api-key=",
            "openai.base-url=http://127.0.0.1:9/v1"
    })
    @Import({SecurityConfig.class, OpenAiClient.class, LlmRateLimiter.class,
            AiUnavailableWebMvcTest.OpenAiPropertiesConfig.class})
    @WithMockUser(username = "u@example.com", roles = "USER")
    class WithTheRealClientAndNoKey {

        @Autowired
        MockMvc mvc;
        @MockBean
        JwtService jwtService;
        @MockBean
        AppUserRepository appUserRepository;

        @Test
        void starAnswersAiUnavailable() throws Exception {
            mvc.perform(json(post("/api/star"), "{\"question\":\"q\",\"cvText\":\"cv\",\"jobDescription\":\"jd\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(BODY, true));
        }
    }
}
