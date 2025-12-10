package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "seniority")
    private String seniority;

    /**
     * Serialized list of Q&A pairs (JSON stored as text for simplicity).
     */
    @Lob
    @Column(name = "question_answers", columnDefinition = "CLOB")
    private String questionAnswers;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

