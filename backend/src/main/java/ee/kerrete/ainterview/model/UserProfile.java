package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores a candidate's self-declared profile fields that power matching and dashboard.
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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "current_role")
    private String currentRole;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    /**
     * Comma separated skills list or JSON string for flexibility.
     */
    @Column(name = "skills", columnDefinition = "CLOB")
    private String skills;

    @Column(name = "bio", columnDefinition = "CLOB")
    private String bio;

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








