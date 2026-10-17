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
import java.util.Locale;
import java.util.Optional;

/**
 * Marks a purchase paid (FP-62), idempotently. The transaction is committed inside {@link #recordPaid}, so a clash
 * with the PostgreSQL "one paid row per user+product" index surfaces here and is treated as already paid.
 * A full refund ({@link #recordRefund}) sets the purchase to refunded, which ends the entitlement.
 */
@Component
public class PurchasePaymentRecorder {

    public enum Outcome { MARKED_PAID, ALREADY_PAID, DUPLICATE_PAYMENT, PAYMENT_MISMATCH, NOT_PAYABLE, UNKNOWN_SESSION }

    public enum RefundOutcome { REFUNDED, PARTIALLY_REFUNDED, UNKNOWN_PAYMENT, NOT_REFUNDABLE }

    /**
     * Values from a verified, paid Checkout session; clientReferenceId/country/amount/currency/paymentIntentId may
     * be null.
     */
    public record Payment(String sessionId, Long purchaseId, String clientReferenceId, String buyerCountry,
                          Integer amountCents, String currency, String paymentIntentId) {

        public Payment(String sessionId, Long purchaseId, String clientReferenceId, String buyerCountry,
                       Integer amountCents, String currency) {
            this(sessionId, purchaseId, clientReferenceId, buyerCountry, amountCents, currency, null);
        }
    }

    /** Values from a verified charge.refunded event: the refunded total so far, and whether that is the whole charge. */
    public record Refund(String paymentIntentId, long amountRefundedCents, boolean fullyRefunded) {
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
        if (!matches(payment, p)) {
            log.error("payment mismatch, not marked paid: session {} purchase {}", payment.sessionId(), p.getId());
            return Outcome.PAYMENT_MISMATCH;
        }
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
        if (payment.paymentIntentId() != null) {
            p.setStripePaymentIntentId(payment.paymentIntentId());
        }
        if (payment.buyerCountry() != null) {
            p.setBuyerCountry(payment.buyerCountry());
        }
        p.setAmountCents(payment.amountCents());
        p.setCurrency(payment.currency().toUpperCase(Locale.ROOT));
        purchases.saveAndFlush(p);
        log.info("Stripe session {}: purchase {} marked paid", payment.sessionId(), p.getId());
        return Outcome.MARKED_PAID;
    }

    /**
     * Records a refund of the charge behind a purchase. Only a purchase whose PaymentIntent was stored when it was
     * marked paid is touched, so refunding a duplicate payment (never recorded on a purchase) changes nothing.
     * A full refund sets the status to refunded, which ends the entitlement; a partial refund is only recorded and
     * the buyer keeps access. Redelivery is harmless: the amount is Stripe's running total.
     */
    public RefundOutcome recordRefund(Refund refund) {
        return tx.execute(status -> markRefunded(refund));
    }

    private RefundOutcome markRefunded(Refund refund) {
        Optional<Purchase> found = purchases.findByStripePaymentIntentId(refund.paymentIntentId());
        if (found.isEmpty()) {
            log.info("Stripe refund of payment {}: no purchase paid through it (e.g. a duplicate payment)", refund.paymentIntentId());
            return RefundOutcome.UNKNOWN_PAYMENT;
        }
        Purchase p = found.get();
        if (p.getStatus() != PurchaseStatus.PAID && p.getStatus() != PurchaseStatus.REFUNDED) {
            log.warn("Stripe refund of payment {}: purchase {} is {}, not changed", refund.paymentIntentId(), p.getId(), p.getStatus());
            return RefundOutcome.NOT_REFUNDABLE;
        }
        p.setRefundAmountCents((int) refund.amountRefundedCents());
        p.setRefundedAt(LocalDateTime.now());
        p.setRefundStatus(refund.fullyRefunded() ? "full" : "partial");
        if (refund.fullyRefunded()) {
            p.setStatus(PurchaseStatus.REFUNDED);
        }
        purchases.saveAndFlush(p);
        log.info("Stripe refund of payment {}: purchase {} {}", refund.paymentIntentId(), p.getId(),
                refund.fullyRefunded() ? "refunded, access removed" : "partially refunded, access kept");
        return refund.fullyRefunded() ? RefundOutcome.REFUNDED : RefundOutcome.PARTIALLY_REFUNDED;
    }

    /**
     * The session must belong to this purchase: client_reference_id (when present) is the purchase's user id, the
     * currency equals the stored one (case-insensitive), and the charged amount equals the stored amount when we
     * set it (no price_id: price_data from our config). With a Stripe Price the amount lives in Stripe, so any
     * positive amount_total is accepted there.
     */
    static boolean matches(Payment payment, Purchase p) {
        if (payment.clientReferenceId() != null && !payment.clientReferenceId().equals(String.valueOf(p.getUserId()))) {
            return false;
        }
        if (payment.currency() == null || p.getCurrency() == null || !payment.currency().equalsIgnoreCase(p.getCurrency())) {
            return false;
        }
        if (payment.amountCents() == null || payment.amountCents() <= 0) {
            return false;
        }
        return p.getPriceId() != null || payment.amountCents() == p.getAmountCents();
    }
}
