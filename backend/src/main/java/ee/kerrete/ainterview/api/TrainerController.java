package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerResponse;
import ee.kerrete.ainterview.service.AIExampleAnswerService;
import ee.kerrete.ainterview.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * STAR treeneri REST API:
 * - /api/trainer/evaluate-answer  → GPT hindab vastust
 * - /api/trainer/generate-example → GPT genereerib STAR näidisvastuse
 */
@RestController
@RequestMapping("/api/trainer")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TrainerController {

    private final EvaluationService evaluationService;
    private final AIExampleAnswerService aiExampleAnswerService;

    /**
     * Hinda kasutaja STAR vastust.
     * Kasutab EvaluationService'i, mis omakorda proovib OpenAI kaudu hinnata.
     */
    @PostMapping("/evaluate-answer")
    public ResponseEntity<EvaluateAnswerResponse> evaluateAnswer(
            @Valid @RequestBody EvaluateAnswerRequest request
    ) {
        EvaluateAnswerResponse response = evaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Genereeri AI poolt STAR näidisvastus antud küsimusele.
     * Body: { "question": "Selgita oma kogemust ..." }
     * Tagastab plain text'ina näidisvastuse.
     */
    @PostMapping("/generate-example")
    public ResponseEntity<String> generateExampleAnswer(
            @RequestBody Map<String, String> body
    ) {
        String question = body.getOrDefault("question", "");
        try {
            String example = aiExampleAnswerService.generateExampleStarAnswer(question);
            return ResponseEntity.ok(example);
        } catch (Exception e) {
            log.error("Failed to generate example STAR answer", e);
            return ResponseEntity.status(500)
                    .body("AI näidisvastuse genereerimine ebaõnnestus: " + e.getMessage());
        }
    }
}
