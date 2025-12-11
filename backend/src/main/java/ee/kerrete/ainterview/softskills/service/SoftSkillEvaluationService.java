package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.catalog.service.SoftSkillDimensionService;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import ee.kerrete.ainterview.softskills.repository.SoftSkillEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SoftSkillEvaluationService {

    private final SoftSkillEvaluationRepository evaluationRepository;
    private final SoftSkillDimensionService dimensionService;

    @Transactional
    public List<SoftSkillEvaluationResponse> createEvaluations(SoftSkillEvaluationRequest request) {
        if (request.getScores() == null || request.getScores().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scores must not be empty");
        }

        SoftSkillSource source = parseSource(request.getSourceType());
        List<SoftSkillEvaluationResponse> responses = new ArrayList<>();
        for (SoftSkillEvaluationRequest.SoftSkillScoreRequest scoreRequest : request.getScores()) {
            String dimensionKey = normalizeDimensionKey(scoreRequest.getDimensionKey());
            boolean exists = dimensionService.findByKey(dimensionKey).isPresent();
            if (!exists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown dimension: " + dimensionKey);
            }

            SoftSkillEvaluation entity = SoftSkillEvaluation.builder()
                .email(request.getEmail())
                .dimension(dimensionKey)
                .source(source)
                .score(scoreRequest.getScore())
                .comment(StringUtils.hasText(scoreRequest.getExplanation()) ? scoreRequest.getExplanation().trim() : null)
                .build();

            SoftSkillEvaluation saved = evaluationRepository.save(entity);
            responses.add(SoftSkillMapper.toDto(saved));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<SoftSkillEvaluationResponse> getEvaluationsForUser(String email) {
        List<SoftSkillEvaluation> evaluations = evaluationRepository.findByEmail(email);
        return SoftSkillMapper.toDtoList(evaluations);
    }

    private String normalizeDimensionKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dimension is required");
        }
        return key.trim()
            .replace(' ', '_')
            .replace('-', '_')
            .toLowerCase();
    }

    private SoftSkillSource parseSource(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceType is required");
        }
        try {
            return SoftSkillSource.valueOf(sourceType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown sourceType: " + sourceType);
        }
    }
}


