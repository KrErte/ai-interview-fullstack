package ee.kerrete.ainterview.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The real Liquibase changelog on in-memory H2 (PostgreSQL mode): the purchase table, its constraints,
 * and that only a paid roadmap purchase of the same user grants the entitlement.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp60-purchase;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "billing.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BillingSettings.class, EntitlementService.class})
class EntitlementServiceIntegrationTest {

    @Autowired
    EntitlementService entitlements;
    @Autowired
    PurchaseRepository purchases;
    @Autowired
    ee.kerrete.ainterview.repository.AppUserRepository users;
    @Autowired
    JdbcTemplate jdbc;

    long alice;
    long bob;

    @BeforeEach
    void users() {
        alice = user("alice@example.com");
        bob = user("bob@example.com");
    }

    private long user(String email) {
        jdbc.update("INSERT INTO app_users (email, password, role, enabled, created_at) VALUES (?, 'x', 'CANDIDATE', true, CURRENT_TIMESTAMP)", email);
        return jdbc.queryForObject("SELECT id FROM app_users WHERE email = ?", Long.class, email);
    }

    private Purchase purchase(long userId, String email, String product, PurchaseStatus status, String session) {
        Purchase p = new Purchase();
        p.setUserId(userId);
        p.setEmail(email);
        p.setProduct(product);
        p.setStatus(status);
        p.setStripeSessionId(session);
        p.setAmountCents(1900);
        p.setCurrency("EUR");
        p.setCreatedAt(LocalDateTime.now());
        if (status == PurchaseStatus.PAID) {
            p.setPaidAt(LocalDateTime.now());
        }
        return purchases.saveAndFlush(p);
    }

    @Test
    void noPurchaseNoEntitlement() {
        assertThat(entitlements.hasRoadmap("alice@example.com")).isFalse();
    }

    @Test
    void pendingAndRefundedDoNotCount() {
        purchase(alice, "alice@example.com", "roadmap", PurchaseStatus.PENDING, "cs_1");
        purchase(alice, "alice@example.com", "roadmap", PurchaseStatus.REFUNDED, "cs_2");
        assertThat(entitlements.hasRoadmap("alice@example.com")).isFalse();
    }

    @Test
    void paidRoadmapCountsOnlyForItsUser() {
        purchase(alice, "alice@example.com", "roadmap", PurchaseStatus.PAID, null);
        assertThat(entitlements.hasRoadmap("alice@example.com")).isTrue();
        assertThat(entitlements.hasRoadmap("bob@example.com")).isFalse();
        assertThat(entitlements.hasRoadmap("ALICE@example.com")).as("exact principal email").isFalse();
    }

    @Test
    void paidOtherProductDoesNotCount() {
        purchase(bob, "bob@example.com", "team", PurchaseStatus.PAID, null);
        assertThat(entitlements.hasRoadmap("bob@example.com")).isFalse();
    }

    @Test
    void statusIsStoredLowercaseWithWaiverDefaults() {
        Purchase p = purchase(alice, "alice@example.com", "roadmap", PurchaseStatus.PENDING, "cs_x");
        assertThat(jdbc.queryForObject("SELECT status FROM purchase WHERE id = ?", String.class, p.getId())).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT withdrawal_waiver_accepted FROM purchase WHERE id = ?", Boolean.class, p.getId())).isFalse();
    }

    @Test
    void stripeSessionIdIsUniqueButNullable() {
        purchase(alice, "alice@example.com", "roadmap", PurchaseStatus.PENDING, null);
        purchase(bob, "bob@example.com", "roadmap", PurchaseStatus.PENDING, null);
        purchase(alice, "alice@example.com", "roadmap", PurchaseStatus.PENDING, "cs_dup");
        assertThatThrownBy(() -> purchase(bob, "bob@example.com", "roadmap", PurchaseStatus.PENDING, "cs_dup"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unknownStatusAndUnknownUserAreRejectedByTheDatabase() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO purchase (user_id, email, product, status, amount_cents, currency, created_at) "
                + "VALUES (?, 'a', 'roadmap', 'bogus', 1, 'EUR', CURRENT_TIMESTAMP)", alice))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO purchase (user_id, email, product, status, amount_cents, currency, created_at) "
                + "VALUES (999999, 'a', 'roadmap', 'paid', 1, 'EUR', CURRENT_TIMESTAMP)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void billingDisabledEntitlesEveryone() {
        EntitlementService off = new EntitlementService(
                new BillingSettings(false, "", "", new MockEnvironment()), users, purchases);
        assertThat(off.hasRoadmap("bob@example.com")).isTrue();
    }
}
