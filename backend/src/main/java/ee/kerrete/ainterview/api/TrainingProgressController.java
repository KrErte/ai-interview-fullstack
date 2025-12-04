package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.TrainingProgressResponse;
import ee.kerrete.ainterview.dto.TrainingTaskRequest;
import ee.kerrete.ainterview.service.TrainerQuestionService;
import ee.kerrete.ainterview.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Treening-roadmapi REST API.
 *
 *  - GET  /api/training/progress?email=...
 *  - POST /api/training/task        (uuendab ühe taski staatust + tagastab progressi)
 *  - GET  /api/training/next-question (lihtne random treeneri küsimus)
 */
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TrainingProgressController {

    private final TrainingService trainingService;
    private final TrainerQuestionService trainerQuestionService;

    /**
     * Tagastab kasutaja koondprogressi.
     */
    @GetMapping("/progress")
    public ResponseEntity<TrainingProgressResponse> getProgress(@RequestParam String email) {
        TrainingProgressResponse response = trainingService.getProgress(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Uuendab ühe taski staatust ja tagastab uuendatud progressi.
     */
    @PostMapping("/task")
    public ResponseEntity<TrainingProgressResponse> updateTask(
            @Valid @RequestBody TrainingTaskRequest request
    ) {
        TrainingProgressResponse response = trainingService.updateTaskStatus(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Tagastab lihtsa järgmise treeneri küsimuse (MVP jaoks random).
     */
    @GetMapping("/next-question")
    public ResponseEntity<String> getNextQuestion() {
        String q = trainerQuestionService.getRandomQuestion();
        log.info("Next trainer question: {}", q);
        return ResponseEntity.ok(q);
    }
}
