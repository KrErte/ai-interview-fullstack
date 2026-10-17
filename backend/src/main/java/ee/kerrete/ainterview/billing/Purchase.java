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

    /** The product/plan bought, e.g. {@link Feature#ROADMAP}'s "roadmap"; a later subscription plan gets its own code. */
    @Column(name = "product", nullable = false, length = 32)
    private String product;

    /** Stripe Price that was charged (from STRIPE_PRICE_ID at checkout); recorded only. */
    @Column(name = "price_id")
    private String priceId;

    @Convert(converter = PurchaseStatusConverter.class)
    @Column(name = "status", nullable = false, length = 16)
    private PurchaseStatus status;

    @Column(name = "stripe_session_id", unique = true)
    private String stripeSessionId;

    /** PaymentIntent of the paid session, set by the webhook; a refund event names the purchase by it. */
    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    /** Recorded amount actually charged (from Stripe); never a configured price. */
    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "withdrawal_waiver_accepted", nullable = false)
    private boolean withdrawalWaiverAccepted;

    @Column(name = "withdrawal_waiver_accepted_at")
    private LocalDateTime withdrawalWaiverAcceptedAt;

    /** Version of the withdrawal-waiver text the buyer accepted; one of {@link BillingSettings#WAIVER_VERSIONS}. */
    @Column(name = "waiver_text_version", length = 32)
    private String waiverTextVersion;

    /** ISO 3166-1 alpha-2 country from Stripe, nullable. */
    @Column(name = "buyer_country", length = 2)
    private String buyerCountry;

    @Column(name = "refund_status", length = 32)
    private String refundStatus;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /** Recorded refunded amount (from Stripe), nullable. */
    @Column(name = "refund_amount_cents")
    private Integer refundAmountCents;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
