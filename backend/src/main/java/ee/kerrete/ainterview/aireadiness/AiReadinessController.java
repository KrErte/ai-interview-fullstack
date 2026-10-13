package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentRequest;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.ProfessionSummary;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Avalik (sisselogimiseta) AI asendatavuse test ja roadmap.
 */
@RestController
@RequestMapping("/api/ai-readiness")
public class AiReadinessController {

    private final AiReadinessService service;

    public AiReadinessController(AiReadinessService service) {
        this.service = service;
    }

    @GetMapping("/professions")
    public List<ProfessionSummary> professions() {
        return service.professions();
    }

    @GetMapping("/professions/search")
    public List<ProfessionSummary> searchProfessions(@RequestParam(name = "q", required = false) String query,
                                                     @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return service.search(query, limit);
    }

    @GetMapping("/questions")
    public List<Question> questions() {
        return service.questions();
    }

    @PostMapping("/assess")
    public AssessmentResult assess(@Valid @RequestBody AssessmentRequest request) {
        return service.assess(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
