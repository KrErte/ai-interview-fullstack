package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.dto.EvaluateAnswerRequest;
import ee.krerte.aiinterview.dto.EvaluateAnswerResponse;
import ee.krerte.aiinterview.service.EvaluationService;
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
