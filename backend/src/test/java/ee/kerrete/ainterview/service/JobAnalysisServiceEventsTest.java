package ee.kerrete.ainterview.service;

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

    private static JobAnalysisRequest request(String email) {
        JobAnalysisRequest r = new JobAnalysisRequest();
        r.setEmail(email);
        r.setCvText("cv");
        r.setJobDescription("jd");
        return r;
    }

    @Test
    void analysisThatCreatesRoadmapRecordsPlanGenerated() {
        service.analyze(request("u@example.com"));

        verify(events).publishEvent(new ProductEventOccurred("u@example.com", ProductEventType.PLAN_GENERATED,
                Map.of("source", "job_analysis", "taskCount", 4)));
    }

    @Test
    void analysisForUserWithExistingRoadmapRecordsNothing() {
        // FP-7: an existing plan is not overwritten, so no new plan is generated
        when(roadmapTaskRepository.countByEmail("u@example.com")).thenReturn(2L);
        service.analyze(request("u@example.com"));
        verifyNoInteractions(events);
    }

    @Test
    void anonymousAnalysisRecordsNothing() {
        service.analyze(request(null));
        verifyNoInteractions(events);
    }
}
