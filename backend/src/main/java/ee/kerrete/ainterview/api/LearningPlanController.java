package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.LearningPlanDto;
import ee.kerrete.ainterview.dto.SaveLearningPlanRequest;
import ee.kerrete.ainterview.service.LearningPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")   // ⬅ klassi tasemel /api
@RequiredArgsConstructor
public class LearningPlanController {

    private final LearningPlanService learningPlanService;

    // ⬇ MEETODI PATH: /learning-plans  → kokku /api/learning-plans
    @PostMapping("/learning-plans")
    public ResponseEntity<Void> savePlan(@Valid @RequestBody SaveLearningPlanRequest request) {
        learningPlanService.savePlan(request);
        return ResponseEntity.ok().build();
    }

    // GET /api/learning-plans/{email} – hiljem “Minu profiil” jaoks
    @GetMapping("/learning-plans/{email}")
    public ResponseEntity<LearningPlanDto> getLastPlan(@PathVariable String email) {
        Optional<LearningPlanDto> planOpt = learningPlanService.getLastPlan(email);
        return planOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
