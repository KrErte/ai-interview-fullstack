package ee.krerte.aiinterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kasutaja e-mail, millega seome progressi.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Kokku treeningtaskide arv.
     */
    @Column(name = "total_tasks", nullable = false)
    private Integer totalTasks;

    /**
     * Lõpetatud treeningtaskide arv.
     */
    @Column(name = "completed_tasks", nullable = false)
    private Integer completedTasks;

    /**
     * Job Matcheri analüüside koguarv.
     */
    @Column(name = "total_job_analyses", nullable = false)
    private Integer totalJobAnalyses;

    /**
     * Kokku treeningusessioone (võid praegu 0 hoida / tulevikus kasutada).
     */
    @Column(name = "total_training_sessions", nullable = false)
    private Integer totalTrainingSessions;

    /**
     * Üldine progress protsentides (0–100).
     */
    @Column(name = "training_progress_percent", nullable = false)
    private Integer trainingProgressPercent;

    /**
     * Treeningu staatus (NOT_STARTED, IN_PROGRESS, COMPLETED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrainingStatus status;

    /**
     * Viimane aktiivsus (viimane training task või job analysis).
     */
    @Column(name = "last_active")
    private LocalDateTime lastActivityAt;

    /**
     * Viimase progressi uuenduse aeg.
     */
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /**
     * Viimase Job Matcheri analüüsi match score.
     */
    @Column(name = "last_match_score")
    private Double lastMatchScore;

    /**
     * Viimase Job Matcheri analüüsi lühikokkuvõte.
     */
    @Column(name = "last_match_summary", columnDefinition = "CLOB")
    private String lastMatchSummary;
}
