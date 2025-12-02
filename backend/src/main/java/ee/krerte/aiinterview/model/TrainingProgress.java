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
     * NB! Enum ise on eraldi failis TrainingStatus.java
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrainingStatus status;

    /**
     * Viimane aktiivsuse aeg – kas töökuulutuse analüüs või treeningvastus.
     */
    @Column(name = "last_active")
    private LocalDateTime lastActive;

    @Column(name = "last_match_score")
    private Integer lastMatchScore;

    @Column(name = "last_match_summary", length = 1000)
    private String lastMatchSummary;

    /**
     * Millal seda progressi viimati uuendati (meta).
     */
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /**
     * Tagame, et:
     *  - status EI OLE null (DB veerg on NOT NULL)
     *  - lastUpdated saab väärtuse kui seda ei ole seatud
     *
     * Kasutame TrainingStatus.values()[0], et mitte oletada enum’i konkreetset nime.
     * See tähendab: esimene enum väärtus on vaikimisi staatus.
     */
    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = TrainingStatus.values()[0]; // nt NEW / IN_PROGRESS / ACTIVE – mis iganes sul esimesena defineeritud on
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
