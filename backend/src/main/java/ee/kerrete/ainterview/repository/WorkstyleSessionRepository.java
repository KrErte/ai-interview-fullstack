package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.WorkstyleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkstyleSessionRepository extends JpaRepository<WorkstyleSession, UUID> {
    WorkstyleSession findByEmailAndCompletedFalse(String email);
}
