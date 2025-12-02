package ee.krerte.aiinterview.repository;

import ee.krerte.aiinterview.model.TrainingProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingProgressRepository extends JpaRepository<TrainingProgress, Long> {

    Optional<TrainingProgress> findByEmail(String email);

}
