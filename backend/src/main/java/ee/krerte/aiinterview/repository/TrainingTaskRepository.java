package ee.krerte.aiinterview.repository;

import ee.krerte.aiinterview.model.TrainingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingTaskRepository extends JpaRepository<TrainingTask, Long> {

    // olemasolevad kasutused teistes teenustes
    List<TrainingTask> findByEmail(String email);

    Optional<TrainingTask> findByEmailAndTaskKey(String email, String taskKey);

    long countByEmailAndCompletedIsTrue(String email);

    // meie progressi jaoks
    long countByEmail(String email);

    List<TrainingTask> findByEmailOrderByCreatedAtDesc(String email);
}
