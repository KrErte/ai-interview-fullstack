package ee.kerrete.ainterview.softskills.repository;

import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SoftSkillMergedProfileRepository extends JpaRepository<SoftSkillMergedProfile, UUID> {

    Optional<SoftSkillMergedProfile> findByEmail(String email);
}


