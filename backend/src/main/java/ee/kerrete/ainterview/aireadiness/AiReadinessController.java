package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentRequest;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.CategorySummary;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.Features;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.ProfessionSummary;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import ee.kerrete.ainterview.billing.EntitlementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final EntitlementService entitlements;

    public AiReadinessController(AiReadinessService service, EntitlementService entitlements) {
        this.service = service;
        this.entitlements = entitlements;
    }

    @GetMapping("/professions")
    public List<ProfessionSummary> professions() {
        return service.professions();
    }

    @GetMapping("/professions/search")
    public List<ProfessionSummary> searchProfessions(@RequestParam(name = "q", required = false) String query,
                                                     @RequestParam(name = "category", required = false) String category,
                                                     @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return service.search(query, category, limit);
    }

    @GetMapping("/professions/{id}")
    public ResponseEntity<ProfessionSummary> profession(@PathVariable String id) {
        return ResponseEntity.of(service.summary(id));
    }

    @GetMapping("/professions/categories")
    public List<CategorySummary> professionCategories() {
        return service.categories();
    }

    @GetMapping("/features")
    public Features features() {
        return service.features().withBillingEnabled(entitlements.billingEnabled());
    }

    @GetMapping("/questions")
    public List<Question> questions() {
        return service.questions();
    }

    @PostMapping("/assess")
    public AssessmentResult assess(@Valid @RequestBody AssessmentRequest request) {
        AssessmentResult result = service.assess(request);
        // FP-60: EntitlementService decides; billing on -> always only the teaser (billingEnabled:true), even for
        // paying users (they use POST /roadmap); billing off -> unchanged shape with billingEnabled:false
        return entitlements.lockPublicAssessment() ? result.locked() : result;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
