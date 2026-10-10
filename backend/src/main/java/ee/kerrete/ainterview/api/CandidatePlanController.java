package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.CandidatePlanRequest;
import ee.kerrete.ainterview.model.LearningPlan;
import ee.kerrete.ainterview.service.CandidatePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Candidate learning plan of the authenticated user (FP-24). The user is always the auth principal;
 * an {@code email} query parameter sent by older clients is ignored.
 */
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidatePlanController {

    private final CandidatePlanService candidatePlanService;

    @PostMapping
    public ResponseEntity<LearningPlan> buildPlan(
            @Validated @RequestBody CandidatePlanRequest request) {
        LearningPlan plan = candidatePlanService.buildPlan(CurrentUser.email(), request);
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/last")
    public ResponseEntity<LearningPlan> getLastPlan() {
        LearningPlan plan = candidatePlanService.getLastPlan(CurrentUser.email());
        if (plan == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(plan);
    }
}
