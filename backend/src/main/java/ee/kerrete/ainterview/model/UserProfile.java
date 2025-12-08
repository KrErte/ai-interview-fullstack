package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Candidate self-declared profile that powers matching and dashboard.
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Logical key – must match AppUser.email.
     */
    @Column(nullable = false, unique = true)
    private String email;

    private String fullName;

    /**
     * Current role/title of the candidate.
     * Use a non-reserved column name to keep H2 happy.
     */
    @Column(name = "current_role_name")
    private String currentRole;

    /**
     * Target role/title of the candidate.
     */
    @Column(name = "target_role_name")
    private String targetRole;

    /**
     * Free-form skills JSON / comma-separated list.
     */
    @Lob
    private String skills;

    @Lob
    private String bio;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
