package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persisted CV extraction summary for later matching and dashboard stats.
 */
@Entity
@Table(name = "cv_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Short headline extracted from the CV (first line or best guess).
     */
    @Column(name = "headline")
    private String headline;

    /**
     * Parsed skills string (JSON array or comma separated).
     */
    @Column(name = "parsed_skills", columnDefinition = "CLOB")
    private String parsedSkills;

    @Column(name = "experience_summary", columnDefinition = "CLOB")
    private String experienceSummary;

    /**
     * Raw CV text for re-use in matching if provided by upload.
     */
    @Column(name = "raw_text", columnDefinition = "CLOB")
    private String rawText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}













