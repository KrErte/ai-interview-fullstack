package ee.kerrete.ainterview.service;

import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.mockito.MockedConstruction;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.ProductEvent;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.ProductEventRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-10 (QA): product analytics must never break or roll back the user's action.
 * <p>
 * Real Spring proxies, real transactions (no surrounding test transaction) and the real Liquibase schema
 * on in-memory H2. A failing event write is produced by renaming the product_event table at runtime,
 * which is what QA did on Postgres ("product_event dropped").
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp10-isolation;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({RoadmapService.class, ProductEventService.class, JobAnalysisService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProductEventIsolationIntegrationTest {

    @Autowired
    RoadmapService roadmapService;
    @Autowired
    JobAnalysisService jobAnalysisService;
    @Autowired
    RoadmapTaskRepository roadmapTaskRepository;
    @Autowired
    JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @Autowired
    ProductEventRepository productEventRepository;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;

    String email;

    @BeforeEach
    void newUser() {
        email = "fp10-" + UUID.randomUUID() + "@example.com";
    }

    List<RoadmapTaskDto> twoTasks() {
        return List.of(
                RoadmapTaskDto.builder().taskKey("a").title("A").description("a").dayNumber(1).build(),
                RoadmapTaskDto.builder().taskKey("b").title("B").description("b").dayNumber(2).build());
    }

    UpdateRoadmapTaskRequest complete(String key) {
        UpdateRoadmapTaskRequest req = new UpdateRoadmapTaskRequest();
        req.setEmail(email);
        req.setTaskKey(key);
        req.setCompleted(true);
        return req;
    }

    JobAnalysisRequest analysisRequest() {
        JobAnalysisRequest req = new JobAnalysisRequest();
        req.setEmail(email);
        req.setCvText("Java developer with Spring Boot experience");
        req.setJobDescription("Senior Java developer, Spring Boot, Kubernetes");
        return req;
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

    List<String> eventTypes() {
        return productEventRepository.findByEmailOrderByCreatedAtAsc(email).stream()
                .map(ProductEvent::getEventType).toList();
    }

    /** Runs {@code action} while every product_event write fails (table renamed away). */
    void withBrokenProductEventTable(Runnable action) {
        jdbc.execute("ALTER TABLE product_event RENAME TO product_event_disabled");
        try {
            action.run();
        } finally {
            jdbc.execute("ALTER TABLE product_event_disabled RENAME TO product_event");
        }
    }

    // --- a failing event write never breaks or rolls back the user's action ---

    @Test
    void savePlanStillCommitsAndReturnsWhenEventWriteFails() {
        withBrokenProductEventTable(() -> {
            List<RoadmapTaskDto> result = roadmapService.savePlan(email, "dev", twoTasks());
            assertThat(result).hasSize(2);
        });
        assertThat(roadmapTaskRepository.countByEmail(email)).isEqualTo(2);
        assertThat(eventTypes()).isEmpty();
    }

    @Test
    void updateTaskStillCommitsAndReturnsWhenEventWriteFails() {
        roadmapService.savePlan(email, "dev", twoTasks());

        withBrokenProductEventTable(() -> {
            List<RoadmapTaskDto> result = roadmapService.updateTask(complete("a"));
            assertThat(result).filteredOn(RoadmapTaskDto::isCompleted).extracting(RoadmapTaskDto::getTaskKey)
                    .containsExactly("a");
        });
        assertThat(roadmapTaskRepository.findByEmailAndTaskKey(email, "a").orElseThrow().isCompleted()).isTrue();
        assertThat(eventTypes()).containsExactly("plan_generated");
    }

    @Test
    void analyzeStillPersistsAndReturnsWhenEventWriteFails() {
        withBrokenProductEventTable(() -> {
            JobAnalysisResponse response = analyzeWithAi(jobAnalysisService, analysisRequest());
            assertThat(response).isNotNull();
            assertThat(response.getRoadmap()).isNotEmpty();
            assertThat(roadmapTaskRepository.countByEmail(email)).isEqualTo(response.getRoadmap().size());
        });
        assertThat(jobAnalysisSessionRepository.findAll()).anyMatch(s -> email.equals(s.getEmail()));
        assertThat(eventTypes()).isEmpty();
    }

    // --- normal operation ---

    @Test
    void eventsAreWrittenAfterCommit() {
        roadmapService.savePlan(email, "dev", twoTasks());
        roadmapService.updateTask(complete("a"));
        roadmapService.updateTask(complete("a")); // already completed: no second event

        assertThat(eventTypes()).containsExactly("plan_generated", "task_completed");
    }

    @Test
    void analyzeWithoutSurroundingTransactionStillRecordsPlanGenerated() {
        analyzeWithAi(jobAnalysisService, analysisRequest());

        assertThat(eventTypes()).containsExactly("plan_generated");
        assertThat(productEventRepository.findByEmailOrderByCreatedAtAsc(email).get(0).getDetails())
                .contains("\"source\":\"job_analysis\"");
    }

    @Test
    void noKeyFallbackAnalysisRecordsNoPlanGenerated() {
        // FP-20: empty fallback roadmap -> no plan, no event; the session is still saved
        JobAnalysisResponse response = jobAnalysisService.analyze(analysisRequest());

        assertThat(response.isFallback()).isTrue();
        assertThat(roadmapTaskRepository.countByEmail(email)).isZero();
        assertThat(jobAnalysisSessionRepository.findAll()).anyMatch(s -> email.equals(s.getEmail()));
        assertThat(eventTypes()).isEmpty();
    }

    @Test
    void noPlanGeneratedEventWhenTheBusinessTransactionRollsBack() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.executeWithoutResult(status -> {
            roadmapService.savePlan(email, "dev", twoTasks());
            status.setRollbackOnly();
        });

        assertThat(roadmapTaskRepository.countByEmail(email)).isZero();
        assertThat(eventTypes()).isEmpty();
    }

    @Test
    void noTaskCompletedEventWhenUpdateRollsBack() {
        roadmapService.savePlan(email, "dev", twoTasks());

        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.executeWithoutResult(status -> {
            roadmapService.updateTask(complete("a"));
            status.setRollbackOnly();
        });

        assertThat(roadmapTaskRepository.findByEmailAndTaskKey(email, "a").orElseThrow().isCompleted()).isFalse();
        assertThat(eventTypes()).containsExactly("plan_generated");
    }
}
