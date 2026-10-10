package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.model.ProductEvent;
import ee.kerrete.ainterview.model.ProductEventType;
import ee.kerrete.ainterview.repository.ProductEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Liquibase changelog against in-memory H2 (PostgreSQL mode) and checks that
 * product_event is created, mapped and written by the real roadmap flow.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp10-events;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import(ProductEventService.class)
class ProductEventPersistenceTest {

    @Autowired
    private ProductEventService productEventService;
    @Autowired
    private ProductEventRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseCreatesProductEventTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = 'product_event'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void recordedEventIsPersisted() {
        String email = "fp10-" + UUID.randomUUID() + "@example.com";
        productEventService.record(email, ProductEventType.PLAN_GENERATED, Map.of("taskCount", 3));

        List<ProductEvent> events = repository.findByEmailOrderByCreatedAtAsc(email);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getId()).isNotNull();
        assertThat(events.get(0).getEventType()).isEqualTo("plan_generated");
        assertThat(events.get(0).getDetails()).contains("\"taskCount\":3");
    }

    // The roadmap flow (events written after commit) is covered by ProductEventIsolationIntegrationTest,
    // which runs without a surrounding test transaction.
}
