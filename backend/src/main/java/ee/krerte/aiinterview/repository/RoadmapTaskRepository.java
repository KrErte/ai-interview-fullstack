package ee.krerte.aiinterview.repository;

import ee.krerte.aiinterview.model.RoadmapTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadmapTaskRepository
        extends JpaRepository<RoadmapTask, Long> {

    List<RoadmapTask> findByEmail(String email);
}
