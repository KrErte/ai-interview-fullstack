package ee.kerrete.ainterview.softskills.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "soft_skill_merged_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillMergedProfileRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(columnDefinition = "CLOB")
    private String summary;

    @Column(name = "strengths_json", columnDefinition = "CLOB")
    private String strengthsJson;

    @Column(name = "risks_json", columnDefinition = "CLOB")
    private String risksJson;

    @Column(name = "communication_style", length = 512)
    private String communicationStyle;

    @Column(name = "collaboration_style", length = 512)
    private String collaborationStyle;

    @Column(name = "growth_areas_json", columnDefinition = "CLOB")
    private String growthAreasJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

