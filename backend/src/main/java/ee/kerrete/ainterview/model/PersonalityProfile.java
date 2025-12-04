// Fail: backend/src/main/java/ee/krerte/aiinterview/model/PersonalityProfile.java

package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personality_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // Näiteks mõned isiksuse tunnused skaalal 1-5 või tekstina
    private int introversionLevel;
    private int teamworkPreference;
    private String preferredWorkStyle;
}
