package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-25: roadmap order is stable across re-saves. Kept (completed) tasks stay first, new tasks follow
 * in request order, even when the new tasks have no day number.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp25-order;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({RoadmapService.class, ProductEventService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RoadmapOrderingIntegrationTest {

    @Autowired
    private RoadmapService roadmapService;
    @Autowired
    private RoadmapTaskRepository repo;

    private static RoadmapTaskDto task(String key, Integer day) {
        return RoadmapTaskDto.builder().taskKey(key).title(key).dayNumber(day).build();
    }

    private static List<String> keys(List<RoadmapTaskDto> tasks) {
        return tasks.stream().map(RoadmapTaskDto::getTaskKey).toList();
    }

    private void complete(String email, String key) {
        UpdateRoadmapTaskRequest req = new UpdateRoadmapTaskRequest();
        req.setEmail(email);
        req.setTaskKey(key);
        req.setCompleted(true);
        roadmapService.updateTask(req);
    }

    @Test
    void completedTasksStayFirstAfterResaveWithoutDayNumbers() {
        String email = "order-" + UUID.randomUUID() + "@example.com";
        roadmapService.savePlan(email, "role", List.of(task("d1", 1), task("d2", 2), task("d3", 3)));
        complete(email, "d1");
        complete(email, "d2");

        // new plan: tasks without day numbers (as the job analysis path produces them)
        List<RoadmapTaskDto> result = roadmapService.savePlan(email, "role",
                List.of(task("new-b", null), task("new-a", null), task("new-c", null)));

        assertThat(keys(result)).containsExactly("d1", "d2", "new-b", "new-a", "new-c");
        assertThat(keys(roadmapService.getTasksForEmail(email))).containsExactly("d1", "d2", "new-b", "new-a", "new-c");
    }

    @Test
    void orderIsStableAcrossRepeatedResaves() {
        String email = "order-" + UUID.randomUUID() + "@example.com";
        roadmapService.savePlan(email, "role", List.of(task("a", 5), task("b", 1)));
        complete(email, "a");
        roadmapService.savePlan(email, "role", List.of(task("c", null), task("d", 2)));
        complete(email, "c");
        List<RoadmapTaskDto> result = roadmapService.savePlan(email, "role", List.of(task("e", 1), task("f", null)));

        // explicit position wins over day number: a (kept), c (kept), then the newest tasks in request order
        assertThat(keys(result)).containsExactly("a", "c", "e", "f");
    }

    @Test
    void legacyTasksWithoutPositionComeFirstOrderedByDayThenCreation() {
        String email = "order-" + UUID.randomUUID() + "@example.com";
        repo.save(RoadmapTask.builder().email(email).taskKey("legacy-day2").dayNumber(2).completed(true).build());
        repo.save(RoadmapTask.builder().email(email).taskKey("legacy-noday").completed(true).build());
        repo.save(RoadmapTask.builder().email(email).taskKey("legacy-day1").dayNumber(1).completed(true).build());

        List<RoadmapTaskDto> result = roadmapService.savePlan(email, "role", List.of(task("new", null)));

        assertThat(keys(result)).containsExactly("legacy-day1", "legacy-day2", "legacy-noday", "new");
    }
}
