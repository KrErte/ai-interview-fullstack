import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "roadmap_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;       // kasutaja identifikaator
    private String taskKey;     // nt "incident_management"
    private String title;       // "Incident management STAR"
    private String description; // "Harjuta ootamatut veaolukorda"
    private boolean completed;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
