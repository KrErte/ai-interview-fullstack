package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.dto.CandidatePlanRequest;
import ee.krerte.aiinterview.model.LearningPlan;
import ee.krerte.aiinterview.service.CandidatePlanService;
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
