package ee.kerrete.ainterview.service;

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
            // fallback plaan, et UI ei jääks tühjaks
            LearningPlan fallback = new LearningPlan();
            fallback.setHeadline("Õppekava genereerimine ebaõnnestus.");
            fallback.setSummary("Proovi uuesti. Võid CV-d ja eesmärki natuke täpsustada.");
            fallback.setSteps(List.of(
                    "Kirjelda CV-s tehnoloogiad punktidena.",
                    "Lisa CV-sse GitHubi link näidisprojektidele.",
                    "Genereeri uus õppeplaan."
            ));
            lastPlan = fallback;
            return fallback;
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
