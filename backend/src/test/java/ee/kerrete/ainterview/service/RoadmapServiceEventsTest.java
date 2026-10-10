package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.ProductEventType;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceEventsTest {

    private static final String EMAIL = "u@example.com";

    @Mock
    private RoadmapTaskRepository repo;
    @Mock
    private ApplicationEventPublisher events;
    @InjectMocks
    private RoadmapService service;

    private static UpdateRoadmapTaskRequest update(boolean completed) {
        UpdateRoadmapTaskRequest r = new UpdateRoadmapTaskRequest();
        r.setEmail(EMAIL);
        r.setTaskKey("step-1");
        r.setCompleted(completed);
        return r;
    }

    private void existingTask(boolean completed) {
        RoadmapTask task = RoadmapTask.builder().email(EMAIL).taskKey("step-1").completed(completed).build();
        when(repo.findByEmailAndTaskKey(EMAIL, "step-1")).thenReturn(Optional.of(task));
    }

    @Test
    void savePlanRecordsPlanGeneratedWithTaskCount() {
        service.savePlan(EMAIL, "dev", List.of(
                RoadmapTaskDto.builder().title("A").build(),
                RoadmapTaskDto.builder().title("B").build()));

        verify(events).publishEvent(new ProductEventOccurred(EMAIL, ProductEventType.PLAN_GENERATED,
                Map.of("source", "training_start", "taskCount", 2)));
    }

    @Test
    void emptyPlanRecordsNothing() {
        service.savePlan(EMAIL, "dev", List.of());
        service.savePlan(EMAIL, "dev", null);
        verifyNoInteractions(events);
    }

    @Test
    void savePlanThatOnlyRepeatsCompletedTasksRecordsNothing() {
        when(repo.findByEmail(EMAIL)).thenReturn(List.of(
                RoadmapTask.builder().email(EMAIL).taskKey("a").completed(true).orderIndex(1).build()));

        service.savePlan(EMAIL, "dev", List.of(RoadmapTaskDto.builder().taskKey("a").title("A").build()));

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void completingTaskRecordsTaskCompleted() {
        existingTask(false);

        service.updateTask(update(true));

        verify(events).publishEvent(new ProductEventOccurred(EMAIL, ProductEventType.TASK_COMPLETED, Map.of("taskKey", "step-1")));
    }

    @Test
    void alreadyCompletedTaskIsNotRecordedTwice() {
        existingTask(true);
        service.updateTask(update(true));
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void uncompletingTaskRecordsNothing() {
        existingTask(true);
        service.updateTask(update(false));
        verify(events, never()).publishEvent(any(Object.class));
    }
}
