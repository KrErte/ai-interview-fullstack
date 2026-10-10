package ee.kerrete.ainterview.service;

import org.springframework.context.ApplicationEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-20 (d): /api/job-analysis always returns matchScore on a 0–100 scale (whole number), the prompt asks
 * for 0–100, and the persisted session / training progress use the same normalized value.
 */
@ExtendWith(MockitoExtension.class)
class JobAnalysisMatchScoreTest {

    private static final String EMAIL = "u@example.com";

    @Mock
    RoadmapTaskRepository roadmapTaskRepository;
    @Mock
    JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @Mock
    TrainingProgressRepository trainingProgressRepository;
    @Mock
    ApplicationEventPublisher events;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JobAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new JobAnalysisService(objectMapper, roadmapTaskRepository, jobAnalysisSessionRepository,
                trainingProgressRepository, events);
        ReflectionTestUtils.setField(service, "apiKey", "sk-test");
        ReflectionTestUtils.setField(service, "baseUrl", "https://openai.invalid/v1");
        ReflectionTestUtils.setField(service, "model", "m");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // only x < 1 is a fraction (x100) ...
            "0.82, 82", "0.825, 83", "0.5, 50", "0.99, 99", "0, 0", "0.004, 0", "-0.3, 0",
            // ... 1 and above are already percent: 1 stays 1 (FP-20 contract fix; was 1 -> 100)
            "1, 1", "1.0, 1", "1.4, 1", "1.5, 2",
            "75, 75", "75.4, 75", "75.5, 76", "100, 100", "150, 100", "-5, 0"
    })
    void normalizeScore(double raw, int expected) {
        assertThat(JobAnalysisService.normalizeScore(raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "normalize(normalize({0})) == normalize({0})")
    @CsvSource({"0.82", "1", "0.004", "55.5", "150", "-5"})
    void normalizeScoreIsIdempotent(double raw) {
        Integer once = JobAnalysisService.normalizeScore(raw);
        assertThat(JobAnalysisService.normalizeScore(once.doubleValue())).isEqualTo(once);
    }

    @Test
    void normalizeScoreHandlesMissingValues() {
        assertThat(JobAnalysisService.normalizeScore(null)).isNull();
        assertThat(JobAnalysisService.normalizeScore(Double.NaN)).isNull();
        assertThat(JobAnalysisService.normalizeScore(Double.POSITIVE_INFINITY)).isNull();
    }

    private JobAnalysisRequest request() {
        JobAnalysisRequest r = new JobAnalysisRequest();
        r.setEmail(EMAIL);
        r.setCvText("cv");
        r.setJobDescription("jd");
        return r;
    }

    private MockedConstruction<RestTemplate> aiReturns(String content) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("role", "assistant", "content", content)))));
        return mockConstruction(RestTemplate.class, (rt, ctx) ->
                when(rt.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(ResponseEntity.ok(body)));
    }

    @ParameterizedTest(name = "AI matchScore {0} -> {1}")
    @CsvSource({"0.734, 73", "73.6, 74", "250, 100", "-1, 0", "1, 1", "83.0, 83"})
    void aiScoreIsReturnedAndPersistedOnTheSameScale(String aiScore, int expected) throws Exception {
        when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        JobAnalysisResponse res;
        try (MockedConstruction<RestTemplate> ignored = aiReturns("{\"matchScore\":" + aiScore + ",\"summary\":\"s\"}")) {
            res = service.analyze(request());
        }

        assertThat(res.getMatchScore()).isEqualTo(expected);
        assertThat(res.isFallback()).isFalse();

        ArgumentCaptor<JobAnalysisSession> session = ArgumentCaptor.forClass(JobAnalysisSession.class);
        verify(jobAnalysisSessionRepository).save(session.capture());
        assertThat(session.getValue().getMatchScore()).isEqualTo((double) expected);
        assertThat(session.getValue().getAiScore()).isEqualTo(expected);
        // the stored JSON uses the same integer as the API response
        assertThat(objectMapper.readTree(session.getValue().getAnalysisResult()).get("matchScore").isInt()).isTrue();
        assertThat(objectMapper.readTree(session.getValue().getAnalysisResult()).get("matchScore").asInt()).isEqualTo(expected);

        ArgumentCaptor<TrainingProgress> progress = ArgumentCaptor.forClass(TrainingProgress.class);
        verify(trainingProgressRepository).save(progress.capture());
        assertThat(progress.getValue().getLastMatchScore()).isEqualTo((double) expected);
    }

    @Test
    void scoreAliasIsNormalizedToo() throws Exception {
        when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        JobAnalysisResponse res;
        try (MockedConstruction<RestTemplate> ignored = aiReturns("{\"score\":0.61}")) {
            res = service.analyze(request());
        }
        assertThat(res.getMatchScore()).isEqualTo(61);
        assertThat(res.isFallback()).isFalse();
    }

    @Test
    void responseJsonHasAnIntegerMatchScore() throws Exception {
        JobAnalysisResponse res = new JobAnalysisResponse();
        res.setMatchScore(83);
        String json = objectMapper.writeValueAsString(res);
        assertThat(json).contains("\"matchScore\":83").doesNotContain("83.0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void promptAsksForZeroToHundred() throws Exception {
        when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        try (MockedConstruction<RestTemplate> rt = aiReturns("{\"matchScore\":80}")) {
            service.analyze(request());
            ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt.constructed().get(0)).exchange(anyString(), eq(HttpMethod.POST), entity.capture(), eq(String.class));
            String messages = ((Map<String, Object>) entity.getValue().getBody()).get("messages").toString();
            assertThat(messages).contains("integer from 0 to 100").doesNotContain("between 0.0 and 1.0");
        }
    }

    @Test
    void fallbackScoreAndSummaryAreOnTheSameScale() {
        ReflectionTestUtils.setField(service, "apiKey", "");
        when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        JobAnalysisResponse res = service.analyze(request()); // equal lengths -> 90

        assertThat(res.isFallback()).isTrue();
        assertThat(res.getMatchScore()).isEqualTo(90);
        assertThat(res.getSummary()).contains("90%");
        assertThat(res.getStrengths()).isEmpty();
        assertThat(res.getWeaknesses()).isEmpty();
        assertThat(res.getMissingSkills()).isEmpty();
        assertThat(res.getRoadmap()).isEmpty();
        assertThat(res.getSuggestedImprovements()).isEmpty();
        ArgumentCaptor<JobAnalysisSession> session = ArgumentCaptor.forClass(JobAnalysisSession.class);
        verify(jobAnalysisSessionRepository).save(session.capture());
        assertThat(session.getValue().getMatchScore()).isEqualTo(90.0);
        assertThat(session.getValue().getAiScore()).isEqualTo(90);
    }
}
