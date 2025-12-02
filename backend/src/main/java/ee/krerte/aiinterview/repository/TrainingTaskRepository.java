package ee.krerte.aiinterview.repository;

import ee.krerte.aiinterview.model.TrainingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingTaskRepository extends JpaRepository<TrainingTask, Long> {

    /**
     * Kõik treening-taskid konkreetse e-maili järgi (count).
     */
    long countByEmail(String email);

    /**
     * Lõpetatud (completed = true) treening-taskide arv.
     * Mõlemad kujud on projektis kasutusel, seega defineerime mõlemad.
     */
    long countByEmailAndCompletedTrue(String email);

    long countByEmailAndCompletedIsTrue(String email);

    /**
     * Kõik treening-taskid kasutaja järgi.
     */
    List<TrainingTask> findByEmail(String email);

    /**
     * Kõik treening-taskid kasutaja järgi, uuemad ees.
     */
    List<TrainingTask> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Ühe unikaalse taski leidmine email + taskKey järgi.
     */
    Optional<TrainingTask> findByEmailAndTaskKey(String email, String taskKey);
}
