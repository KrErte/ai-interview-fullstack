package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.repository.SoftSkillEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SoftSkillEvaluationService {

    private final SoftSkillEvaluationRepository evaluationRepository;

    @Transactional
    public SoftSkillEvaluationResponse createEvaluation(SoftSkillEvaluationRequest request) {
        SoftSkillEvaluation entity = SoftSkillMapper.toEntity(request);
        SoftSkillEvaluation saved = evaluationRepository.save(entity);
        return SoftSkillMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SoftSkillEvaluationResponse> getEvaluationsForUser(String email) {
        List<SoftSkillEvaluation> evaluations = evaluationRepository.findByEmail(email);
        return SoftSkillMapper.toDtoList(evaluations);
    }
}


