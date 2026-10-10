package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.model.ProductEvent;
import ee.kerrete.ainterview.model.ProductEventType;
import ee.kerrete.ainterview.repository.ProductEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEventServiceTest {

    @Mock
    private ProductEventRepository repository;
    @Mock
    private PlatformTransactionManager transactionManager;
    private ProductEventService service;

    @BeforeEach
    void setUp() {
        service = new ProductEventService(repository, new ObjectMapper(), transactionManager);
    }

    @Test
    void recordsEventWithCodeDetailsAndTimestamp() throws Exception {
        service.record("u@example.com", ProductEventType.PLAN_GENERATED, Map.of("taskCount", 4));

        ArgumentCaptor<ProductEvent> captor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(repository).save(captor.capture());
        ProductEvent e = captor.getValue();
        assertThat(e.getEmail()).isEqualTo("u@example.com");
        assertThat(e.getEventType()).isEqualTo("plan_generated");
        assertThat(new ObjectMapper().readTree(e.getDetails()).get("taskCount").asInt()).isEqualTo(4);
        assertThat(e.getCreatedAt()).isNotNull();
    }

    @Test
    void emptyDetailsAreStoredAsNull() {
        service.record("u@example.com", ProductEventType.TASK_COMPLETED, Map.of());

        ArgumentCaptor<ProductEvent> captor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("task_completed");
        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    void eventsWithoutUserOrTypeAreSkipped() {
        service.record(null, ProductEventType.PLAN_GENERATED, Map.of());
        service.record("  ", ProductEventType.PLAN_GENERATED, Map.of());
        service.record("u@example.com", null, Map.of());
        verifyNoInteractions(repository);
    }

    @Test
    void writesInItsOwnRequiresNewTransaction() {
        service.record("u@example.com", ProductEventType.TASK_COMPLETED, Map.of());

        ArgumentCaptor<TransactionDefinition> captor = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(captor.capture());
        assertThat(captor.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void listenerRecordsThePublishedEvent() {
        service.on(new ProductEventOccurred("u@example.com", ProductEventType.PLAN_GENERATED, Map.of("taskCount", 2)));

        ArgumentCaptor<ProductEvent> captor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("plan_generated");
    }

    @Test
    void listenerIsAfterCommitWithFallbackForNonTransactionalCallers() throws Exception {
        TransactionalEventListener listener = ProductEventService.class
                .getMethod("on", ProductEventOccurred.class).getAnnotation(TransactionalEventListener.class);
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isTrue();
    }

    @Test
    void commitFailureIsSwallowed() {
        // e.g. UnexpectedRollbackException from the commit at the end of the REQUIRES_NEW transaction
        doThrow(new UnexpectedRollbackException("rollback-only")).when(transactionManager).commit(any());
        assertThatCode(() -> service.record("u@example.com", ProductEventType.TASK_COMPLETED, Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void repositoryFailureIsSwallowed() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));
        assertThatCode(() -> service.record("u@example.com", ProductEventType.TASK_COMPLETED, Map.of("taskKey", "a")))
                .doesNotThrowAnyException();
    }
}
