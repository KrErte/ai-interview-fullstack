package ee.kerrete.ainterview.softskills.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.service.OpenAiClient;
import ee.kerrete.ainterview.softskills.dto.SoftSkillDimensionDto;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergeRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergeResponse;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileDto;
import ee.kerrete.ainterview.softskills.dto.SoftSkillSourceDto;
import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedProfileRecord;
import ee.kerrete.ainterview.softskills.repository.SoftSkillMergedProfileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoftSkillMergeRestService {

    private static final int MAX_LIST_ITEMS = 5;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final SoftSkillMergeAiService aiService;
    private final SoftSkillMergedProfileRecordRepository mergedProfileRecordRepository;

    public List<SoftSkillDimensionDto> listDimensions() {
        return Arrays.stream(DIMENSIONS)
            .map(dim -> SoftSkillDimensionDto.builder()
                .key(dim[0])
                .displayName(dim[1])
                .description(dim[2])
                .build())
            .toList();
    }

    public SoftSkillMergeResponse merge(SoftSkillMergeRequest request) {
        SoftSkillMergedProfileDto mergedProfile = aiService.callAi(
            request.getSources(),
            null,
            request.getEmail()
        );

        boolean shouldSave = Boolean.TRUE.equals(request.getSaveMerged());
        UUID savedId = null;
        LocalDateTime savedAt = LocalDateTime.now();
        if (shouldSave) {
            SoftSkillMergedProfileRecord record = persistMergedProfile(request.getEmail(), mergedProfile);
            savedId = record.getId();
            savedAt = LocalDateTime.ofInstant(record.getCreatedAt(), java.time.ZoneOffset.UTC);
        }

        return SoftSkillMergeResponse.builder()
                .mergedProfile(mergedProfile)
                .saved(shouldSave && savedId != null)
                .savedProfileId(savedId)
                .createdAt(savedAt)
                .build();
    }

    private SoftSkillMergedProfileDto generateMergedProfile(List<SoftSkillSourceDto> sources) {
        return normalize(aiService.callAi(sources, null, null));
    }

    private String buildFallbackSummary(List<SoftSkillSourceDto> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return "No feedback supplied.";
        }
        String combined = sources.stream()
                .map(SoftSkillSourceDto::getContent)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" | "));
        if (!StringUtils.hasText(combined)) {
            return "Feedback content missing.";
        }
        int max = 320;
        return combined.length() <= max ? combined : combined.substring(0, max) + "...";
    }

    private List<String> limitList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .limit(MAX_LIST_ITEMS)
                .toList();
    }

    private SoftSkillMergedProfileDto normalize(SoftSkillMergedProfileDto dto) {
        if (dto == null) {
            return SoftSkillMergedProfileDto.builder()
                    .summary("No feedback supplied.")
                    .strengths(List.of())
                    .risks(List.of())
                    .communicationStyle("Not provided")
                    .collaborationStyle("Not provided")
                    .growthAreas(List.of())
                    .build();
        }
        return SoftSkillMergedProfileDto.builder()
                .summary(StringUtils.hasText(dto.getSummary()) ? dto.getSummary().trim() : "No summary provided.")
                .strengths(limitList(dto.getStrengths()))
                .risks(limitList(dto.getRisks()))
                .communicationStyle(StringUtils.hasText(dto.getCommunicationStyle()) ? dto.getCommunicationStyle().trim() : "Not provided")
                .collaborationStyle(StringUtils.hasText(dto.getCollaborationStyle()) ? dto.getCollaborationStyle().trim() : "Not provided")
                .growthAreas(limitList(dto.getGrowthAreas()))
                .build();
    }

    private SoftSkillMergedProfileRecord persistMergedProfile(String email, SoftSkillMergedProfileDto dto) {
        SoftSkillMergedProfileRecord record = SoftSkillMergedProfileRecord.builder()
            .email(email)
            .summary(dto.getSummary())
            .strengthsJson(toJsonSafe(dto.getStrengths()))
            .risksJson(toJsonSafe(dto.getRisks()))
            .communicationStyle(dto.getCommunicationStyle())
            .collaborationStyle(dto.getCollaborationStyle())
            .growthAreasJson(toJsonSafe(dto.getGrowthAreas()))
            .build();

        return mergedProfileRecordRepository.save(record);
    }

    private String toJsonSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize merged profile payload: {}", e.getMessage());
            return "";
        }
    }

    private static final String[][] DIMENSIONS = new String[][]{
        {"communication", "Communication", "Clarity and effectiveness in sharing ideas."},
        {"collaboration", "Collaboration", "Ability to work with others and build trust."},
        {"ownership", "Ownership", "Takes responsibility and follows through on commitments."},
        {"problem_solving", "Problem Solving", "Approaches issues analytically and creatively."},
        {"learning_agility", "Learning Agility", "Learns quickly and adapts to new contexts."},
        {"stress_management", "Stress Management", "Stays composed and prioritizes under pressure."}
    };
}

