package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
}

