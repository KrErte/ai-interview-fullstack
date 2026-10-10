package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.ai.AiUnavailableException;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.CandidatePlanRequest;
import ee.kerrete.ainterview.model.LearningPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a learning plan from a CV and goal and remembers each user's last plan.
 * <p>
 * FP-24: plans are stored per user, keyed by the exact email of the authenticated principal
 * (no trim, no case folding), matching registration/login and other DB-backed stores. Different
 * casings or surrounding spaces are different accounts and must not share a plan.
 * <p>
 * Storage is in memory (there is no table for these plans yet): plans are lost on restart and not
 * shared between backend instances. The map is bounded (access-order eviction at 10_000 entries)
 * so it cannot grow forever.
 */
@Service
@RequiredArgsConstructor
public class CandidatePlanService {

    private static final int MAX_ENTRIES = 10_000;

    private final OpenAiClient openAiClient;

    private final Map<String, LearningPlan> lastPlanByEmail = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, LearningPlan> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    /**
     * Builds a plan with GPT (or a fallback plan) and stores it as {@code email}'s last plan.
     */
    public LearningPlan buildPlan(String email, CandidatePlanRequest request) {
        String key = key(email);
        String systemPrompt = """
            You are a senior software engineering mentor.

            Based on candidate's CV and target career goal,
            create a professional learning roadmap.

            Return ONLY valid JSON (no markdown, no explanations):
            {
              "headline": "ONE sentence overall plan",
              "summary": "2-4 sentences of encouragement + priority focus",
              "steps": [
                "first concrete step",
                "second concrete step",
                "third concrete step"
              ]
            }
            """;

        String userPrompt = """
            Candidate CV (plain text):
            %s

            Candidate Goal / Target position:
            %s
            """.formatted(
                request.getCvText(),
                request.getGoal()
        );

        String gptResponse = openAiClient.createChatCompletion(systemPrompt, userPrompt);

        try {
            ObjectMapper mapper = new ObjectMapper();
            LearningPlan plan = mapper.readValue(gptResponse, LearningPlan.class);
            lastPlanByEmail.put(key, plan);
            return plan;
        } catch (Exception e) {
            // FP-20: an unusable AI answer is "AI unavailable" (503), not a "generation failed" plan
            throw new AiUnavailableException("OpenAI learning plan could not be parsed", e);
        }
    }

    /**
     * @return {@code email}'s last plan, or {@code null} if that user has none
     */
    public LearningPlan getLastPlan(String email) {
        return lastPlanByEmail.get(key(email));
    }

    /** Exact principal email; null/blank rejected. No trim or case folding. */
    private static String key(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        return email;
    }
}
