package ee.kerrete.ainterview.softskills.catalog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.softskills.catalog.dto.SoftSkillDimensionResponseDto;
import ee.kerrete.ainterview.softskills.catalog.entity.SoftSkillDimension;

import java.util.List;

public final class SoftSkillDimensionResponseMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SoftSkillDimensionResponseMapper() {
    }

    public static SoftSkillDimensionResponseDto toDto(SoftSkillDimension entity) {
        if (entity == null) {
            return null;
        }
        return SoftSkillDimensionResponseDto.builder()
            .key(entity.getDimensionKey())
            .label(entity.getLabel())
            .definition(entity.getDefinition())
            .highSignals(parseList(entity.getHighSignals()))
            .lowSignals(parseList(entity.getLowSignals()))
            .interviewSignals(parseList(entity.getInterviewSignals()))
            .coachingIdeas(parseList(entity.getCoachingIdeas()))
            .build();
    }

    public static List<SoftSkillDimensionResponseDto> toDtoList(List<SoftSkillDimension> entities) {
        return entities == null ? List.of() : entities.stream().map(SoftSkillDimensionResponseMapper::toDto).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}

