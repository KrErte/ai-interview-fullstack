package ee.kerrete.ainterview.softskills.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileDto;
import ee.kerrete.ainterview.softskills.dto.SoftSkillSourceDto;
import ee.kerrete.ainterview.service.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoftSkillMergeAiService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public SoftSkillMergedProfileDto callAi(List<SoftSkillSourceDto> sources, String jobContext, String email) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(sources, jobContext, email);

        String aiResponse = openAiClient.createChatCompletion(systemPrompt, userPrompt);
        if (!StringUtils.hasText(aiResponse)) {
            log.error("Soft skill merge AI response empty");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI response invalid");
        }

        try {
            String cleaned = stripCodeFence(aiResponse.trim());
            return objectMapper.readValue(cleaned, SoftSkillMergedProfileDto.class);
        } catch (Exception ex) {
            log.error("Soft skill merge AI parse failed. Raw response: {}", aiResponse);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI response invalid");
        }
    }

    private String buildSystemPrompt() {
        return """
            You are an AI assistant that merges multiple soft-skill feedback sources into a concise JSON object.
            Return ONLY valid JSON. No markdown, no code fences.
            JSON schema:
            {
              "summary": "2-4 sentence overview",
              "strengths": ["bullet"],
              "risks": ["bullet"],
              "communicationStyle": "short sentence",
              "collaborationStyle": "short sentence",
              "growthAreas": ["bullet"],
              "dimensionScoresMerged": [
                {
                  "dimension": "communication | collaboration | ownership | problem_solving | learning_agility | stress_management | other",
                  "mergedScore": 1-5,
                  "confidence": 0-1,
                  "rationale": "short explanation"
                }
              ],
              "meta": {
                "overallConfidence": 0-1,
                "mainDisagreements": ["short bullets"],
                "notesForCoach": ["short bullets"]
              }
            }
            Keep arrays concise (max 5 items). If data missing, leave arrays empty and strings as "Not provided".
            """;
    }

    private String buildUserPrompt(List<SoftSkillSourceDto> sources, String jobContext, String email) {
        StringBuilder sb = new StringBuilder();
        sb.append("Candidate email: ").append(StringUtils.hasText(email) ? email : "unknown").append("\n");
        if (StringUtils.hasText(jobContext)) {
            sb.append("Job context: ").append(jobContext).append("\n");
        }
        sb.append("Feedback sources to merge:\n");
        if (!CollectionUtils.isEmpty(sources)) {
            int idx = 1;
            for (SoftSkillSourceDto src : sources) {
                sb.append(idx++).append(". [")
                    .append(StringUtils.hasText(src.getSourceType()) ? src.getSourceType() : "UNKNOWN")
                    .append("]");
                if (StringUtils.hasText(src.getLabel())) {
                    sb.append(" ").append(src.getLabel());
                }
                sb.append(" => ")
                    .append(StringUtils.hasText(src.getContent()) ? src.getContent().trim() : "(no content)")
                    .append("\n");
            }
        } else {
            sb.append("(no feedback provided)\n");
        }
        sb.append("Produce JSON exactly in the schema above. Do not add any extra fields.");
        return sb.toString();
    }

    private String stripCodeFence(String raw) {
        if (raw.startsWith("```")) {
            int first = raw.indexOf('\n');
            int lastFence = raw.lastIndexOf("```");
            if (first >= 0 && lastFence > first) {
                return raw.substring(first + 1, lastFence).trim();
            }
        }
        return raw;
    }
}

