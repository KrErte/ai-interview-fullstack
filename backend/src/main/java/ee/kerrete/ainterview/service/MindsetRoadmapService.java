package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.MindsetRoadmapDetail;
import ee.kerrete.ainterview.dto.MindsetRoadmapSummary;
import ee.kerrete.ainterview.dto.MindsetTaskDto;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arvutab mindset-roadmapid olemasolevate TrainingTask kirjade põhjal.
 *
 * Loogika:
 *  - roadmapKey = taskKey prefiks enne esimest "_" märki
 *    nt "conflict_1" -> "conflict"
 *  - iga roadmapi progress = completed / total
 */
@Service
@RequiredArgsConstructor
public class MindsetRoadmapService {

    private final TrainingTaskRepository trainingTaskRepository;

    /**
     * Tagastab kõik mindset-roadmapid (sidebar) koos progressiga.
     */
    public List<MindsetRoadmapSummary> getRoadmapsForEmail(String email) {
        List<TrainingTask> tasks = trainingTaskRepository.findByEmail(email);

        Map<String, List<TrainingTask>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(this::extractRoadmapKey));

        return grouped.entrySet().stream()
                .map(entry -> buildSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MindsetRoadmapSummary::getTitle))
                .collect(Collectors.toList());
    }

    /**
     * Tagastab ühe roadmapi detailid.
     */
    public MindsetRoadmapDetail getRoadmapDetail(String email, String roadmapKey) {
        List<TrainingTask> tasks = trainingTaskRepository.findByEmail(email)
                .stream()
                .filter(t -> roadmapKey.equals(extractRoadmapKey(t)))
                .collect(Collectors.toList());

        MindsetRoadmapSummary summary = buildSummary(roadmapKey, tasks);

        List<MindsetTaskDto> taskDtos = tasks.stream()
                .sorted(Comparator.comparing(TrainingTask::getTaskKey))
                .map(this::toTaskDto)
                .collect(Collectors.toList());

        return MindsetRoadmapDetail.builder()
                .summary(summary)
                .tasks(taskDtos)
                .build();
    }

    /**
     * Võtab taskKey ja arvutab sellest roadmapi võtme (prefix enne "_").
     */
    public String resolveRoadmapKeyFromTaskKey(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return "general";
        }
        int idx = taskKey.indexOf('_');
        if (idx <= 0) {
            return taskKey;
        }
        return taskKey.substring(0, idx);
    }

    /**
     * Sisemine abimeetod TrainingTask põhjal roadmapi võtme saamiseks.
     */
    private String extractRoadmapKey(TrainingTask task) {
        return resolveRoadmapKeyFromTaskKey(task.getTaskKey());
    }

    private MindsetRoadmapSummary buildSummary(String roadmapKey, List<TrainingTask> tasks) {
        int total = tasks.size();
        long completed = tasks.stream()
                .filter(TrainingTask::isCompleted)
                .count();

        int percent = total == 0 ? 0 : (int) Math.round(100.0 * completed / total);

        return MindsetRoadmapSummary.builder()
                .roadmapKey(roadmapKey)
                .title(toTitle(roadmapKey))
                .totalTasks(total)
                .completedTasks((int) completed)
                .progressPercent(percent)
                .build();
    }

    private MindsetTaskDto toTaskDto(TrainingTask task) {
        LocalDateTime updatedAt = task.getUpdatedAt();
        return MindsetTaskDto.builder()
                .taskKey(task.getTaskKey())
                .completed(task.isCompleted())
                .score(task.getScore())
                .updatedAt(updatedAt != null ? updatedAt.toString() : null)
                .build();
    }

    /**
     * Teeb "conflict_management" -> "Conflict Management".
     */
    private String toTitle(String key) {
        if (key == null || key.isBlank()) {
            return "General";
        }
        String normalized = key.replace('-', ' ').replace('_', ' ');
        String[] parts = normalized.split("\\s+");
        return Arrays.stream(parts)
                .filter(p -> !p.isBlank())
                .map(p -> p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
