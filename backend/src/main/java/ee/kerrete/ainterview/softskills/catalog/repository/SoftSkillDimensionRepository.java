package ee.kerrete.ainterview.softskills.catalog.repository;

import ee.kerrete.ainterview.softskills.catalog.entity.SoftSkillDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SoftSkillDimensionRepository extends JpaRepository<SoftSkillDimension, UUID> {

    Optional<SoftSkillDimension> findByDimensionKey(String dimensionKey);
}

