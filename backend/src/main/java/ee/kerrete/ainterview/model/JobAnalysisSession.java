package ee.kerrete.ainterview.model;

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
     * Kasutaja email (võib olla ka null, kui anonüümne).
     */
    @Column(name = "email")
    private String email;

    /**
     * Originaalne kasutaja email enne refaktorit (USER_EMAIL veerg).
     * Soovi korral võid selle hiljem eemaldada, kui pole vaja.
     */
    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "job_description", columnDefinition = "CLOB")
    private String jobDescription;

    /**
     * AI analüüsi toor-tulemus (JSON või tekst).
     */
    @Column(name = "analysis_result", columnDefinition = "CLOB")
    private String analysisResult;

    @Column(name = "missing_skills_json", columnDefinition = "CLOB")
    private String missingSkillsJson;

    @Column(name = "strengths_json", columnDefinition = "CLOB")
    private String strengthsJson;

    @Column(name = "weaknesses_json", columnDefinition = "CLOB")
    private String weaknessesJson;

    @Column(name = "roadmap_json", columnDefinition = "CLOB")
    private String roadmapJson;

    @Column(name = "suggested_improvements_json", columnDefinition = "CLOB")
    private String suggestedImprovementsJson;
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    @Column(name = "summary", columnDefinition = "CLOB")
    private String summary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
