package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedDimensionView;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileView;
import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.repository.SoftSkillEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SoftSkillMergedProfileViewService {

    private final SoftSkillEvaluationRepository evaluationRepository;

    /**
     * Returns an aggregated view of a candidate's soft skill evaluations.
     * - Validates that email is provided.
     * - Returns an empty dimensions list when no evaluations exist (HTTP 200 use-case).
     * - Averages scores per dimension and includes rating counts.
     */
    @Transactional(readOnly = true)
    public SoftSkillMergedProfileView getMergedProfile(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter 'email' is required");
        }

        List<SoftSkillEvaluation> evaluations = evaluationRepository.findByEmail(email);
        if (evaluations.isEmpty()) {
            return SoftSkillMergedProfileView.builder()
                .email(email)
                .dimensions(List.of())
                .build();
        }

        Map<String, List<SoftSkillEvaluation>> byDimension = evaluations.stream()
            .filter(ev -> StringUtils.hasText(ev.getDimension()))
            .collect(Collectors.groupingBy(ev -> ev.getDimension().trim().toLowerCase()));

        List<SoftSkillMergedDimensionView> dimensions = byDimension.entrySet().stream()
            .map(entry -> {
                String dimensionKey = entry.getKey();
                List<SoftSkillEvaluation> perDimension = entry.getValue();
                double avg = perDimension.stream()
                    .map(SoftSkillEvaluation::getScore)
                    .filter(score -> score != null)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
                return SoftSkillMergedDimensionView.builder()
                    .dimensionKey(dimensionKey)
                    .averageScore(avg)
                    .ratingCount(perDimension.size())
                    .build();
            })
            .sorted(Comparator.comparing(SoftSkillMergedDimensionView::getDimensionKey))
            .toList();

        return SoftSkillMergedProfileView.builder()
            .email(email)
            .dimensions(dimensions)
            .build();
    }
}


