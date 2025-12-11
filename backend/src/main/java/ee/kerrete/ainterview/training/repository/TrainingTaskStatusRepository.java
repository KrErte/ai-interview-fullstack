package ee.kerrete.ainterview.training.repository;

import ee.kerrete.ainterview.training.entity.TrainingTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingTaskStatusRepository extends JpaRepository<TrainingTaskStatus, Long> {

    List<TrainingTaskStatus> findByUserEmail(String userEmail);

    Optional<TrainingTaskStatus> findByUserEmailAndTaskKey(String userEmail, String taskKey);
}

