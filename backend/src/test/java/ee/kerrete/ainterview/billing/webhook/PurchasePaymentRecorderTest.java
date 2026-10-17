package ee.kerrete.ainterview.billing.webhook;

import ee.kerrete.ainterview.billing.Purchase;
import ee.kerrete.ainterview.billing.PurchaseRepository;
import ee.kerrete.ainterview.billing.PurchaseStatus;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FP-62: a clash with the PostgreSQL one-paid-row-per-user+product index is a duplicate payment (H2 has no such index). */
class PurchasePaymentRecorderTest {

    final Logger logger = (Logger) LoggerFactory.getLogger(PurchasePaymentRecorder.class);
    final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void attach() {
        logs.start();
        logger.addAppender(logs);
    }

    @Test
    void uniquePaidIndexClashIsAlreadyPaid() {
        PurchaseRepository repo = mock(PurchaseRepository.class);
        Purchase p = new Purchase();
        p.setId(3L);
        p.setStatus(PurchaseStatus.PENDING);
        p.setAmountCents(1900);
        p.setCurrency("EUR");
        when(repo.findByStripeSessionId("cs_dup")).thenReturn(Optional.of(p));
        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_purchase_one_paid_per_product"));
        TransactionTemplate tx = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };

        PurchasePaymentRecorder.Outcome outcome = new PurchasePaymentRecorder(repo, tx)
                .recordPaid(new PurchasePaymentRecorder.Payment("cs_dup", 3L, null, "EE", 1900, "EUR"));

        assertThat(outcome).isEqualTo(PurchasePaymentRecorder.Outcome.DUPLICATE_PAYMENT);
        assertThat(logs.list).singleElement().satisfies(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.ERROR);
            assertThat(e.getFormattedMessage())
                    .isEqualTo("duplicate payment, refund manually via the Stripe dashboard: session cs_dup purchase 3");
        });
    }

    @Test
    void refundOfAPurchaseThatIsNotPaidChangesNothing() {
        PurchaseRepository repo = mock(PurchaseRepository.class);
        Purchase p = new Purchase();
        p.setId(4L);
        p.setStatus(PurchaseStatus.PENDING);
        when(repo.findByStripePaymentIntentId("pi_pending")).thenReturn(Optional.of(p));
        TransactionTemplate tx = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };

        PurchasePaymentRecorder.RefundOutcome outcome = new PurchasePaymentRecorder(repo, tx)
                .recordRefund(new PurchasePaymentRecorder.Refund("pi_pending", 1900, true));

        assertThat(outcome).isEqualTo(PurchasePaymentRecorder.RefundOutcome.NOT_REFUNDABLE);
        assertThat(p.getStatus()).isEqualTo(PurchaseStatus.PENDING);
        assertThat(p.getRefundStatus()).isNull();
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void matchRules() {
        Purchase own = new Purchase();
        own.setUserId(7L);
        own.setAmountCents(1900);
        own.setCurrency("EUR");
        assertThat(PurchasePaymentRecorder.matches(pay("7", 1900, "eur"), own)).isTrue();
        assertThat(PurchasePaymentRecorder.matches(pay(null, 1900, "EUR"), own)).isTrue();
        assertThat(PurchasePaymentRecorder.matches(pay("8", 1900, "eur"), own)).as("other user").isFalse();
        assertThat(PurchasePaymentRecorder.matches(pay("x", 1900, "eur"), own)).isFalse();
        assertThat(PurchasePaymentRecorder.matches(pay("7", 1, "eur"), own)).as("amount").isFalse();
        assertThat(PurchasePaymentRecorder.matches(pay("7", 0, "eur"), own)).isFalse();
        assertThat(PurchasePaymentRecorder.matches(pay("7", 1900, "usd"), own)).as("currency").isFalse();
        assertThat(PurchasePaymentRecorder.matches(pay("7", null, "eur"), own)).isFalse();
        assertThat(PurchasePaymentRecorder.matches(pay("7", 1900, null), own)).isFalse();
        own.setPriceId("price_1"); // amount lives in Stripe: any positive amount, same currency
        assertThat(PurchasePaymentRecorder.matches(pay("7", 2500, "eur"), own)).isTrue();
        assertThat(PurchasePaymentRecorder.matches(pay("7", 0, "eur"), own)).isFalse();
        assertThat(PurchasePaymentRecorder.matches(pay("7", 2500, "usd"), own)).isFalse();
    }

    private static PurchasePaymentRecorder.Payment pay(String clientRef, Integer amount, String currency) {
        return new PurchasePaymentRecorder.Payment("cs_1", 1L, clientRef, "EE", amount, currency);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(logs);
    }
}
