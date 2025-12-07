package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kasutaja email, kellele see task kuulub.
     */
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * Küsimus, mida treenitakse.
     */
    @Column(name = "question", columnDefinition = "CLOB")
    private String question;

    /**
     * Kasutaja viimane vastus.
     */
    @Column(name = "answer", columnDefinition = "CLOB")
    private String answer;

    /**
     * Viimane skoor (0–100 vms).
     */
    @Column(name = "score")
    private Integer score;

    /**
     * Taski loomise aeg.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Viimane uuendus (vastus/score muutus).
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Eraldi aeg, millal skoor viimati uuendati (soovi järgi).
     */
    @Column(name = "score_updated")
    private LocalDateTime scoreUpdated;

    /**
     * Loogiline võti (nt roadmapi/teema identifikaator).
     */
    @Column(name = "task_key")
    private String taskKey;

    /**
     * Treenitava soft-skill teema (nt conflict_management).
     */
    @Column(name = "skill_key")
    private String skillKey;

    /**
     * Kas task on lõpetatud (treening tehtud, skoor OK jne).
     */
    @Column(name = "completed", nullable = false)
    private boolean completed;

    /**
     * Viimane AI tagasiside kasutaja vastusele.
     */
    @Column(name = "feedback", columnDefinition = "CLOB")
    private String feedback;
}
