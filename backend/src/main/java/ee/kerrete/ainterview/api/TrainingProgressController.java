package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.*;
import ee.kerrete.ainterview.service.RoadmapService;
import ee.kerrete.ainterview.service.TrainerQuestionService;
import ee.kerrete.ainterview.service.TrainingService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Treening-roadmapi REST API.
 *
 * The user is always the authenticated principal; emails in params/body are ignored.
 *
 *  - GET  /api/training/progress
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
    private final RoadmapService roadmapService;
    private final Validator validator;

    /**
     * Tagastab kasutaja koondprogressi.
     */
    @GetMapping("/progress")
    public ResponseEntity<TrainingProgressResponse> getProgress() {
        TrainingProgressResponse response = trainingService.getProgress(CurrentUser.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Uuendab ühe taski staatust ja tagastab uuendatud progressi.
     */
    @PostMapping("/task")
    public ResponseEntity<TrainingProgressResponse> updateTask(
            @RequestBody TrainingTaskRequest request
    ) {
        // set the email from the principal first, then validate the rest of the request
        RequestValidation.withPrincipalEmail(validator, request, TrainingTaskRequest::setEmail);
        TrainingProgressResponse response = trainingService.updateTaskStatus(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<?> startPlan(@RequestBody TrainingPlanRequest request) {
        String email = CurrentUser.email();
        var tasks = roadmapService.savePlan(email, request.getTargetRole(), request.getTasks());
        // refresh progress to capture task counts
        TrainingProgressResponse progress = trainingService.getTrainingProgress(email);
        return ResponseEntity.ok(Map.of("tasks", tasks, "progress", progress));
    }

    @PostMapping("/task/complete")
    public ResponseEntity<TrainingProgressResponse> completeRoadmapTask(@RequestBody UpdateRoadmapTaskRequest request) {
        request.setEmail(CurrentUser.email());
        roadmapService.updateTask(request); // 404 for an unknown key or another user's key
        TrainingProgressResponse progress = trainingService.getTrainingProgress(request.getEmail());
        return ResponseEntity.ok(progress);
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
