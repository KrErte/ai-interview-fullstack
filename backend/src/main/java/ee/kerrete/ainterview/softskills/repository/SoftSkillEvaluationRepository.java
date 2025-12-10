package ee.kerrete.ainterview.softskills.repository;

import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SoftSkillEvaluationRepository extends JpaRepository<SoftSkillEvaluation, UUID> {

    List<SoftSkillEvaluation> findByEmail(String email);

    List<SoftSkillEvaluation> findByEmailAndDimension(String email, SoftSkillDimension dimension);
}


