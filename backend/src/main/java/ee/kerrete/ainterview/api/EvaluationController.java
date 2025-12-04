package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerResponse;
import ee.kerrete.ainterview.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * CORS preflight (OPTIONS) – et brauser ei karjuks.
     */
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }

    /**
     * Põhiendpoint vastuse hindamiseks.
     * Kasutab EvaluationService → GPT (AiEvaluationClient) + fallback.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluateAnswerResponse> evaluate(
            @Valid @RequestBody EvaluateAnswerRequest request
    ) {
        log.info("Evaluate answer for email='{}', questionId='{}'",
                request.getEmail(), request.getQuestionId());

        EvaluateAnswerResponse response = evaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }
}
