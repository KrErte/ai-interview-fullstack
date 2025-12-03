package ee.krerte.aiinterview.repository;

import ee.krerte.aiinterview.model.TrainingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingTaskRepository extends JpaRepository<TrainingTask, Long> {

    long countByEmail(String email);

    long countByEmailAndCompletedIsTrue(String email);

    /**
     * Kõik taskid antud kasutajale (järjekord vaba).
     * Kasutavad MindsetRoadmapService, SkillMatrixService, SoftSkillMatrixService.
     */
    List<TrainingTask> findByEmail(String email);

    /**
     * Kõik taskid antud kasutajale, uuem enne.
     */
    List<TrainingTask> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Konkreetsed taskid email + taskKey järgi (treeningu identifikaator).
     */
    Optional<TrainingTask> findByEmailAndTaskKey(String email, String taskKey);
}
