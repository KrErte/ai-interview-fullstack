package ee.krerte.aiinterview.repository;

import ee.krerte.aiinterview.model.WorkstyleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkstyleSessionRepository extends JpaRepository<WorkstyleSession, UUID> {
    WorkstyleSession findByEmailAndCompletedFalse(String email);
}
