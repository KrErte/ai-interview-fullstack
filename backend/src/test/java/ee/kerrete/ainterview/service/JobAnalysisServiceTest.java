package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JobAnalysisService}.
 * <p>
 * The service does not use {@link OpenAiClient}; it creates its own {@link RestTemplate}
 * inside {@code analyze()}. The AI path is therefore tested by mocking the construction
 * of {@link RestTemplate} with Mockito, so no network calls are ever made.
 */
@ExtendWith(MockitoExtension.class)
class JobAnalysisServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String BASE_URL = "https://openai.test/v1";

    @Mock
    private RoadmapTaskRepository roadmapTaskRepository;
    @Mock
    private JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @Mock
    private TrainingProgressRepository trainingProgressRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JobAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new JobAnalysisService(objectMapper, roadmapTaskRepository,
                jobAnalysisSessionRepository, trainingProgressRepository);
        ReflectionTestUtils.setField(service, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(service, "model", "test-model");
    }

    private static JobAnalysisRequest request(String email, String cv, String jd) {
        JobAnalysisRequest r = new JobAnalysisRequest();
        r.setEmail(email);
        r.setCvText(cv);
        r.setJobDescription(jd);
        return r;
    }

    // ------------------------------------------------------------------
    // Fallback path (no API key) – heuristic scoring
    // ------------------------------------------------------------------
    @Nested
    class FallbackWithoutApiKey {

        @BeforeEach
        void noKey() {
            ReflectionTestUtils.setField(service, "apiKey", "");
        }

        @Test
        void equalLengthCvAndJobDescriptionScores090() {
            JobAnalysisResponse res = service.analyze(request(null, "abcdefghij", "0123456789"));

            // ratio = 1.0 -> 0.4 + 0.5 = 0.9
            assertThat(res.getMatchScore()).isCloseTo(0.9, within(1e-9));
            assertThat(res.getSummary()).contains("90%");
            assertThat(res.getStrengths()).isNotEmpty();
            assertThat(res.getWeaknesses()).isNotEmpty();
            assertThat(res.getMissingSkills()).isNotEmpty();
            assertThat(res.getRoadmap()).hasSize(4);
            assertThat(res.getSuggestedImprovements()).isNotBlank();
        }

        @Test
        void scoreDependsOnLengthRatioRegardlessOfDirection() {
            // ratio 0.5 -> 0.4 + 0.25 = 0.65, symmetric in cv/jd
            double cvShorter = service.analyze(request(null, "x".repeat(50), "y".repeat(100))).getMatchScore();
            double jdShorter = service.analyze(request(null, "x".repeat(100), "y".repeat(50))).getMatchScore();

            assertThat(cvShorter).isCloseTo(0.65, within(1e-9));
            assertThat(jdShorter).isCloseTo(0.65, within(1e-9));
        }

        @Test
        void veryDifferentLengthsApproachLowerBoundOf04() {
            double score = service.analyze(request(null, "x", "y".repeat(10_000))).getMatchScore();
            assertThat(score).isBetween(0.4, 0.41);
        }

        @Test
        void blankCvReturnsDefaultScore03() {
            JobAnalysisResponse res = service.analyze(request(null, "   ", "Java developer"));
            assertThat(res.getMatchScore()).isEqualTo(0.3);
            assertThat(res.getSummary()).contains("30%");
        }

        @Test
        void nullInputsReturnDefaultScore03() {
            JobAnalysisResponse res = service.analyze(request(null, null, null));
            assertThat(res.getMatchScore()).isEqualTo(0.3);
        }

        @Test
        void nullApiKeyAlsoUsesFallbackWithoutHttpCall() {
            ReflectionTestUtils.setField(service, "apiKey", null);
            try (MockedConstruction<RestTemplate> rt = mockConstruction(RestTemplate.class)) {
                JobAnalysisResponse res = service.analyze(request(null, "cv", "jd"));
                assertThat(res.getMatchScore()).isCloseTo(0.9, within(1e-9));
                assertThat(rt.constructed()).isEmpty();
            }
        }

        @Test
        void withoutEmailNothingIsPersisted() {
            service.analyze(request("  ", "cv text", "job text"));
            verifyNoInteractions(roadmapTaskRepository, jobAnalysisSessionRepository, trainingProgressRepository);
        }

        @Test
        void withEmailPersistsRoadmapSessionAndNewTrainingProgress() {
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            service.analyze(request(EMAIL, "abcdefghij", "0123456789"));

            verify(roadmapTaskRepository).deleteByEmail(EMAIL);
            ArgumentCaptor<RoadmapTask> tasks = ArgumentCaptor.forClass(RoadmapTask.class);
            verify(roadmapTaskRepository, times(4)).save(tasks.capture());
            RoadmapTask first = tasks.getAllValues().get(0);
            assertThat(first.getEmail()).isEqualTo(EMAIL);
            assertThat(first.getTaskKey()).isEqualTo("step-1");
            assertThat(first.getOrderIndex()).isEqualTo(1);
            assertThat(first.isCompleted()).isFalse();
            assertThat(tasks.getAllValues().get(3).getTaskKey()).isEqualTo("step-4");

            ArgumentCaptor<JobAnalysisSession> session = ArgumentCaptor.forClass(JobAnalysisSession.class);
            verify(jobAnalysisSessionRepository).save(session.capture());
            assertThat(session.getValue().getEmail()).isEqualTo(EMAIL);
            assertThat(session.getValue().getJobDescription()).isEqualTo("0123456789");
            // 0.9 is normalized to percent
            assertThat(session.getValue().getMatchScore()).isCloseTo(90.0, within(1e-9));
            assertThat(session.getValue().getAiScore()).isEqualTo(0);

            ArgumentCaptor<TrainingProgress> progress = ArgumentCaptor.forClass(TrainingProgress.class);
            verify(trainingProgressRepository).save(progress.capture());
            assertThat(progress.getValue().getEmail()).isEqualTo(EMAIL);
            assertThat(progress.getValue().getTotalJobAnalyses()).isEqualTo(1);
            assertThat(progress.getValue().getLastMatchScore()).isCloseTo(90.0, within(1e-9));
            assertThat(progress.getValue().getLastUpdated()).isNotNull();
        }

        @Test
        void existingTrainingProgressIsIncremented() {
            TrainingProgress existing = TrainingProgress.builder()
                    .email(EMAIL)
                    .totalJobAnalyses(4)
                    .build();
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            service.analyze(request(EMAIL, "", "jd"));

            verify(trainingProgressRepository).save(existing);
            assertThat(existing.getTotalJobAnalyses()).isEqualTo(5);
            assertThat(existing.getLastMatchScore()).isCloseTo(30.0, within(1e-9));
        }

        @Test
        void sessionPersistenceFailureDoesNotBreakAnalysis() {
            when(jobAnalysisSessionRepository.save(any())).thenThrow(new RuntimeException("db down"));
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            JobAnalysisResponse res = service.analyze(request(EMAIL, "cv", "jd"));

            assertThat(res.getMatchScore()).isNotNull();
            verify(trainingProgressRepository).save(any());
        }
    }

    // ------------------------------------------------------------------
    // OpenAI path – RestTemplate construction is mocked
    // ------------------------------------------------------------------
    @Nested
    class WithApiKey {

        @BeforeEach
        void key() {
            ReflectionTestUtils.setField(service, "apiKey", "sk-test");
        }

        private MockedConstruction<RestTemplate> stubHttp(ResponseEntity<String> response) {
            return mockConstruction(RestTemplate.class, (mock, ctx) ->
                    when(mock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                            .thenReturn(response));
        }

        private MockedConstruction<RestTemplate> stubHttpThrowing(RuntimeException ex) {
            return mockConstruction(RestTemplate.class, (mock, ctx) ->
                    when(mock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                            .thenThrow(ex));
        }

        private String chatCompletion(String content) throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("role", "assistant", "content", content)))));
        }

        @Test
        @SuppressWarnings("unchecked")
        void validAiResponseIsMappedAndRequestIsBuiltCorrectly() throws Exception {
            String content = """
                    {"matchScore":0.82,"strengths":["Java"],"weaknesses":["Cloud"],
                     "missingSkills":["Kubernetes"],"roadmap":["Learn K8s","Build project"],
                     "suggestedImprovements":"Add metrics","summary":"Hea sobivus"}
                    """;
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            try (MockedConstruction<RestTemplate> rt = stubHttp(ResponseEntity.ok(chatCompletion(content)))) {
                JobAnalysisResponse res = service.analyze(request(EMAIL, "my cv", "the job"));

                assertThat(res.getMatchScore()).isEqualTo(0.82);
                assertThat(res.getStrengths()).containsExactly("Java");
                assertThat(res.getWeaknesses()).containsExactly("Cloud");
                assertThat(res.getMissingSkills()).containsExactly("Kubernetes");
                assertThat(res.getRoadmap()).containsExactly("Learn K8s", "Build project");
                assertThat(res.getSuggestedImprovements()).isEqualTo("Add metrics");
                assertThat(res.getSummary()).isEqualTo("Hea sobivus");

                ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
                verify(rt.constructed().get(0)).exchange(
                        eq(BASE_URL + "/chat/completions"), eq(HttpMethod.POST), entity.capture(), eq(String.class));
                HttpHeaders headers = entity.getValue().getHeaders();
                assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer sk-test");
                Map<String, Object> body = (Map<String, Object>) entity.getValue().getBody();
                assertThat(body).containsEntry("model", "test-model");
                assertThat(body.get("messages").toString()).contains("my cv").contains("the job");
            }

            verify(roadmapTaskRepository).deleteByEmail(EMAIL);
            verify(roadmapTaskRepository, times(2)).save(any(RoadmapTask.class));
            ArgumentCaptor<JobAnalysisSession> session = ArgumentCaptor.forClass(JobAnalysisSession.class);
            verify(jobAnalysisSessionRepository).save(session.capture());
            assertThat(session.getValue().getMatchScore()).isCloseTo(82.0, within(1e-9));
            verify(trainingProgressRepository).save(any(TrainingProgress.class));
        }

        @Test
        void baseUrlWithTrailingSlashIsNotDoubled() throws Exception {
            ReflectionTestUtils.setField(service, "baseUrl", BASE_URL + "/");
            try (MockedConstruction<RestTemplate> rt = stubHttp(ResponseEntity.ok(chatCompletion("{\"matchScore\":0.5}")))) {
                service.analyze(request(null, "cv", "jd"));
                verify(rt.constructed().get(0)).exchange(
                        eq(BASE_URL + "/chat/completions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
            }
        }

        @Test
        void percentageScoreAboveOneIsKeptAsIsWhenNormalized() throws Exception {
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok(chatCompletion("{\"matchScore\":75}")))) {
                JobAnalysisResponse res = service.analyze(request(EMAIL, "cv", "jd"));
                assertThat(res.getMatchScore()).isEqualTo(75.0);
            }
            ArgumentCaptor<JobAnalysisSession> session = ArgumentCaptor.forClass(JobAnalysisSession.class);
            verify(jobAnalysisSessionRepository).save(session.capture());
            assertThat(session.getValue().getMatchScore()).isEqualTo(75.0);
            assertThat(session.getValue().getAiScore()).isEqualTo(75);
        }

        @Test
        void scoreAliasIsAcceptedAndMissingFieldsGetDefaults() throws Exception {
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok(chatCompletion("{\"score\":0.6}")))) {
                JobAnalysisResponse res = service.analyze(request(null, "cv", "jd"));
                assertThat(res.getMatchScore()).isEqualTo(0.6);
                assertThat(res.getStrengths()).isEmpty();
                assertThat(res.getWeaknesses()).isEmpty();
                assertThat(res.getMissingSkills()).isEmpty();
                assertThat(res.getRoadmap()).isEmpty();
                assertThat(res.getSuggestedImprovements()).isEmpty();
                // summary has no default
                assertThat(res.getSummary()).isNull();
            }
        }

        @Test
        void missingScoreFallsBackToHeuristicScore() throws Exception {
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok(chatCompletion("{\"summary\":\"x\"}")))) {
                JobAnalysisResponse res = service.analyze(request(null, "", "jd"));
                assertThat(res.getMatchScore()).isEqualTo(0.3);
                assertThat(res.getSummary()).isEqualTo("x");
            }
        }

        @Test
        void blankRoadmapStepsAreSkippedWhenPersisting() throws Exception {
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            String content = "{\"matchScore\":0.7,\"roadmap\":[\"First\",\"  \",\"Second\"]}";
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok(chatCompletion(content)))) {
                service.analyze(request(EMAIL, "cv", "jd"));
            }
            ArgumentCaptor<RoadmapTask> tasks = ArgumentCaptor.forClass(RoadmapTask.class);
            verify(roadmapTaskRepository, times(2)).save(tasks.capture());
            assertThat(tasks.getAllValues()).extracting(RoadmapTask::getTaskKey).containsExactly("step-1", "step-2");
            assertThat(tasks.getAllValues()).extracting(RoadmapTask::getDescription).containsExactly("First", "Second");
        }

        @Test
        void emptyAiRoadmapDoesNotDeleteExistingTasks() throws Exception {
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok(chatCompletion("{\"matchScore\":0.7}")))) {
                service.analyze(request(EMAIL, "cv", "jd"));
            }
            verify(roadmapTaskRepository, never()).deleteByEmail(anyString());
            verify(roadmapTaskRepository, never()).save(any());
            verify(jobAnalysisSessionRepository).save(any());
        }

        @Test
        void malformedJsonContentFallsBack() throws Exception {
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok(chatCompletion("Sorry, I cannot do that")))) {
                JobAnalysisResponse res = service.analyze(request(null, "abcdefghij", "0123456789"));
                assertThat(res.getMatchScore()).isCloseTo(0.9, within(1e-9));
                assertThat(res.getSummary()).startsWith("Lihtne fallback-analüüs");
            }
        }

        @Test
        void nonJsonHttpBodyFallsBack() {
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok("<html>bad gateway</html>"))) {
                JobAnalysisResponse res = service.analyze(request(null, "cv", "jd"));
                assertThat(res.getSummary()).startsWith("Lihtne fallback-analüüs");
            }
        }

        @Test
        void missingChoicesFallsBackAndPersistsEverything() {
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok("{\"choices\":[]}"))) {
                JobAnalysisResponse res = service.analyze(request(EMAIL, "cv", "jd"));
                assertThat(res.getSummary()).startsWith("Lihtne fallback-analüüs");
            }
            verify(roadmapTaskRepository, times(4)).save(any());
            verify(jobAnalysisSessionRepository).save(any());
            verify(trainingProgressRepository).save(any());
        }

        @Test
        void blankContentFallsBack() throws Exception {
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok(chatCompletion("   ")))) {
                JobAnalysisResponse res = service.analyze(request(null, "cv", "jd"));
                assertThat(res.getSummary()).startsWith("Lihtne fallback-analüüs");
            }
        }

        @Test
        void nullBodyFallsBackButOnlyPersistsRoadmap() {
            // Current behavior: this early-return branch skips session + training progress persistence.
            try (MockedConstruction<RestTemplate> ignored = stubHttp(ResponseEntity.ok().body(null))) {
                JobAnalysisResponse res = service.analyze(request(EMAIL, "cv", "jd"));
                assertThat(res.getSummary()).startsWith("Lihtne fallback-analüüs");
            }
            verify(roadmapTaskRepository, times(4)).save(any());
            verifyNoInteractions(jobAnalysisSessionRepository, trainingProgressRepository);
        }

        @Test
        void non2xxStatusFallsBack() {
            try (MockedConstruction<RestTemplate> ignored =
                         stubHttp(ResponseEntity.status(HttpStatus.NO_CONTENT.value() + 100).body("{}"))) {
                JobAnalysisResponse res = service.analyze(request(null, "cv", "jd"));
                assertThat(res.getSummary()).startsWith("Lihtne fallback-analüüs");
            }
        }

        @Test
        void httpClientExceptionFallsBack() {
            when(trainingProgressRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            try (MockedConstruction<RestTemplate> ignored = stubHttpThrowing(new ResourceAccessException("timeout"))) {
                JobAnalysisResponse res = service.analyze(request(EMAIL, "cv", "jd"));
                assertThat(res.getMatchScore()).isCloseTo(0.9, within(1e-9));
                assertThat(res.getSummary()).startsWith("Lihtne fallback-analüüs");
            }
            verify(jobAnalysisSessionRepository).save(any());
            verify(trainingProgressRepository).save(any());
        }
    }
}
