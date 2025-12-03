package ee.krerte.aiinterview.repository;

import ee.krerte.aiinterview.model.TrainingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingTaskRepository extends JpaRepository<TrainingTask, Long> {

    /**
     * Kõik treeningtaskid kasutaja emaili järgi.
     */
    List<TrainingTask> findByEmail(String email);

    /**
     * Kõik treeningtaskid kasutaja emaili järgi, uuemad esimesena.
     * (kasutab TrainingService).
     */
    List<TrainingTask> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Mitu treeningtaski on antud emailiga kokku.
     */
    long countByEmail(String email);

    /**
     * Mitu treeningtaski on antud emailiga ja completed = true.
     * Kasutatakse ProgressService'is.
     */
    long countByEmailAndCompletedIsTrue(String email);

    /**
     * Sama, kuid "CompletedTrue" stiilis – kasutame UserProgressService'is.
     */
    long countByEmailAndCompletedTrue(String email);

    /**
     * Viimane (uuendatud) treeningtask antud kasutajale – kasutame lastActivity arvutamiseks.
     */
    Optional<TrainingTask> findTopByEmailOrderByUpdatedAtDesc(String email);

    /**
     * Leia konkreetne task loogilise võtme järgi (roadmap key).
     * Kasutab TrainingService ja TrainingTaskController.
     */
    Optional<TrainingTask> findByEmailAndTaskKey(String email, String taskKey);
}
