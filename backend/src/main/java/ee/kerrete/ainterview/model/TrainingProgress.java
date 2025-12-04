package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "TrainingProgressBuilder", toBuilder = true)
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
     * Mitu treeningülesannet on kokku genereeritud.
     */
    @Column(name = "total_tasks", nullable = false)
    private int totalTasks;

    /**
     * Mitu treeningülesannet on lõpetatud.
     */
    @Column(name = "completed_tasks", nullable = false)
    private int completedTasks;

    /**
     * Mitu Job Matcheri analüüsi on kasutaja teinud.
     */
    @Column(name = "total_job_analyses", nullable = false)
    private int totalJobAnalyses;

    /**
     * Mitu treeningsessiooni on salvestatud.
     */
    @Column(name = "total_training_sessions", nullable = false)
    private int totalTrainingSessions;

    /**
     * Üldine progress protsentides (0–100).
     */
    @Column(name = "training_progress_percent", nullable = false)
    private int trainingProgressPercent;

    /**
     * Treeningu staatuse enum (NOT NULL).
     * STATUS: Väli NIMI on 'status', et Lombok genereeriks
     * getStatus()/setStatus() ja builder().status(),
     * mida kõik teenused/DTOd juba kasutavad.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TrainingStatus status = TrainingStatus.NOT_STARTED;

    /**
     * Viimane aktiivsus profiilis.
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /**
     * Viimane aeg, kui progressi arvutati või midagi uuendati.
     */
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /**
     * Viimane Job Matcheri skoor (nullable).
     */
    @Column(name = "last_match_score")
    private Double lastMatchScore;

    /**
     * Viimane Job Matcheri kokkuvõte (nullable).
     */
    @Column(name = "last_match_summary")
    private String lastMatchSummary;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = TrainingStatus.NOT_STARTED;
        }
    }
}
