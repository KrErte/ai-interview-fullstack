package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * Optional style prompt or guidelines for Ghost Interviewer.
     */
    @Lob
    @Column(name = "style_prompt", columnDefinition = "CLOB")
    private String stylePrompt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

