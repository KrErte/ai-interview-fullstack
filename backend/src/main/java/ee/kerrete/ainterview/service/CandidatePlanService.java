package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.ai.AiUnavailableException;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.CandidatePlanRequest;
import ee.kerrete.ainterview.model.LearningPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatePlanService {

    private final OpenAiClient openAiClient;

    // HOIAN AINULT VIIMAST PLAANI (ilma emailita)
    private volatile LearningPlan lastPlan;

    /**
     * GPT abil õppeplaani loomine + salvestamine.
     */
    public LearningPlan buildPlan(CandidatePlanRequest request) {
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
            lastPlan = plan;
            return plan;
        } catch (Exception e) {
            // FP-20: an unusable AI answer is "AI unavailable" (503), not a "generation failed" plan
            throw new AiUnavailableException("OpenAI learning plan could not be parsed", e);
        }
    }

    /**
     * Kandidaadi VIIMASE plaani tagastus.
     */
    public LearningPlan getLastPlan(String email) {
        // praegu emaili pole vaja — tagastame lihtsalt viimase plaani
        return lastPlan;
    }
}
