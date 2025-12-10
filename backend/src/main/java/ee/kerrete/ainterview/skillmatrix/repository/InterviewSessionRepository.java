package ee.kerrete.ainterview.skillmatrix.repository;

import ee.kerrete.ainterview.skillmatrix.entity.InterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, UUID> {
}


