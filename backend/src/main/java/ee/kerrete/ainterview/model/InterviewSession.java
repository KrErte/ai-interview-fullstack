package ee.kerrete.ainterview.model;

import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "session_uuid", unique = true)
    private UUID sessionUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "interviewer_style", length = 50)
    private InterviewerStyle interviewerStyle;

    @Column(name = "last1_answer", columnDefinition = "CLOB")
    private String last1Answer;

    @Column(name = "last3_answers", columnDefinition = "CLOB")
    private String last3Answers;

    @Column(name = "last5_answers", columnDefinition = "CLOB")
    private String last5Answers;

    /**
     * Serialized list of Q&A pairs (JSON stored as text for simplicity).
     */
    @Lob
    @Column(name = "question_answers", columnDefinition = "CLOB")
    private String questionAnswers;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

