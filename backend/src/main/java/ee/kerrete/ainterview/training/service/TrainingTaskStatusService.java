package ee.kerrete.ainterview.training.service;

import ee.kerrete.ainterview.training.dto.TrainingStatusDto;
import ee.kerrete.ainterview.training.dto.TrainingTaskDto;
import ee.kerrete.ainterview.training.entity.TrainingTaskStatus;
import ee.kerrete.ainterview.training.repository.TrainingTaskStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingTaskStatusService {

    public static final List<String> DEFAULT_TASK_KEYS = List.of(
        "cv-refresh",
        "star-stories",
        "system-design-outline",
        "frontend-performance"
    );

    private static final int TOTAL_TASKS = DEFAULT_TASK_KEYS.size();

    private final TrainingTaskStatusRepository repository;

    @Transactional(readOnly = true)
    public TrainingStatusDto getStatusForUser(String email) {
        String userEmail = requireEmail(email);
        Map<String, TrainingTaskStatus> byKey = repository.findByUserEmail(userEmail).stream()
            .collect(Collectors.toMap(
                TrainingTaskStatus::getTaskKey,
                status -> status,
                (a, b) -> a,
                LinkedHashMap::new
            ));

        List<TrainingTaskDto> tasks = DEFAULT_TASK_KEYS.stream()
            .map(key -> {
                TrainingTaskStatus status = byKey.get(key);
                boolean completed = status != null && status.isCompleted();
                Instant completedAt = status != null ? status.getCompletedAt() : null;
                return TrainingTaskDto.builder()
                    .taskKey(key)
                    .completed(completed)
                    .completedAt(completedAt)
                    .build();
            })
            .toList();

        int completedCount = (int) tasks.stream().filter(TrainingTaskDto::isCompleted).count();
        int progressPercent = TOTAL_TASKS == 0 ? 0 : (int) Math.round(completedCount * 100.0 / TOTAL_TASKS);

        return TrainingStatusDto.builder()
            .totalTasks(TOTAL_TASKS)
            .completedTasks(completedCount)
            .progressPercent(progressPercent)
            .tasks(tasks)
            .build();
    }

    @Transactional
    public TrainingStatusDto setTaskCompletion(String email, String taskKey, boolean completed) {
        String normalizedKey = normalizeTaskKey(taskKey);
        validateTaskKey(normalizedKey);
        String userEmail = requireEmail(email);

        TrainingTaskStatus status = repository.findByUserEmailAndTaskKey(userEmail, normalizedKey)
            .orElseGet(() -> TrainingTaskStatus.builder()
                .userEmail(userEmail)
                .taskKey(normalizedKey)
                .build());

        status.setCompleted(completed);
        status.setCompletedAt(completed ? Instant.now() : null);
        repository.save(status);

        return getStatusForUser(userEmail);
    }

    public Set<String> getSupportedTaskKeys() {
        return Set.copyOf(DEFAULT_TASK_KEYS);
    }

    private String requireEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeTaskKey(String taskKey) {
        if (!StringUtils.hasText(taskKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task key is required");
        }
        return taskKey.trim().toLowerCase();
    }

    private void validateTaskKey(String taskKey) {
        if (!DEFAULT_TASK_KEYS.contains(taskKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown training task: " + taskKey);
        }
    }
}

