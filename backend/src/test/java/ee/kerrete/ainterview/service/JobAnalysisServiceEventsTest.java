package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.mockito.MockedConstruction;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.model.ProductEventType;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.mockConstruction;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * plan_generated is published (ProductEventOccurred) when a job analysis creates the user's roadmap tasks.
 * No OpenAI key is set, so the deterministic fallback (4 roadmap steps) is used; no network.
 */
@ExtendWith(MockitoExtension.class)
class JobAnalysisServiceEventsTest {

    @Mock
    private RoadmapTaskRepository roadmapTaskRepository;
    @Mock
    private JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @Mock
    private TrainingProgressRepository trainingProgressRepository;
    @Mock
    private ApplicationEventPublisher events;

    private JobAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new JobAnalysisService(new ObjectMapper(), roadmapTaskRepository,
                jobAnalysisSessionRepository, trainingProgressRepository, events);
        ReflectionTestUtils.setField(service, "apiKey", "");
    }

    /** FP-20: the no-key fallback has no roadmap, so plan creation runs on a stubbed AI answer (no network). */
    static final String AI_TWO_STEP_ROADMAP = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
            + "\"{\\\"matchScore\\\":70,\\\"roadmap\\\":[\\\"S1\\\",\\\"S2\\\"]}\"}}]}";

    static JobAnalysisResponse analyzeWithAi(JobAnalysisService service, JobAnalysisRequest request) {
        ReflectionTestUtils.setField(service, "apiKey", "sk-test");
        ReflectionTestUtils.setField(service, "baseUrl", "https://openai.invalid/v1");
        ReflectionTestUtils.setField(service, "model", "m");
        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class, (rt, ctx) ->
                when(rt.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(ResponseEntity.ok(AI_TWO_STEP_ROADMAP)))) {
            return service.analyze(request);
        } finally {
            ReflectionTestUtils.setField(service, "apiKey", "");
        }
    }

    private static JobAnalysisRequest request(String email) {
        JobAnalysisRequest r = new JobAnalysisRequest();
        r.setEmail(email);
        r.setCvText("cv");
        r.setJobDescription("jd");
        return r;
    }

    @Test
    void analysisThatCreatesRoadmapRecordsPlanGenerated() {
        analyzeWithAi(service, request("u@example.com"));

        verify(events).publishEvent(new ProductEventOccurred("u@example.com", ProductEventType.PLAN_GENERATED,
                Map.of("source", "job_analysis", "taskCount", 2)));
    }

    @Test
    void noKeyFallbackAnalysisCreatesNoPlanAndRecordsNothing() {
        // FP-20: the fallback has no (invented) roadmap, so no plan and no plan_generated event
        service.analyze(request("u@example.com"));
        verifyNoInteractions(events);
    }

    @Test
    void analysisForUserWithExistingRoadmapRecordsNothing() {
        // FP-7: an existing plan is not overwritten, so no new plan is generated
        when(roadmapTaskRepository.countByEmail("u@example.com")).thenReturn(2L);
        analyzeWithAi(service, request("u@example.com"));
        verifyNoInteractions(events);
    }

    @Test
    void anonymousAnalysisRecordsNothing() {
        analyzeWithAi(service, request(null));
        verifyNoInteractions(events);
    }
}
