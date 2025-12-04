package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.CandidatePlanRequest;
import ee.kerrete.ainterview.model.LearningPlan;
import ee.kerrete.ainterview.service.CandidatePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidatePlanController {

    private final CandidatePlanService candidatePlanService;

    @PostMapping
    public ResponseEntity<LearningPlan> buildPlan(
            @Validated @RequestBody CandidatePlanRequest request) {
        LearningPlan plan = candidatePlanService.buildPlan(request);
        return ResponseEntity.ok(plan);  // nüüd kompileerub!
    }

    @GetMapping("/last")
    public ResponseEntity<LearningPlan> getLastPlan(@RequestParam String email) {
        LearningPlan plan = candidatePlanService.getLastPlan(email);
        if (plan == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(plan);
    }
}
