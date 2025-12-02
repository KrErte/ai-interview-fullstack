package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.dto.EvaluateAnswerRequest;
import ee.krerte.aiinterview.dto.EvaluateAnswerResponse;
import ee.krerte.aiinterview.service.EvaluationService;
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
