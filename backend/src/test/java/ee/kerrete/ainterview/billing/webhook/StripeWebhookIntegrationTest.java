package ee.kerrete.ainterview.billing.webhook;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.config.DataSeeder;
import ee.kerrete.ainterview.config.ProdDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.util.Map;

import static ee.kerrete.ainterview.billing.webhook.StripeSignatureFixture.TEST_SECRET;
import static ee.kerrete.ainterview.billing.webhook.StripeSignatureFixture.sessionEvent;
import static ee.kerrete.ainterview.billing.webhook.StripeSignatureFixture.sign;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-62 end to end: real SecurityConfig, controller, signature check (fixtures signed with a test secret, no network),
 * Liquibase schema on H2, and the entitlement it unlocks.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp62-webhook;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "billing.enabled=true",
        "billing.dev-entitlement=false",
        "billing.stripe.webhook-secret=" + TEST_SECRET
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class StripeWebhookIntegrationTest {

    static final String EMAIL = "webhook.buyer@example.com";

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntitlementService entitlements;
    @MockBean
    DataSeeder dataSeeder;
    @MockBean
    ProdDataSeeder prodDataSeeder;

    long purchaseId;

    @BeforeEach
    void pendingPurchase() {
        jdbc.update("DELETE FROM purchase");
        jdbc.update("DELETE FROM app_users WHERE email = ?", EMAIL);
        jdbc.update("INSERT INTO app_users (email, password, role, enabled, created_at) VALUES (?, 'x', 'CANDIDATE', true, CURRENT_TIMESTAMP)", EMAIL);
        long userId = jdbc.queryForObject("SELECT id FROM app_users WHERE email = ?", Long.class, EMAIL);
        jdbc.update("INSERT INTO purchase (user_id, email, product, status, stripe_session_id, amount_cents, currency, "
                + "withdrawal_waiver_accepted, withdrawal_waiver_accepted_at, waiver_text_version, created_at) "
                + "VALUES (?, ?, 'roadmap', 'pending', 'cs_test_paid', 1900, 'EUR', true, CURRENT_TIMESTAMP, 'waiver-v1-2026-10-16', CURRENT_TIMESTAMP)",
                userId, EMAIL);
        purchaseId = jdbc.queryForObject("SELECT id FROM purchase", Long.class);
    }

    ResultActions deliver(String payload, String signature) throws Exception {
        var req = post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON).content(payload);
        if (signature != null) {
            req.header("Stripe-Signature", signature);
        }
        return mvc.perform(req);
    }

    Map<String, Object> row() {
        return jdbc.queryForMap("SELECT status, paid_at, buyer_country, stripe_session_id FROM purchase WHERE id = ?", purchaseId);
    }

    String completed(String sessionId, String paymentStatus) {
        return sessionEvent("evt_1", "checkout.session.completed", sessionId, paymentStatus, String.valueOf(purchaseId));
    }

    @Test
    void validSignedPaidEventMarksPaidAndUnlocks(CapturedOutput output) throws Exception {
        assertThat(entitlements.hasRoadmap(EMAIL)).isFalse();
        String payload = completed("cs_test_paid", "paid");

        deliver(payload, sign(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.received").value(true));

        assertThat(row()).containsEntry("status", "paid").containsEntry("buyer_country", "EE");
        assertThat(row().get("paid_at")).isNotNull();
        assertThat(entitlements.hasRoadmap(EMAIL)).isTrue();
        assertThat(output.getAll()).doesNotContain("payer@example.com").doesNotContain("Pat Payer").doesNotContain(EMAIL)
                .doesNotContain(TEST_SECRET);
    }

    @Test
    void duplicateDeliveryIsIdempotent() throws Exception {
        String payload = completed("cs_test_paid", "paid");
        deliver(payload, sign(payload)).andExpect(status().isOk());
        Timestamp firstPaidAt = (Timestamp) row().get("paid_at");

        deliver(payload, sign(payload)).andExpect(status().isOk());
        String async = sessionEvent("evt_2", "checkout.session.async_payment_succeeded", "cs_test_paid", "paid", String.valueOf(purchaseId));
        deliver(async, sign(async)).andExpect(status().isOk());

        assertThat(row().get("paid_at")).isEqualTo(firstPaidAt);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase WHERE status = 'paid'", Integer.class)).isEqualTo(1);
    }

    @Test
    void badOrMissingSignatureIs400WithoutChange() throws Exception {
        String payload = completed("cs_test_paid", "paid");
        deliver(payload, sign(payload, "whsec_wrong", System.currentTimeMillis() / 1000))
                .andExpect(status().isBadRequest()).andExpect(content().json("{\"error\":\"invalid_signature\"}", true));
        deliver(payload, null).andExpect(status().isBadRequest());
        deliver(payload, "garbage").andExpect(status().isBadRequest());
        // signed for another body
        deliver(payload, sign(payload.replace("\"paid\"", "\"unpaid\""))).andExpect(status().isBadRequest());
        // too old (default tolerance 300 s)
        deliver(payload, sign(payload, TEST_SECRET, System.currentTimeMillis() / 1000 - 3600)).andExpect(status().isBadRequest());

        assertThat(row()).containsEntry("status", "pending").containsEntry("paid_at", null);
        assertThat(entitlements.hasRoadmap(EMAIL)).isFalse();
    }

    @Test
    void unpaidSessionChangesNothing() throws Exception {
        String payload = completed("cs_test_paid", "unpaid");
        deliver(payload, sign(payload)).andExpect(status().isOk());
        assertThat(row()).containsEntry("status", "pending");
        assertThat(entitlements.hasRoadmap(EMAIL)).isFalse();
    }

    @Test
    void unrelatedEventIsAcknowledgedAndIgnored() throws Exception {
        String payload = sessionEvent("evt_3", "payment_intent.created", "cs_test_paid", "paid", String.valueOf(purchaseId));
        deliver(payload, sign(payload)).andExpect(status().isOk());
        String expired = sessionEvent("evt_4", "checkout.session.expired", "cs_test_paid", "paid", String.valueOf(purchaseId));
        deliver(expired, sign(expired)).andExpect(status().isOk());
        assertThat(row()).containsEntry("status", "pending");
    }

    @Test
    void unknownSessionIsAcknowledgedAndLogsOnlyTheSessionId(CapturedOutput output) throws Exception {
        String payload = sessionEvent("evt_5", "checkout.session.completed", "cs_test_unknown", "paid", "999999");
        deliver(payload, sign(payload)).andExpect(status().isOk());
        assertThat(row()).containsEntry("status", "pending");
        assertThat(output.getAll()).contains("cs_test_unknown").doesNotContain("payer@example.com");
    }

    @Test
    void fallsBackToMetadataPurchaseIdForAnOlderSession() throws Exception {
        String payload = completed("cs_test_older", "paid");
        deliver(payload, sign(payload)).andExpect(status().isOk());
        assertThat(row()).containsEntry("status", "paid").containsEntry("stripe_session_id", "cs_test_older");
    }

    @Test
    void secondPaidSessionOfAnEntitledUserIsLoggedAsDuplicateAtError() throws Exception {
        String first = completed("cs_test_paid", "paid");
        deliver(first, sign(first)).andExpect(status().isOk());
        long userId = jdbc.queryForObject("SELECT user_id FROM purchase WHERE id = ?", Long.class, purchaseId);
        jdbc.update("INSERT INTO purchase (user_id, email, product, status, stripe_session_id, amount_cents, currency, "
                + "withdrawal_waiver_accepted, created_at) VALUES (?, ?, 'roadmap', 'pending', 'cs_test_second', 1900, 'EUR', true, CURRENT_TIMESTAMP)",
                userId, EMAIL);
        long second = jdbc.queryForObject("SELECT id FROM purchase WHERE stripe_session_id = 'cs_test_second'", Long.class);

        Logger logger = (Logger) LoggerFactory.getLogger(PurchasePaymentRecorder.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            String dup = sessionEvent("evt_dup", "checkout.session.completed", "cs_test_second", "paid", String.valueOf(second));
            deliver(dup, sign(dup)).andExpect(status().isOk());
            // an older session of the already paid row, matched via metadata.purchase_id
            String older = completed("cs_test_older", "paid");
            deliver(older, sign(older)).andExpect(status().isOk());
        } finally {
            logger.detachAppender(logs);
        }

        assertThat(jdbc.queryForObject("SELECT status FROM purchase WHERE id = ?", String.class, second)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase WHERE status = 'paid'", Integer.class)).isEqualTo(1);
        assertThat(row()).containsEntry("stripe_session_id", "cs_test_paid");
        assertThat(logs.list).filteredOn(e -> e.getLevel() == Level.ERROR).extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        "duplicate payment, refund manually via the Stripe dashboard: session cs_test_second purchase " + second,
                        "duplicate payment, refund manually via the Stripe dashboard: session cs_test_older purchase " + purchaseId);
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(m -> m.contains("@") || m.contains("Pat Payer"));
    }

    @Test
    void refundedPurchaseIsNotRevived() throws Exception {
        jdbc.update("UPDATE purchase SET status = 'refunded' WHERE id = ?", purchaseId);
        String payload = completed("cs_test_paid", "paid");
        deliver(payload, sign(payload)).andExpect(status().isOk());
        assertThat(row()).containsEntry("status", "refunded");
    }

    @Test
    @WithMockUser(username = EMAIL)
    void successRedirectDoesNotUnlock() throws Exception {
        // the browser lands on {FRONTEND_BASE_URL}/roadmap?paid=1 and asks for the entitlement: still locked
        mvc.perform(get("/api/billing/entitlement").param("paid", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roadmap").value(false));
        mvc.perform(post("/api/ai-readiness/roadmap").param("paid", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionId\":\"translator\",\"answers\":{}}"))
                .andExpect(status().isPaymentRequired());
        assertThat(row()).containsEntry("status", "pending");
    }

    @Test
    void webhookNeedsNoLoginButGetIsNotPublic() throws Exception {
        mvc.perform(get("/api/billing/webhook")).andExpect(status().isUnauthorized());
    }
}
