package ee.kerrete.ainterview.billing.webhook;

import ee.kerrete.ainterview.billing.Purchase;
import ee.kerrete.ainterview.billing.PurchaseRepository;
import ee.kerrete.ainterview.billing.PurchaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Marks a purchase paid (FP-62), idempotently. The transaction is committed inside {@link #recordPaid}, so a clash
 * with the PostgreSQL "one paid row per user+product" index surfaces here and is treated as already paid.
 */
@Component
public class PurchasePaymentRecorder {

    public enum Outcome { MARKED_PAID, ALREADY_PAID, DUPLICATE_PAYMENT, NOT_PAYABLE, UNKNOWN_SESSION }

    /** Values from a verified, paid Checkout session; country/amount/currency may be null. */
    public record Payment(String sessionId, Long purchaseId, String buyerCountry, Integer amountCents, String currency) {
    }

    private static final Logger log = LoggerFactory.getLogger(PurchasePaymentRecorder.class);

    private final PurchaseRepository purchases;
    private final TransactionTemplate tx;

    public PurchasePaymentRecorder(PurchaseRepository purchases, TransactionTemplate tx) {
        this.purchases = purchases;
        this.tx = tx;
    }

    public Outcome recordPaid(Payment payment) {
        try {
            return tx.execute(status -> markPaid(payment));
        } catch (DataIntegrityViolationException e) {
            // PostgreSQL one-paid-row-per-user+product index: another paid row exists
            duplicate(payment.sessionId(), payment.purchaseId());
            return Outcome.DUPLICATE_PAYMENT;
        }
    }

    /** Session and purchase ids only, never PII. The webhook still answers 200. */
    private static void duplicate(String sessionId, Long purchaseId) {
        log.error("duplicate payment, refund manually via the Stripe dashboard: session {} purchase {}", sessionId, purchaseId);
    }

    private Outcome markPaid(Payment payment) {
        Optional<Purchase> found = purchases.findByStripeSessionId(payment.sessionId());
        if (found.isEmpty() && payment.purchaseId() != null) {
            found = purchases.findById(payment.purchaseId());
        }
        if (found.isEmpty()) {
            log.warn("Stripe session {}: no matching purchase", payment.sessionId());
            return Outcome.UNKNOWN_SESSION;
        }
        Purchase p = found.get();
        if (p.getStatus() == PurchaseStatus.PAID) {
            if (payment.sessionId().equals(p.getStripeSessionId())) {
                return Outcome.ALREADY_PAID; // redelivery of the same event/session
            }
            // an older session of a reused row was paid as well (matched via metadata.purchase_id)
            duplicate(payment.sessionId(), p.getId());
            return Outcome.DUPLICATE_PAYMENT;
        }
        if (p.getStatus() != PurchaseStatus.PENDING) {
            log.warn("Stripe session {}: purchase {} is {}, not marked paid", payment.sessionId(), p.getId(), p.getStatus());
            return Outcome.NOT_PAYABLE;
        }
        if (purchases.existsByUserIdAndProductAndStatus(p.getUserId(), p.getProduct(), PurchaseStatus.PAID)) {
            // the user is already entitled through another paid purchase
            duplicate(payment.sessionId(), p.getId());
            return Outcome.DUPLICATE_PAYMENT;
        }
        p.setStatus(PurchaseStatus.PAID);
        p.setPaidAt(LocalDateTime.now());
        p.setStripeSessionId(payment.sessionId());
        if (payment.buyerCountry() != null) {
            p.setBuyerCountry(payment.buyerCountry());
        }
        if (payment.amountCents() != null) {
            p.setAmountCents(payment.amountCents());
        }
        if (payment.currency() != null && payment.currency().length() == 3) {
            p.setCurrency(payment.currency());
        }
        purchases.saveAndFlush(p);
        log.info("Stripe session {}: purchase {} marked paid", payment.sessionId(), p.getId());
        return Outcome.MARKED_PAID;
    }
}
