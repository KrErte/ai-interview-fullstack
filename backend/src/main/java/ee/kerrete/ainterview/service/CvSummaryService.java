package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.CvSummaryDto;
import ee.kerrete.ainterview.model.CvSummary;
import ee.kerrete.ainterview.repository.CvSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CvSummaryService {

    private final CvSummaryRepository cvSummaryRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> KNOWN_SKILLS = List.of(
            "java", "spring", "spring boot", "angular", "react", "typescript", "javascript",
            "node", "python", "aws", "gcp", "azure", "docker", "kubernetes", "sql",
            "postgresql", "mysql", "mongodb", "kafka", "rabbitmq", "ci/cd", "jenkins",
            "git", "terraform", "linux", "rest", "graphql"
    );

    @Transactional
    public CvSummaryDto saveSummary(String email, String rawText) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required for CV summary");
        }
        if (rawText == null) {
            rawText = "";
        }

        String headline = extractHeadline(rawText, email);
        List<String> skills = extractSkills(rawText);
        String experience = extractExperience(rawText);

        CvSummary summary = cvSummaryRepository.findByEmail(email)
                .orElseGet(() -> CvSummary.builder().email(email).build());

        summary.setHeadline(headline);
        summary.setParsedSkills(writeSkills(skills));
        summary.setExperienceSummary(experience);
        summary.setRawText(rawText);

        cvSummaryRepository.save(summary);

        return toDto(summary, skills);
    }

    @Transactional(readOnly = true)
    public Optional<CvSummaryDto> findByEmail(String email) {
        return cvSummaryRepository.findByEmail(email)
                .map(this::toDto);
    }

    private String extractHeadline(String text, String fallbackEmail) {
        if (StringUtils.hasText(text)) {
            return Arrays.stream(text.split("\\R"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse("CV for " + fallbackEmail);
        }
        return "CV for " + fallbackEmail;
    }

    public List<String> extractSkills(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> matches = new LinkedHashSet<>();
        for (String skill : KNOWN_SKILLS) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) {
                matches.add(skill);
            }
        }
        // fallback: pick capitalized tokens as potential skills
        if (matches.isEmpty()) {
            Arrays.stream(text.split("[,\\n]"))
                    .map(String::trim)
                    .filter(s -> s.length() > 1 && Character.isUpperCase(s.charAt(0)))
                    .limit(8)
                    .forEach(matches::add);
        }
        return matches.stream().toList();
    }

    private String extractExperience(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String[] lines = text.split("\\R");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line.toLowerCase(Locale.ROOT).contains("experience")
                    || line.toLowerCase(Locale.ROOT).contains("worked")
                    || builder.length() < 220) {
                builder.append(line.trim()).append(" ");
            }
            if (builder.length() > 480) break;
        }
        String result = builder.toString().trim();
        if (result.isEmpty()) {
            result = text.substring(0, Math.min(480, text.length()));
        }
        return result;
    }

    private String writeSkills(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (JsonProcessingException e) {
            return String.join(",", skills);
        }
    }

    private List<String> readSkills(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }
    }

    private CvSummaryDto toDto(CvSummary summary) {
        return toDto(summary, readSkills(summary.getParsedSkills()));
    }

    private CvSummaryDto toDto(CvSummary summary, List<String> skills) {
        return CvSummaryDto.builder()
                .email(summary.getEmail())
                .headline(summary.getHeadline())
                .parsedSkills(skills)
                .experienceSummary(summary.getExperienceSummary())
                .rawText(summary.getRawText())
                .build();
    }
}













