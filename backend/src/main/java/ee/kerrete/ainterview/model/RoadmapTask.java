package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
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

    @Column(name = "day_number")
    private Integer dayNumber;

    @Column(name = "order_index")
    private Integer orderIndex;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
