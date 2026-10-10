package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.RoadmapTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapTaskRepository
        extends JpaRepository<RoadmapTask, Long> {

    List<RoadmapTask> findByEmail(String email);

    Optional<RoadmapTask> findByEmailAndTaskKey(String email, String taskKey);

    void deleteByEmail(String email);

    /**
     * Deletes only the tasks that are not completed yet. Must run inside a transaction.
     */
    long deleteByEmailAndCompletedFalse(String email);

    long countByEmail(String email);

    long countByEmailAndCompletedIsTrue(String email);
}
