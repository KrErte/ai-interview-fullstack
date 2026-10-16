package ee.kerrete.ainterview.billing.webhook;

import ee.kerrete.ainterview.billing.Purchase;
import ee.kerrete.ainterview.billing.PurchaseRepository;
import ee.kerrete.ainterview.billing.PurchaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** FP-62: a clash with the PostgreSQL one-paid-row-per-user+product index counts as already paid (H2 has no such index). */
class PurchasePaymentRecorderTest {

    @Test
    void uniquePaidIndexClashIsAlreadyPaid() {
        PurchaseRepository repo = mock(PurchaseRepository.class);
        Purchase p = new Purchase();
        p.setId(3L);
        p.setStatus(PurchaseStatus.PENDING);
        when(repo.findByStripeSessionId("cs_dup")).thenReturn(Optional.of(p));
        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_purchase_one_paid_per_product"));
        TransactionTemplate tx = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };

        PurchasePaymentRecorder.Outcome outcome = new PurchasePaymentRecorder(repo, tx)
                .recordPaid(new PurchasePaymentRecorder.Payment("cs_dup", 3L, "EE", 1900, "EUR"));

        assertThat(outcome).isEqualTo(PurchasePaymentRecorder.Outcome.ALREADY_PAID);
    }
}
