package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.TrainingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrainingProgressRepository extends JpaRepository<TrainingProgress, Long> {

    /**
     * Kas antud emailiga on juba TrainingProgress kirje olemas.
     */
    @Query("SELECT COUNT(t) > 0 FROM TrainingProgress t WHERE t.email = :email")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Leiab progressi kasutaja emaili järgi.
     */
    Optional<TrainingProgress> findByEmail(String email);
}
