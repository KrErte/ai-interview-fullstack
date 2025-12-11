package ee.kerrete.ainterview.softskills.entity;

import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "legacy_soft_skill_merged_dimension")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillMergedDimension {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private SoftSkillMergedProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private SoftSkillDimension dimension;

    /**
     * Merged score in range [0, 100].
     */
    @Column(name = "merged_score")
    private Integer mergedScore;

    @Column(columnDefinition = "CLOB")
    private String explanation;
}


