package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.billing.BillingSettings;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.billing.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** FP-61 against the real Liquibase schema (H2): the pending row and its waiver columns as stored. */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp61-checkout;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "billing.enabled=true",
        "billing.stripe-price-id=price_test_roadmap"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BillingSettings.class, EntitlementService.class, CheckoutSettings.class, CheckoutService.class})
class CheckoutServiceIntegrationTest {

    @Autowired
    CheckoutService checkout;
    @Autowired
    PurchaseRepository purchases;
    @Autowired
    JdbcTemplate jdbc;
    @MockBean
    CheckoutGateway gateway;

    @Test
    void storesOnePendingRowWithTheWaiverAndReusesIt() {
        jdbc.update("INSERT INTO app_users (email, password, role, enabled, created_at) VALUES ('carol@example.com', 'x', 'CANDIDATE', true, CURRENT_TIMESTAMP)");
        when(gateway.createSession(any()))
                .thenReturn(new CheckoutGateway.Session("cs_test_a", "https://checkout.stripe.test/a"))
                .thenReturn(new CheckoutGateway.Session("cs_test_b", "https://checkout.stripe.test/b"));
        CheckoutService.CheckoutRequest req = new CheckoutService.CheckoutRequest("roadmap", true, "waiver-v1-2026-10-16");

        assertThat(checkout.checkout("carol@example.com", req).url()).isEqualTo("https://checkout.stripe.test/a");
        assertThat(checkout.checkout("carol@example.com", req).url()).isEqualTo("https://checkout.stripe.test/b");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase", Integer.class)).isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap("SELECT status, stripe_session_id, amount_cents, currency, price_id, "
                + "withdrawal_waiver_accepted, withdrawal_waiver_accepted_at, waiver_text_version, paid_at FROM purchase");
        assertThat(row).containsEntry("status", "pending").containsEntry("stripe_session_id", "cs_test_b")
                .containsEntry("amount_cents", 1900).containsEntry("currency", "EUR")
                .containsEntry("price_id", "price_test_roadmap").containsEntry("withdrawal_waiver_accepted", true)
                .containsEntry("waiver_text_version", "waiver-v1-2026-10-16").containsEntry("paid_at", null);
        assertThat(row.get("withdrawal_waiver_accepted_at")).isNotNull();
    }
}
