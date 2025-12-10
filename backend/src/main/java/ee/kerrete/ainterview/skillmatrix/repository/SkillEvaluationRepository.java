package ee.kerrete.ainterview.skillmatrix.repository;

import ee.kerrete.ainterview.skillmatrix.entity.SkillEvaluationEntity;
import ee.kerrete.ainterview.skillmatrix.entity.InterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillEvaluationRepository extends JpaRepository<SkillEvaluationEntity, UUID> {

    List<SkillEvaluationEntity> findBySession(InterviewSessionEntity session);
}


