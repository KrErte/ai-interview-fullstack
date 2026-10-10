package ee.kerrete.ainterview.ai;

import java.util.Locale;
import java.util.Set;

/**
 * No-key mode (FP-20): when OPENAI_API_KEY is empty, blank or an obvious placeholder, nothing may call
 * OpenAI; AI-only endpoints answer 503 {"error":"ai_unavailable"}.
 */
public final class OpenAiKey {

    /**
     * Values that can never authenticate. "dummy-openai-key" was the old default in application.yml and
     * in several {@code @Value} defaults (removed in FP-20; the property is now empty by default).
     */
    static final Set<String> PLACEHOLDERS = Set.of(
            "dummy-openai-key", "dummy", "changeme", "change-me", "placeholder", "none", "null", "todo",
            "xxx", "sk-xxx", "your-api-key", "your_api_key", "your-openai-api-key", "your_openai_api_key");

    private OpenAiKey() {
    }

    public static boolean isConfigured(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        String key = apiKey.trim().toLowerCase(Locale.ROOT);
        if (PLACEHOLDERS.contains(key)) {
            return false;
        }
        // unresolved property placeholder ("${OPENAI_API_KEY}") or template value ("<your key>")
        return !(key.startsWith("${") || key.startsWith("<"));
    }

    /** @throws AiUnavailableException when no key is configured */
    public static void require(String apiKey) {
        if (!isConfigured(apiKey)) {
            throw AiUnavailableException.noApiKey();
        }
    }
}
