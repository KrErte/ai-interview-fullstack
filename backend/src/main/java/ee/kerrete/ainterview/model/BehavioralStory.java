package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "behavioral_story")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehavioralStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "situation", columnDefinition = "CLOB")
    private String situation;

    @Lob
    @Column(name = "task", columnDefinition = "CLOB")
    private String task;

    @Lob
    @Column(name = "action", columnDefinition = "CLOB")
    private String action;

    @Lob
    @Column(name = "result", columnDefinition = "CLOB")
    private String resultText;

    @Column(name = "tags")
    private String tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

