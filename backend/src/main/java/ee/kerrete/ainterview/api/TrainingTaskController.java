package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.TrainingTaskRequest;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/training/tasks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TrainingTaskController {

    private final TrainingTaskRepository trainingTaskRepository;

    @PostMapping
    public ResponseEntity<Void> saveOrUpdateTask(@RequestBody TrainingTaskRequest request) {
        String taskKey = request.resolveTaskKey();

        log.info("Save training task email={} taskKey={}", request.getEmail(), taskKey);

        // leia olemasolev või loo uus
        TrainingTask task = trainingTaskRepository
                .findByEmailAndTaskKey(request.getEmail(), taskKey)
                .orElseGet(TrainingTask::new);

        LocalDateTime now = LocalDateTime.now();

        // kui on uus rida, pane createdAt
        if (task.getId() == null) {
            task.setCreatedAt(now);
        }

        task.setEmail(request.getEmail());
        task.setTaskKey(taskKey);

        // salvestame vastuse teksti (DTO.answerText -> entity.answer)
        if (request.getAnswerText() != null && !request.getAnswerText().isBlank()) {
            task.setAnswer(request.getAnswerText());
        }

        // score + scoreUpdated
        if (request.getScore() != null) {
            task.setScore(request.getScore());
            task.setScoreUpdated(now);
        }

        // completed (DTO Boolean -> entity boolean)
        if (request.getCompleted() != null) {
            task.setCompleted(request.getCompleted());
        }

        task.setUpdatedAt(now);

        trainingTaskRepository.save(task);

        return ResponseEntity.ok().build();
    }
}
