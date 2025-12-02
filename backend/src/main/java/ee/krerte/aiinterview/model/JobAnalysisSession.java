package ee.krerte.aiinterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_analysis_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAnalysisSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kasutaja e-mail, kellele see tööanalüüs kuulub.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Töö ametinimetus (nt "Senior Java Developer").
     */
    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    /**
     * Ettevõtte nimi (nt "Bolt", "Wise").
     */
    @Column(name = "company_name")
    private String companyName;

    /**
     * Algne töökuulutuse kirjeldus (toores tekst).
     */
    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    /**
     * Analüüsitud peamised oskused / märksõnad.
     */
    @Column(name = "skills_summary", columnDefinition = "TEXT")
    private String skillsSummary;

    /**
     * Sobivuse skoor (0–100).
     */
    @Column(name = "fit_score")
    private Integer fitScore;

    /**
     * Üldine kokkuvõte / soovitused (see on see, mida DataSeeder praegu .summary(...) meetodiga määrab).
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * Sessiooni loomise aeg.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
