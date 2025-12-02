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

    @Column(name = "email")
    private String email;

    @Column(name = "summary", length = 4000)
    private String summary;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "missing_skills_json", length = 4000)
    private String missingSkillsJson;

    @Column(name = "roadmap_json", length = 4000)
    private String roadmapJson;

    @Column(name = "suggested_improvements_json", length = 4000)
    private String suggestedImprovementsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
