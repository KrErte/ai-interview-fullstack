package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One product analytics event (append-only).
 */
@Entity
@Table(name = "product_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    /** e.g. "plan_generated", see {@link ProductEventType#code()}. */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /** Small JSON object with event details, e.g. {"source":"job_analysis","taskCount":4}. */
    @Column(name = "details", columnDefinition = "CLOB")
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
