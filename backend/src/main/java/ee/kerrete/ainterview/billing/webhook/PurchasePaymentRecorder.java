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

    public enum Outcome { MARKED_PAID, ALREADY_PAID, NOT_PAYABLE, UNKNOWN_SESSION }

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
            // another paid row of the same user+product exists: the user already has the roadmap (needs a manual refund)
            log.warn("Stripe session {}: user already has a paid purchase of this product; left unchanged, check for a refund",
                    payment.sessionId());
            return Outcome.ALREADY_PAID;
        }
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
            return Outcome.ALREADY_PAID;
        }
        if (p.getStatus() != PurchaseStatus.PENDING) {
            log.warn("Stripe session {}: purchase {} is {}, not marked paid", payment.sessionId(), p.getId(), p.getStatus());
            return Outcome.NOT_PAYABLE;
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
