package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    Optional<InterviewSession> findBySessionUuid(UUID sessionUuid);
}

