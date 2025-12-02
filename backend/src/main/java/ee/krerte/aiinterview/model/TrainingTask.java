package ee.krerte.aiinterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ainult oluline osa
@Entity
@Table(name = "training_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String question;
    private String answer;
    private Integer score;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "score_updated")
    private LocalDateTime scoreUpdated;

    @Column(name = "task_key")
    private String taskKey;

    @Column(name = "completed", nullable = false)
    private boolean completed;
}
