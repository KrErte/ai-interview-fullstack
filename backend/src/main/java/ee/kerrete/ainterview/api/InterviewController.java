package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerResponse;
import ee.kerrete.ainterview.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InterviewController {

    private final EvaluationService evaluationService;

    public InterviewController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluateAnswerResponse> evaluate(
            @Valid @RequestBody EvaluateAnswerRequest request) {

        EvaluateAnswerResponse response = evaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }
}
