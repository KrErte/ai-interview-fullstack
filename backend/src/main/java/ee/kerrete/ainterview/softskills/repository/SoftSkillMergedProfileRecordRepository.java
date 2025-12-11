package ee.kerrete.ainterview.softskills.repository;

import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedProfileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SoftSkillMergedProfileRecordRepository extends JpaRepository<SoftSkillMergedProfileRecord, UUID> {
}

