package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.ai.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * FP-20: every key-dependent endpoint answers 503 {"error":"ai_unavailable"} when there is no OpenAI key
 * or the AI call failed.
 */
@Slf4j
@RestControllerAdvice
public class AiUnavailableAdvice {

    public static final Map<String, String> BODY = Map.of("error", "ai_unavailable");

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<Map<String, String>> aiUnavailable(AiUnavailableException e) {
        log.warn("AI unavailable: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(BODY);
    }
}
