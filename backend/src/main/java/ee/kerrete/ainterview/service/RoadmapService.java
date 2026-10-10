package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapTaskRepository repo;

    @Transactional(readOnly = true)
    public List<RoadmapTaskDto> getTasksForEmail(String email) {
        return repo.findByEmail(email).stream()
                .sorted(Comparator.comparing((RoadmapTask t) -> t.getDayNumber() != null ? t.getDayNumber() : 0)
                        .thenComparing(t -> t.getOrderIndex() != null ? t.getOrderIndex() : 0)
                        .thenComparing(RoadmapTask::getTaskKey))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<RoadmapTaskDto> updateTask(UpdateRoadmapTaskRequest request) {
        RoadmapTask task = repo.findByEmailAndTaskKey(request.getEmail(), request.getTaskKey())
                .orElseThrow(() -> new IllegalArgumentException("Roadmap task not found for given email and key"));

        task.setCompleted(request.isCompleted());
        repo.save(task);

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
