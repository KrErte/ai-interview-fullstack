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

    @Column(name = "total_tasks", nullable = false)
    private int totalTasks;

    @Column(name = "completed_tasks", nullable = false)
    private int completedTasks;

    @Column(name = "total_job_analyses", nullable = false)
    private int totalJobAnalyses;

    @Column(name = "total_training_sessions", nullable = false)
    private int totalTrainingSessions;

    /**
     * Üldine progress protsentides (0–100).
     */
    @Column(name = "training_progress_percent", nullable = false)
    private int trainingProgressPercent;

    /**
     * Treeningu staatuse enum (NOT NULL).
     * NB! Enum ise on eraldi failis TrainingStatus.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrainingStatus status;

    /**
     * Viimane aktiivsus (frontendis kuvatav "Last active").
     * Mappime veerule last_active.
     */
    @Column(name = "last_active")
    private LocalDateTime lastActivityAt;

    /**
     * Sisemine "viimati uuendatud" timestamp.
     * DB-s on see NOT NULL veerg LAST_UPDATED, seega PEAB entity seda täitma.
     */
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * Viimase tööanalüüsi match score.
     */
    @Column(name = "last_match_score")
    private Integer lastMatchScore;

    /**
     * Viimase tööanalüüsi kokkuvõte (short summary).
     */
    @Column(name = "last_match_summary", length = 1000)
    private String lastMatchSummary;
}
