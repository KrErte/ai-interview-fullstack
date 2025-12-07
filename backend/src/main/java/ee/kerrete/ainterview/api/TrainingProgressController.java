package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.*;
import ee.kerrete.ainterview.service.RoadmapService;
import ee.kerrete.ainterview.service.TrainerQuestionService;
import ee.kerrete.ainterview.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    private final RoadmapService roadmapService;

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
            @Valid @RequestBody TrainingTaskRequest request
    ) {
        request.setEmail(resolveEmail(request.getEmail()));
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

    private String resolveEmail(String incoming) {
        if (incoming != null && !incoming.isBlank()) return incoming;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String principal) {
            return principal;
        }
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        throw new IllegalArgumentException("Email required");
    }
}
