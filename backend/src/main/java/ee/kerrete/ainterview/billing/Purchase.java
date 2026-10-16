package ee.kerrete.ainterview.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One purchase of a paid product (FP-60). See 2026-10-16-create-purchase-table.xml. */
@Entity
@Table(name = "purchase")
@Getter
@Setter
@NoArgsConstructor
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email", nullable = false)
    private String email;

    /** e.g. {@link EntitlementService#ROADMAP}. */
    @Column(name = "product", nullable = false, length = 32)
    private String product;

    @Convert(converter = PurchaseStatusConverter.class)
    @Column(name = "status", nullable = false, length = 16)
    private PurchaseStatus status;

    @Column(name = "stripe_session_id", unique = true)
    private String stripeSessionId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "withdrawal_waiver_accepted", nullable = false)
    private boolean withdrawalWaiverAccepted;

    @Column(name = "withdrawal_waiver_accepted_at")
    private LocalDateTime withdrawalWaiverAcceptedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
