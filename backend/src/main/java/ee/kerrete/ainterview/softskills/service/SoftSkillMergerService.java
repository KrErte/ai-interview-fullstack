package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileResponse;
import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedDimension;
import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedProfile;
import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import ee.kerrete.ainterview.softskills.repository.SoftSkillEvaluationRepository;
import ee.kerrete.ainterview.softskills.repository.SoftSkillMergedProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SoftSkillMergerService {

    private final SoftSkillEvaluationRepository evaluationRepository;
    private final SoftSkillMergedProfileRepository mergedProfileRepository;

    /**
     * Merge all available evaluations for a user and persist a merged profile.
     *
     * @param email user email
     * @return merged profile if any evaluations existed, otherwise empty
     */
    @Transactional
    public Optional<SoftSkillMergedProfileResponse> mergeForUser(String email) {
        List<SoftSkillEvaluation> evaluations = evaluationRepository.findByEmail(email);
        if (evaluations.isEmpty()) {
            return Optional.empty();
        }

        Map<SoftSkillDimension, List<SoftSkillEvaluation>> byDimension = evaluations.stream()
                .map(e -> new DimensionWrapper(resolveEnumDimension(e.getDimension()), e))
                .filter(dw -> dw.dimension() != null)
                .collect(Collectors.groupingBy(
                        DimensionWrapper::dimension,
                        () -> new EnumMap<>(SoftSkillDimension.class),
                        Collectors.mapping(DimensionWrapper::evaluation, Collectors.toList())
                ));

        // Build merged dimension entities deterministically (sorted by enum order)
        List<SoftSkillMergedDimension> mergedDimensions = new ArrayList<>();
        for (SoftSkillDimension dimension : SoftSkillDimension.values()) {
            List<SoftSkillEvaluation> perDim = byDimension.get(dimension);
            if (perDim == null || perDim.isEmpty()) {
                continue;
            }

            int mergedScore = computeAverageScore(perDim);
            String explanation = buildExplanation(perDim);

            SoftSkillMergedDimension mergedDimension = SoftSkillMergedDimension.builder()
                    .dimension(dimension)
                    .mergedScore(mergedScore)
                    .explanation(explanation)
                    .build();
            mergedDimensions.add(mergedDimension);
        }

        if (mergedDimensions.isEmpty()) {
            return Optional.empty();
        }

        int overallScore = (int) Math.round(
                mergedDimensions.stream()
                        .map(SoftSkillMergedDimension::getMergedScore)
                        .filter(score -> score != null)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0)
        );

        SoftSkillMergedProfile profile = mergedProfileRepository.findByEmail(email)
                .orElseGet(() -> SoftSkillMergedProfile.builder()
                        .email(email)
                        .build()
                );

        profile.setOverallScore(overallScore);

        // Replace dimension collection with the new merged values
        profile.getDimensions().clear();
        for (SoftSkillMergedDimension dim : mergedDimensions) {
            dim.setProfile(profile);
            profile.getDimensions().add(dim);
        }

        SoftSkillMergedProfile saved = mergedProfileRepository.save(profile);
        return Optional.ofNullable(SoftSkillMapper.toDto(saved));
    }

    @Transactional(readOnly = true)
    public Optional<SoftSkillMergedProfileResponse> getLatestProfile(String email) {
        return mergedProfileRepository.findByEmail(email)
                .map(SoftSkillMapper::toDto);
    }

    private int computeAverageScore(List<SoftSkillEvaluation> evaluations) {
        return (int) Math.round(
                evaluations.stream()
                        .map(SoftSkillEvaluation::getScore)
                        .filter(score -> score != null)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0)
        );
    }

    private String buildExplanation(List<SoftSkillEvaluation> evaluations) {
        return evaluations.stream()
                .sorted(Comparator.comparing(e -> e.getSource() != null ? e.getSource().name() : ""))
                .map(e -> {
                    SoftSkillSource src = e.getSource();
                    String prefix = src != null ? src.name() : "UNKNOWN";
                    String comment = StringUtils.hasText(e.getComment()) ? e.getComment().trim() : "no comment provided";
                    return prefix + ": " + comment;
                })
                .collect(Collectors.joining(" | "));
    }

    private SoftSkillDimension resolveEnumDimension(String dimensionKey) {
        if (!StringUtils.hasText(dimensionKey)) {
            return null;
        }
        String normalized = dimensionKey.trim()
            .replace(' ', '_')
            .replace('-', '_')
            .toUpperCase();
        try {
            return SoftSkillDimension.valueOf(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private record DimensionWrapper(SoftSkillDimension dimension, SoftSkillEvaluation evaluation) { }
}


