package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedDimensionDto;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileResponse;
import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedDimension;
import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedProfile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lightweight mapping helpers between entities and DTOs.
 */
public final class SoftSkillMapper {

    private SoftSkillMapper() {
        // utility
    }

    public static SoftSkillEvaluationResponse toDto(SoftSkillEvaluation entity) {
        if (entity == null) {
            return null;
        }
        return SoftSkillEvaluationResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .dimension(entity.getDimension())
                .source(entity.getSource())
                .score(entity.getScore())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static List<SoftSkillEvaluationResponse> toDtoList(List<SoftSkillEvaluation> entities) {
        return entities == null
                ? List.of()
                : entities.stream().map(SoftSkillMapper::toDto).collect(Collectors.toList());
    }

    public static SoftSkillMergedDimensionDto toDto(SoftSkillMergedDimension entity) {
        if (entity == null) {
            return null;
        }
        return SoftSkillMergedDimensionDto.builder()
                .dimension(entity.getDimension())
                .mergedScore(entity.getMergedScore())
                .explanation(entity.getExplanation())
                .build();
    }

    public static List<SoftSkillMergedDimensionDto> toDimensionDtoList(List<SoftSkillMergedDimension> entities) {
        return entities == null
                ? List.of()
                : entities.stream().map(SoftSkillMapper::toDto).collect(Collectors.toList());
    }

    public static SoftSkillMergedProfileResponse toDto(SoftSkillMergedProfile entity) {
        if (entity == null) {
            return null;
        }
        return SoftSkillMergedProfileResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .overallScore(entity.getOverallScore())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .dimensions(toDimensionDtoList(entity.getDimensions()))
                .build();
    }

}


