package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the roadmap save path against a real (in-memory H2, Liquibase schema) database.
 * <p>
 * The test methods deliberately run WITHOUT a surrounding test transaction
 * ({@code Propagation.NOT_SUPPORTED}), so the services' own transaction handling is exercised.
 * Before FP-7 a second save threw InvalidDataAccessApiUsageException ("No EntityManager with actual
 * transaction available") because deleteByEmail ran outside a transaction.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp7-roadmap;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({RoadmapService.class, ProductEventService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RoadmapSavePathIntegrationTest {

    @Autowired
    private RoadmapService roadmapService;
    @Autowired
    private RoadmapTaskRepository roadmapTaskRepository;
    @Autowired
    private JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @Autowired
    private TrainingProgressRepository trainingProgressRepository;
    @Autowired
    private ApplicationEventPublisher events;

    private JobAnalysisService jobAnalysisService;
    private String email;

    @BeforeEach
    void setUp() {
        email = "fp7-" + UUID.randomUUID() + "@example.com";
        // No OpenAI key configured -> deterministic fallback analysis with a 4-step roadmap.
        jobAnalysisService = new JobAnalysisService(new ObjectMapper(), roadmapTaskRepository,
                jobAnalysisSessionRepository, trainingProgressRepository, events);
    }

    private static RoadmapTaskDto dto(String key, String title, int day) {
        return RoadmapTaskDto.builder().taskKey(key).title(title).description(title + " desc").dayNumber(day).build();
    }

    private void complete(String key) {
        UpdateRoadmapTaskRequest req = new UpdateRoadmapTaskRequest();
        req.setEmail(email);
        req.setTaskKey(key);
        req.setCompleted(true);
        roadmapService.updateTask(req);
    }

    private List<RoadmapTask> tasksInDb() {
        return roadmapTaskRepository.findByEmail(email).stream()
                .sorted(Comparator.comparing(RoadmapTask::getOrderIndex))
                .toList();
    }

    private JobAnalysisRequest analysisRequest() {
        JobAnalysisRequest r = new JobAnalysisRequest();
        r.setEmail(email);
        r.setCvText("Java developer with Spring experience");
        r.setJobDescription("Looking for a Java developer");
        return r;
    }

    // ---------------- RoadmapService.savePlan ----------------

    @Test
    void savePlanOnEmptyRoadmapCreatesTasks() {
        List<RoadmapTaskDto> result = roadmapService.savePlan(email, "dev",
                List.of(dto("a", "A", 1), dto(null, "B", 2)));

        assertThat(result).extracting(RoadmapTaskDto::getTaskKey).containsExactly("a", "step-1");
        assertThat(result).noneMatch(RoadmapTaskDto::isCompleted);
    }

    @Test
    void secondSavePlanKeepsCompletedTasksAndReplacesPendingOnes() {
        roadmapService.savePlan(email, "dev", List.of(dto("a", "A", 1), dto("b", "B", 2), dto("c", "C", 3)));
        complete("a");

        List<RoadmapTaskDto> result = roadmapService.savePlan(email, "dev",
                List.of(dto("x", "X", 1), dto("y", "Y", 2)));

        assertThat(result).extracting(RoadmapTaskDto::getTaskKey).containsExactlyInAnyOrder("a", "x", "y");
        assertThat(tasksInDb()).extracting(RoadmapTask::getTaskKey).containsExactly("a", "x", "y");
        RoadmapTask kept = roadmapTaskRepository.findByEmailAndTaskKey(email, "a").orElseThrow();
        assertThat(kept.isCompleted()).isTrue();
        assertThat(kept.getTitle()).isEqualTo("A");
    }

    @Test
    void newTaskWithSameKeyAsCompletedTaskIsSkippedAndGeneratedKeysDoNotCollide() {
        roadmapService.savePlan(email, "dev", List.of(dto(null, "First", 1), dto(null, "Second", 2)));
        complete("step-1");

        roadmapService.savePlan(email, "dev",
                List.of(dto("step-1", "Duplicate of completed", 1), dto(null, "New 1", 1), dto(null, "New 2", 2)));

        List<RoadmapTask> tasks = tasksInDb();
        assertThat(tasks).extracting(RoadmapTask::getTaskKey).containsExactly("step-1", "step-2", "step-3");
        assertThat(tasks.get(0).isCompleted()).isTrue();
        assertThat(tasks.get(0).getTitle()).isEqualTo("First");
        assertThat(tasks).extracting(RoadmapTask::getTitle).containsExactly("First", "New 1", "New 2");
        // every key is unique -> findByEmailAndTaskKey stays unambiguous
        assertThat(roadmapTaskRepository.findByEmailAndTaskKey(email, "step-2")).isPresent();
    }

    @Test
    void savePlanWithEmptyOrNullTasksLeavesExistingPlanUntouched() {
        roadmapService.savePlan(email, "dev", List.of(dto("a", "A", 1), dto("b", "B", 2)));

        assertThat(roadmapService.savePlan(email, "dev", List.of())).hasSize(2);
        assertThat(roadmapService.savePlan(email, "dev", null)).hasSize(2);
        assertThat(roadmapTaskRepository.countByEmail(email)).isEqualTo(2);
    }

    // ---------------- JobAnalysisService (analysis -> roadmap) ----------------

    @Test
    void firstAnalysisCreatesInitialRoadmap() {
        jobAnalysisService.analyze(analysisRequest());

        List<RoadmapTask> tasks = tasksInDb();
        assertThat(tasks).hasSize(4);
        assertThat(tasks).extracting(RoadmapTask::getTaskKey).containsExactly("step-1", "step-2", "step-3", "step-4");
    }

    @Test
    void secondAnalysisDoesNotFailAndDoesNotOverwriteExistingPlan() {
        roadmapService.savePlan(email, "dev", List.of(dto("own-1", "My plan task", 1), dto("own-2", "Other", 2)));
        complete("own-1");

        jobAnalysisService.analyze(analysisRequest());
        jobAnalysisService.analyze(analysisRequest());

        List<RoadmapTask> tasks = tasksInDb();
        assertThat(tasks).extracting(RoadmapTask::getTaskKey).containsExactly("own-1", "own-2");
        assertThat(tasks.get(0).isCompleted()).isTrue();
        assertThat(tasks.get(0).getTitle()).isEqualTo("My plan task");
    }

    @Test
    void repeatedAnalysisAfterCompletingTaskKeepsProgress() {
        jobAnalysisService.analyze(analysisRequest());
        complete("step-2");

        jobAnalysisService.analyze(analysisRequest());

        List<RoadmapTask> tasks = tasksInDb();
        assertThat(tasks).hasSize(4);
        assertThat(tasks).filteredOn(RoadmapTask::isCompleted).extracting(RoadmapTask::getTaskKey)
                .containsExactly("step-2");
    }
}
