package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapTaskRepository repo;

    public List<RoadmapTaskDto> getTasksForEmail(String email) {
        return repo.findByEmail(email).stream()
                .sorted(Comparator.comparing((RoadmapTask t) -> t.getDayNumber() != null ? t.getDayNumber() : 0)
                        .thenComparing(t -> t.getOrderIndex() != null ? t.getOrderIndex() : 0)
                        .thenComparing(RoadmapTask::getTaskKey))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<RoadmapTaskDto> updateTask(UpdateRoadmapTaskRequest request) {
        RoadmapTask task = repo.findByEmailAndTaskKey(request.getEmail(), request.getTaskKey())
                .orElseThrow(() -> new IllegalArgumentException("Roadmap task not found for given email and key"));

        task.setCompleted(request.isCompleted());
        repo.save(task);

        return getTasksForEmail(request.getEmail());
    }

    public List<RoadmapTaskDto> savePlan(String email, String targetRole, List<RoadmapTaskDto> tasks) {
        repo.deleteByEmail(email);
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        int order = 1;
        for (RoadmapTaskDto dto : tasks) {
            RoadmapTask task = RoadmapTask.builder()
                    .email(email)
                    .taskKey(dto.getTaskKey() != null ? dto.getTaskKey() : "step-" + order)
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
