package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.LearningPlanDto;
import ee.kerrete.ainterview.dto.SaveLearningPlanRequest;
import ee.kerrete.ainterview.service.LearningPlanService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Learning plans of the authenticated user. The user is always taken from the auth principal.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LearningPlanController {

    private final LearningPlanService learningPlanService;
    private final Validator validator;

    /** POST /api/learning-plans: saves a plan for the authenticated user; any email in the body is ignored. */
    @PostMapping("/learning-plans")
    public ResponseEntity<Void> savePlan(@RequestBody SaveLearningPlanRequest request) {
        RequestValidation.withPrincipalEmail(validator, request, SaveLearningPlanRequest::setEmail);
        learningPlanService.savePlan(request);
        return ResponseEntity.ok().build();
    }

    /** GET /api/learning-plans: the authenticated user's last plan. */
    @GetMapping("/learning-plans")
    public ResponseEntity<LearningPlanDto> getMyLastPlan() {
        return toResponse(learningPlanService.getLastPlan(CurrentUser.email()));
    }

    /**
     * Legacy path kept for existing clients: only the user's own email is allowed (403 otherwise).
     * Prefer {@code GET /api/learning-plans}.
     */
    @GetMapping("/learning-plans/{email}")
    public ResponseEntity<LearningPlanDto> getLastPlan(@PathVariable String email) {
        return toResponse(learningPlanService.getLastPlan(CurrentUser.requireSelf(email)));
    }

    private static ResponseEntity<LearningPlanDto> toResponse(Optional<LearningPlanDto> planOpt) {
        return planOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
