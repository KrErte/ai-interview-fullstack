package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.*;
import ee.kerrete.ainterview.service.RoadmapService;
import ee.kerrete.ainterview.service.TrainerQuestionService;
import ee.kerrete.ainterview.service.TrainingService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ResponseEntity<TrainingProgressResponse> getProgress(@RequestParam(value = "email", required = false) String email) {
        TrainingProgressResponse response = trainingService.getProgress(resolveEmail(email));
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
        request.setEmail(resolveEmail(request.getEmail()));
        validate(request);
        TrainingProgressResponse response = trainingService.updateTaskStatus(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<?> startPlan(@RequestBody TrainingPlanRequest request) {
        String email = resolveEmail(request.getEmail());
        var tasks = roadmapService.savePlan(email, request.getTargetRole(), request.getTasks());
        // refresh progress to capture task counts
        TrainingProgressResponse progress = trainingService.getTrainingProgress(email);
        return ResponseEntity.ok(Map.of("tasks", tasks, "progress", progress));
    }

    @PostMapping("/task/complete")
    public ResponseEntity<TrainingProgressResponse> completeRoadmapTask(@RequestBody UpdateRoadmapTaskRequest request) {
        request.setEmail(resolveEmail(request.getEmail()));
        roadmapService.updateTask(request);
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

    /**
     * Ignores the client-supplied email and always returns the authenticated user's email.
     */
    private String resolveEmail(String ignoredIncoming) {
        return CurrentUser.email();
    }

    private void validate(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
