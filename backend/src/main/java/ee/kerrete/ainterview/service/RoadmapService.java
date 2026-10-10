package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.api.NotFoundException;
import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.ProductEventType;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapTaskRepository repo;
    /** product analytics: written by ProductEventService after commit, see ProductEventOccurred */
    private final ApplicationEventPublisher events;

    /**
     * Display order of a user's roadmap (FP-25), stable across re-saves:
     * <ol>
     *   <li>{@code orderIndex} ascending: the explicit position. {@link #savePlan} gives new tasks positions
     *   after the highest kept (completed) task, so kept tasks stay first and new tasks follow in request
     *   order. Tasks without a position (legacy rows) count as position 0, i.e. first.</li>
     *   <li>{@code dayNumber} ascending, missing day last.</li>
     *   <li>{@code id} ascending (creation order), then {@code taskKey}, as final tie-breakers.</li>
     * </ol>
     * Ordering by day number first (the old behaviour) put new tasks without a day number before the
     * kept completed tasks.
     */
    static final Comparator<RoadmapTask> DISPLAY_ORDER = Comparator
            .comparing((RoadmapTask t) -> t.getOrderIndex() != null ? t.getOrderIndex() : 0)
            .thenComparing(RoadmapTask::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(RoadmapTask::getId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(RoadmapTask::getTaskKey, Comparator.nullsLast(Comparator.naturalOrder()));

    @Transactional(readOnly = true)
    public List<RoadmapTaskDto> getTasksForEmail(String email) {
        return repo.findByEmail(email).stream()
                .sorted(DISPLAY_ORDER)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * @throws NotFoundException (404) if the user has no task with that key; the same for a key that
     *                           only exists for another user, so existence is not revealed
     */
    @Transactional
    public List<RoadmapTaskDto> updateTask(UpdateRoadmapTaskRequest request) {
        RoadmapTask task = (request.getTaskKey() == null ? java.util.Optional.<RoadmapTask>empty()
                : repo.findByEmailAndTaskKey(request.getEmail(), request.getTaskKey()))
                .orElseThrow(() -> new NotFoundException("Roadmap task not found"));

        boolean wasCompleted = task.isCompleted();
        task.setCompleted(request.isCompleted());
        repo.save(task);

        if (!wasCompleted && request.isCompleted()) {
            events.publishEvent(new ProductEventOccurred(request.getEmail(), ProductEventType.TASK_COMPLETED,
                    Map.of("taskKey", task.getTaskKey() != null ? task.getTaskKey() : "")));
        }

        return getTasksForEmail(request.getEmail());
    }

    /**
     * Saves a new plan for the user in a single transaction.
     * <ul>
     *   <li>Completed tasks are never deleted; they stay in the plan.</li>
     *   <li>Pending (not completed) tasks are replaced by the new ones.</li>
     *   <li>A new task whose key matches a kept completed task is skipped (already done).</li>
     *   <li>An empty or null task list is a no-op: the current plan is returned unchanged.</li>
     * </ul>
     */
    @Transactional
    public List<RoadmapTaskDto> savePlan(String email, String targetRole, List<RoadmapTaskDto> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return getTasksForEmail(email);
        }

        List<RoadmapTask> completed = repo.findByEmail(email).stream()
                .filter(RoadmapTask::isCompleted)
                .toList();
        Set<String> usedKeys = new HashSet<>();
        completed.stream().map(RoadmapTask::getTaskKey).filter(Objects::nonNull).forEach(usedKeys::add);
        int order = completed.stream()
                .map(RoadmapTask::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        repo.deleteByEmailAndCompletedFalse(email);
        repo.flush();

        int generated = 1;
        int created = 0;
        for (RoadmapTaskDto dto : tasks) {
            String key = dto.getTaskKey();
            if (key == null || key.isBlank()) {
                while (usedKeys.contains("step-" + generated)) {
                    generated++;
                }
                key = "step-" + generated;
            } else if (usedKeys.contains(key)) {
                // already completed (or duplicated in the request) - keep the existing one
                continue;
            }
            usedKeys.add(key);
            RoadmapTask task = RoadmapTask.builder()
                    .email(email)
                    .taskKey(key)
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .completed(false)
                    .dayNumber(dto.getDayNumber())
                    .orderIndex(order)
                    .build();
            repo.save(task);
            order++;
            created++;
        }
        if (created > 0) {
            events.publishEvent(new ProductEventOccurred(email, ProductEventType.PLAN_GENERATED,
                    Map.of("source", "training_start", "taskCount", created)));
        }
        return getTasksForEmail(email);
    }

    private RoadmapTaskDto toDto(RoadmapTask task) {
        return RoadmapTaskDto.builder()
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .dayNumber(task.getDayNumber())
                .build();
    }
}
