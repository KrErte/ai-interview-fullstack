package ee.kerrete.ainterview.dejavu.service;

import ee.kerrete.ainterview.dejavu.dto.DejaVuPredictionRequest;
import ee.kerrete.ainterview.dejavu.dto.DejaVuPredictionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DejaVuService {

    @Value("classpath:prompts/deja_vu_lite.txt")
    private Resource dejaVuPrompt;

    public DejaVuPredictionResponse predict(DejaVuPredictionRequest request) {
        String prompt = readPrompt();
        List<String> keywords = extractKeywords(request.jobDescription());

        List<String> questions = new ArrayList<>();
        questions.add("Which achievement best maps to this role and why?");
        questions.add("How do you de-risk the top challenge mentioned: " + firstOrFallback(keywords, "the main challenge") + "?");
        questions.add("Tell me about a time you exceeded expectations in a similar context to " + promptSummary(prompt, keywords) + ".");

        return new DejaVuPredictionResponse(questions);
    }

    private String readPrompt() {
        if (dejaVuPrompt == null) {
            return "";
        }
        try {
            return StreamUtils.copyToString(dejaVuPrompt.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] tokens = text.toLowerCase(Locale.ENGLISH).split("\\W+");
        Set<String> seen = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token.length() > 3) {
                seen.add(token);
            }
            if (seen.size() >= 5) {
                break;
            }
        }
        return List.copyOf(seen);
    }

    private String firstOrFallback(List<String> keywords, String fallback) {
        return keywords.isEmpty() ? fallback : keywords.get(0);
    }

    private String promptSummary(String prompt, List<String> keywords) {
        if (!keywords.isEmpty()) {
            return keywords.get(0);
        }
        if (prompt != null && !prompt.isBlank()) {
            return prompt.split("\\R")[0];
        }
        return "the job description";
    }
}

