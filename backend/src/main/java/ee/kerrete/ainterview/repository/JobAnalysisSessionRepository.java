package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.JobAnalysisSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobAnalysisSessionRepository extends JpaRepository<JobAnalysisSession, Long> {

    /**
     * Mitu Job Matcheri analüüsi on antud emailiga tehtud.
     */
    long countByEmail(String email);

    /**
     * Kõik sessioonid antud kasutajale.
     */
    List<JobAnalysisSession> findByEmail(String email);

    /**
     * Kas antud emailiga on üldse sessioone olemas.
     * Kasutab DataSeeder.
     */
    boolean existsByEmail(String email);

    /**
     * Viimane Job Matcheri sessioon antud kasutajale (createdAt DESC LIMIT 1).
     */
    Optional<JobAnalysisSession> findTopByEmailOrderByCreatedAtDesc(String email);

    /**
     * Viimased 10 Job Matcheri sessiooni (createdAt DESC LIMIT 10).
     * Seda kasutab JobAnalysisSessionService.
     */
    List<JobAnalysisSession> findTop10ByEmailOrderByCreatedAtDesc(String email);
}
