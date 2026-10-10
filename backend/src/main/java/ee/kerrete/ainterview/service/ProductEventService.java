package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.model.ProductEvent;
import ee.kerrete.ainterview.model.ProductEventType;
import ee.kerrete.ainterview.repository.ProductEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Records product analytics events, best-effort and outside the business transaction.
 * <ul>
 *   <li>Business code publishes {@link ProductEventOccurred}; {@link #on} runs AFTER_COMMIT, so an event
 *   is written only if the user's action committed. Without a transaction (e.g. the non-transactional
 *   job analysis flow) it runs immediately ({@code fallbackExecution = true}).</li>
 *   <li>The write runs in its own REQUIRES_NEW transaction via {@link TransactionTemplate}, and the whole
 *   {@code execute()} (including its commit) is inside try/catch, so a failing write (e.g. a missing
 *   table) is logged and never reaches the caller. An {@code @Transactional} method with an inner
 *   try/catch would not do that: the failed save marks the transaction rollback-only and the commit at
 *   the method boundary throws UnexpectedRollbackException to the caller.</li>
 * </ul>
 */
@Slf4j
@Service
public class ProductEventService {

    private final ProductEventRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiresNew;

    public ProductEventService(ProductEventRepository repository,
                               ObjectMapper objectMapper,
                               PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ProductEventOccurred event) {
        record(event.email(), event.type(), event.details());
    }

    /**
     * Writes one event in its own transaction. Never throws.
     */
    public void record(String email, ProductEventType type, Map<String, ?> details) {
        if (email == null || email.isBlank() || type == null) {
            return;
        }
        try {
            ProductEvent event = ProductEvent.builder()
                    .email(email)
                    .eventType(type.code())
                    .details(details == null || details.isEmpty() ? null : objectMapper.writeValueAsString(details))
                    .createdAt(LocalDateTime.now())
                    .build();
            requiresNew.executeWithoutResult(status -> repository.save(event));
        } catch (Exception e) {
            log.warn("Recording product event {} failed", type.code(), e);
        }
    }
}
