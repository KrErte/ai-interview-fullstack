package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.JobPages.InterviewQuestion;
import ee.kerrete.ainterview.aireadiness.MockInterviewCoach.Evaluation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * POST /api/ai-readiness/mock-interview: hinnang ühele vastusele. Küsimus võetakse serverist ameti ja indeksi järgi
 * (sama, mis /prep lehel), kasutaja saadab ainult vastuse. Vastus läheb OpenAI-le, midagi ei salvestata.
 * Võtmeta 503 {"error":"ai_unavailable"} (AiUnavailableAdvice), piiri peal 429.
 */
@RestController
public class MockInterviewController {

    public record EvaluateRequest(
            @NotBlank @Size(max = 128) @Pattern(regexp = "[a-z0-9-]+") String professionId,
            @Min(0) @Max(5) int questionIndex,
            @NotBlank @Size(min = 20, max = 2000) String answer
    ) {
    }

    private final AiReadinessCatalog catalog;
    private final JobPages pages;
    private final MockInterviewCoach coach;

    public MockInterviewController(AiReadinessCatalog catalog, JobPages pages, MockInterviewCoach coach) {
        this.catalog = catalog;
        this.pages = pages;
        this.coach = coach;
    }

    @PostMapping("/api/ai-readiness/mock-interview")
    public ResponseEntity<?> evaluate(@Valid @RequestBody EvaluateRequest request) {
        var profession = catalog.profession(request.professionId()).orElse(null);
        if (profession == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "unknown_profession"));
        }
        List<InterviewQuestion> questions = pages.interviewQuestionsOrGeneric(profession);
        if (request.questionIndex() >= questions.size()) {
            return ResponseEntity.badRequest().body(Map.of("error", "unknown_question"));
        }
        Evaluation evaluation = coach.evaluate(profession.name(), questions.get(request.questionIndex()), request.answer().trim());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(evaluation);
    }

    @ExceptionHandler(MockInterviewCoach.LimitReachedException.class)
    public ResponseEntity<Map<String, String>> limited() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "rate_limited", "message", "Too many scored answers from here. Try again in an hour."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String field = e.getBindingResult().getFieldError() == null ? "request" : e.getBindingResult().getFieldError().getField();
        return ResponseEntity.badRequest().body(Map.of("error", "invalid_" + field));
    }
}
