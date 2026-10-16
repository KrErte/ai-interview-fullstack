package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.RiskBand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReadinessCoachRateLimitTest {

    @Test
    void personalNoteIsSkippedWhenTheLimiterRefuses() {
        AiReadinessCoach coach = new AiReadinessCoach(new ObjectMapper(), LlmRateLimiter.blocking());
        ReflectionTestUtils.setField(coach, "apiKey", "sk-test");
        AssessmentResult result = new AssessmentResult(
                "software-developer", "Software developer", 40, 40, 0, RiskBand.MODERATE, "headline",
                20, "Starting", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 4, null);

        assertTrue(coach.personalNote(result, Map.of("Routine work", "Over 80%")).isEmpty());
    }
}
