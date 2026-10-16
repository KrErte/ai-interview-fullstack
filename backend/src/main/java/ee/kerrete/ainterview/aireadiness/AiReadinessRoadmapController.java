package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.RoadmapModels.RoadmapRequest;
import ee.kerrete.ainterview.aireadiness.RoadmapModels.RoadmapResponse;
import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.billing.Feature;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The full AI roadmap (FP-60). Requires a login (explicit matcher in SecurityConfig, ahead of the public
 * /api/ai-readiness/**) and, when billing.enabled=true, a paid roadmap entitlement (402 payment_required).
 */
@RestController
public class AiReadinessRoadmapController {

    private final AiReadinessService service;
    private final EntitlementService entitlements;

    public AiReadinessRoadmapController(AiReadinessService service, EntitlementService entitlements) {
        this.service = service;
        this.entitlements = entitlements;
    }

    @PostMapping("/api/ai-readiness/roadmap")
    public RoadmapResponse roadmap(@Valid @RequestBody RoadmapRequest request) {
        // validation first (400 for bad input or an unknown profession), then the 402 entitlement check
        RoadmapResponse roadmap = service.roadmap(request.professionId(), request.answers());
        entitlements.require(Feature.ROADMAP, CurrentUser.email());
        return roadmap;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String field = e.getBindingResult().getFieldError() == null ? "request"
                : e.getBindingResult().getFieldError().getField();
        return ResponseEntity.badRequest().body(Map.of("error", "invalid " + field));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "malformed request body"));
    }
}
