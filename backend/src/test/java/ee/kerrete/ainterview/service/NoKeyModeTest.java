package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.SkillPlanResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.ai.AiUnavailableException;
import ee.kerrete.ainterview.config.OpenAiProperties;
import ee.kerrete.ainterview.dto.AdaptiveAnalysisRequest;
import ee.kerrete.ainterview.dto.AdaptiveQuestionRequest;
import ee.kerrete.ainterview.dto.CandidatePlanRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerResponse;
import ee.kerrete.ainterview.dto.SkillPlanRequest;
import ee.kerrete.ainterview.dto.SoftSkillQuestionRequest;
import ee.kerrete.ainterview.dto.SoftSkillQuestionResponse;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * FP-20 (a)/(c): without an OpenAI key no service sends a request; AI-only operations throw
 * AiUnavailableException (-> 503), services with a deliberate local fallback use it (fallback=true).
 * Every RestTemplate the services create is a Mockito construction mock, so any outbound call would show
 * up as an interaction.
 */
class NoKeyModeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockedConstruction<RestTemplate> restTemplates;

    @BeforeEach
    void interceptRestTemplates() {
        restTemplates = mockConstruction(RestTemplate.class);
    }

    @AfterEach
    void noOutboundCall() {
        try {
            restTemplates.constructed().forEach(rt -> verifyNoInteractions(rt));
        } finally {
            restTemplates.close();
        }
    }

    private static <T> T withKey(T service, String key) {
        ReflectionTestUtils.setField(service, "apiKey", key);
        ReflectionTestUtils.setField(service, "baseUrl", "https://openai.invalid/v1");
        ReflectionTestUtils.setField(service, "model", "m");
        return service;
    }

    private static OpenAiClient openAiClient(String key) {
        OpenAiProperties props = new OpenAiProperties();
        props.setApiKey(key);
        props.setBaseUrl("https://openai.invalid/v1");
        return new OpenAiClient(props);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "dummy-openai-key"})
    void openAiClientThrowsForEveryOperation(String key) {
        OpenAiClient client = openAiClient(key);

        assertThatThrownBy(() -> client.complete("p")).isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> client.createChatCompletion("s", "u")).isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> client.evaluateAnswer("q", "a")).isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> client.generateQuestionsFromCv("cv", 1, 1)).isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> client.generateSkillBoosterPlan("s", List.of("Java"))).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void nullKeyAlsoCountsAsNoKey() {
        assertThatThrownBy(() -> openAiClient(null).complete("p")).isInstanceOf(AiUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "dummy-openai-key"})
    void exampleAnswerThrows(String key) {
        AIExampleAnswerService service = withKey(new AIExampleAnswerService(objectMapper), key);
        assertThatThrownBy(() -> service.generateExampleStarAnswer("q")).isInstanceOf(AiUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "dummy-openai-key"})
    void adaptiveAnalysisThrows(String key) {
        AiAdaptiveService service = withKey(new AiAdaptiveService(objectMapper), key);
        assertThatThrownBy(() -> service.analyzeAnswer(AdaptiveAnalysisRequest.builder().question("q").answer("a").build()))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void candidatePlanThrows() {
        CandidatePlanService service = new CandidatePlanService(openAiClient(""));
        assertThatThrownBy(() -> service.buildPlan(new CandidatePlanRequest())).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void questionGenerationThrows() {
        QuestionService service = new QuestionService(openAiClient(""));
        assertThatThrownBy(() -> service.generateFromCv("cv", 2, 1)).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void evaluationUsesLocalHeuristicWithFallbackFlag() {
        AiEvaluationClient client = withKey(new AiEvaluationClient(objectMapper), "");
        EvaluateAnswerRequest req = new EvaluateAnswerRequest();
        req.setQuestion("q");
        req.setAnswer("Olukord oli keeruline, ülesanne oli selge, tegevus: refaktoreerisin, tulemus: 30% kiirem");

        assertThatThrownBy(() -> client.evaluateStarAnswer(req)).isInstanceOf(AiUnavailableException.class);
        EvaluateAnswerResponse res = new EvaluationService(client).evaluate(req);
        assertThat(res.isFallback()).isTrue();
        assertThat(res.getScore()).isEqualTo(100);
    }

    @Test
    void adaptiveQuestionUsesFallbackQuestion() {
        AiEvaluationClient client = withKey(new AiEvaluationClient(objectMapper), "");
        assertThat(client.generateAdaptiveQuestion(new AdaptiveQuestionRequest()).isFallback()).isTrue();
    }

    @Test
    void softSkillQuestionUsesQuestionBank() {
        AiAdaptiveService adaptive = withKey(new AiAdaptiveService(objectMapper), "");
        SoftSkillQuestionService service = withKey(
                new SoftSkillQuestionService(objectMapper, adaptive, mock(TrainingTaskRepository.class)), "");

        SoftSkillQuestionResponse res = service.generateQuestion(SoftSkillQuestionRequest.builder()
                .roadmapKey("conflict_management").previousQuestion("q").previousAnswer("a").build());

        assertThat(res.isFallback()).isTrue();
        assertThat(res.getQuestionText()).isNotBlank();
    }

    @Test
    void skillPlanUsesLocalTemplate() {
        SkillPlanService service = new SkillPlanService(openAiClient(""), objectMapper);
        SkillPlanRequest req = new SkillPlanRequest();
        req.setFocusSkills(List.of("Docker"));
        SkillPlanResponse plan = service.buildPlan(req);
        assertThat(plan.getDays()).isNotEmpty();
        // FP-20: the local template plan is flagged like the other deliberate fallbacks
        assertThat(plan.isFallback()).isTrue(); // no outbound call: verified for every RestTemplate in @AfterEach
    }

    @Test
    void skillPlanFromAiIsNotFlaggedAsFallback() {
        OpenAiClient client = mock(OpenAiClient.class);
        when(client.generateSkillBoosterPlan(any(), any())).thenReturn(
                "{\"overallGoal\":\"Docker\",\"days\":[{\"dayNumber\":1,\"title\":\"T\",\"description\":\"D\",\"practiceTask\":\"P\"}]}");
        SkillPlanService service = new SkillPlanService(client, objectMapper);

        SkillPlanResponse plan = service.buildPlan(new SkillPlanRequest());
        assertThat(plan.isFallback()).isFalse();
        assertThat(plan.getOverallGoal()).isEqualTo("Docker");
    }

    @Test
    void skillPlanJsonCarriesTheFallbackFlag() throws Exception {
        SkillPlanResponse plan = new SkillPlanService(openAiClient(""), objectMapper).buildPlan(new SkillPlanRequest());
        assertThat(objectMapper.writeValueAsString(plan)).contains("\"fallback\":true");
    }

    @Test
    void jobAnalysisWithPlaceholderKeyUsesFallbackWithoutCall() {
        JobAnalysisService service = withKey(new JobAnalysisService(objectMapper,
                mock(ee.kerrete.ainterview.repository.RoadmapTaskRepository.class),
                mock(ee.kerrete.ainterview.repository.JobAnalysisSessionRepository.class),
                mock(ee.kerrete.ainterview.repository.TrainingProgressRepository.class),
                mock(org.springframework.context.ApplicationEventPublisher.class)), "dummy-openai-key");
        ee.kerrete.ainterview.dto.JobAnalysisRequest req = new ee.kerrete.ainterview.dto.JobAnalysisRequest();
        req.setCvText("cv");
        req.setJobDescription("jd");

        assertThat(service.analyze(req).isFallback()).isTrue();
        assertThat(restTemplates.constructed()).isEmpty();
    }

    // --- with a key: a failing or empty AI call is "AI unavailable", never error text as the answer ---

    @Test
    void openAiClientFailureIsAiUnavailableNotErrorText() {
        restTemplates.close();
        try (MockedConstruction<RestTemplate> failing = mockConstruction(RestTemplate.class, (rt, ctx) ->
                when(rt.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
                        .thenThrow(new ResourceAccessException("timeout")))) {
            OpenAiClient client = openAiClient("sk-test");
            assertThatThrownBy(() -> client.complete("p")).isInstanceOf(AiUnavailableException.class);
        }
        restTemplates = mockConstruction(RestTemplate.class);
    }

    @Test
    void openAiClientEmptyAnswerIsAiUnavailable() {
        restTemplates.close();
        try (MockedConstruction<RestTemplate> empty = mockConstruction(RestTemplate.class, (rt, ctx) ->
                when(rt.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
                        .thenReturn(ResponseEntity.ok().build()))) {
            OpenAiClient client = openAiClient("sk-test");
            assertThatThrownBy(() -> client.complete("p")).isInstanceOf(AiUnavailableException.class);
        }
        restTemplates = mockConstruction(RestTemplate.class);
    }

    @Test
    void adaptiveAnalysisHttpFailureIsAiUnavailable() {
        restTemplates.close();
        try (MockedConstruction<RestTemplate> failing = mockConstruction(RestTemplate.class, (rt, ctx) ->
                when(rt.postForEntity(anyString(), any(), eq(String.class)))
                        .thenThrow(new ResourceAccessException("timeout")))) {
            AiAdaptiveService service = withKey(new AiAdaptiveService(objectMapper), "sk-test");
            assertThatThrownBy(() -> service.analyzeAnswer(AdaptiveAnalysisRequest.builder().question("q").answer("a").build()))
                    .isInstanceOf(AiUnavailableException.class);
        }
        restTemplates = mockConstruction(RestTemplate.class);
    }

    @Test
    void exampleAnswerHttpFailureIsAiUnavailable() {
        restTemplates.close();
        try (MockedConstruction<RestTemplate> failing = mockConstruction(RestTemplate.class, (rt, ctx) ->
                when(rt.postForEntity(anyString(), any(), eq(String.class)))
                        .thenThrow(new ResourceAccessException("timeout")))) {
            AIExampleAnswerService service = withKey(new AIExampleAnswerService(objectMapper), "sk-test");
            assertThatThrownBy(() -> service.generateExampleStarAnswer("q")).isInstanceOf(AiUnavailableException.class);
        }
        restTemplates = mockConstruction(RestTemplate.class);
    }
}
