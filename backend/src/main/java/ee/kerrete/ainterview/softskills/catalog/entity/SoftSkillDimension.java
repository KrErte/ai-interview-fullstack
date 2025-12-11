package ee.kerrete.ainterview.softskills.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import jakarta.persistence.PrePersist;

@Entity
@Table(name = "soft_skill_dimension")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillDimension {

    @Id
    @GeneratedValue
    @UuidGenerator
    private java.util.UUID id;

    @Column(name = "dimension_key", nullable = false, unique = true, length = 100)
    private String dimensionKey;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(columnDefinition = "CLOB")
    private String definition;

    @Column(name = "high_signals", columnDefinition = "CLOB")
    private String highSignals;

    @Column(name = "low_signals", columnDefinition = "CLOB")
    private String lowSignals;

    @Column(name = "interview_signals", columnDefinition = "CLOB")
    private String interviewSignals;

    @Column(name = "coaching_ideas", columnDefinition = "CLOB")
    private String coachingIdeas;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

