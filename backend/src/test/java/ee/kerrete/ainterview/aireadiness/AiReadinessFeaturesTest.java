package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReadinessFeaturesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AiReadinessCatalog CATALOG = new AiReadinessCatalog(MAPPER);

    private static AiReadinessService serviceWithKey(String key) {
        AiReadinessCoach coach = new AiReadinessCoach(MAPPER);
        ReflectionTestUtils.setField(coach, "apiKey", key);
        return new AiReadinessService(CATALOG, new AiReadinessScorer(CATALOG), coach);
    }

    @Test
    void aiCommentIsOffWithoutAKey() {
        assertFalse(serviceWithKey("").features().aiComment());
        assertFalse(serviceWithKey(null).features().aiComment());
        assertFalse(serviceWithKey("dummy-key").features().aiComment());
    }

    @Test
    void aiCommentIsOnWithAKey() {
        assertTrue(serviceWithKey("sk-test").features().aiComment());
    }
}
