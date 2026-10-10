package ee.kerrete.ainterview.ai;

/**
 * The AI provider cannot produce an answer: no API key is configured, or the call failed / returned
 * nothing usable. Mapped to 503 {"error":"ai_unavailable"} by {@code AiUnavailableAdvice}, so a
 * key-dependent endpoint never answers with a 500 or with error text as if it were the AI answer.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Thrown before any outbound call when no API key is configured. */
    public static AiUnavailableException noApiKey() {
        return new AiUnavailableException("OpenAI API key is not configured");
    }
}
