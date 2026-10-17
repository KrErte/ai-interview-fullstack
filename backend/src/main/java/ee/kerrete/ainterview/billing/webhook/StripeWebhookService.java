package ee.kerrete.ainterview.billing.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Handles verified Stripe events (FP-62). Fields are read from the verified raw JSON, so the result does not depend
 * on the SDK's API version. Only checkout.session.completed / async_payment_succeeded with payment_status=paid and
 * charge.refunded act; everything else is acknowledged and ignored. Logs carry event/session/purchase ids only,
 * never email or name.
 */
@Service
public class StripeWebhookService {

    static final Set<String> PAID_EVENTS = Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded");
    static final String REFUND_EVENT = "charge.refunded";

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final PurchasePaymentRecorder recorder;

    public StripeWebhookService(PurchasePaymentRecorder recorder) {
        this.recorder = recorder;
    }

    public void handle(String payload) {
        JsonNode event;
        try {
            event = JSON.readTree(payload);
        } catch (Exception e) {
            log.warn("Stripe webhook: unreadable payload");
            return;
        }
        String type = event.path("type").asText("");
        if (REFUND_EVENT.equals(type)) {
            handleRefund(event.path("data").path("object"));
            return;
        }
        if (!PAID_EVENTS.contains(type)) {
            return;
        }
        JsonNode session = event.path("data").path("object");
        String sessionId = text(session.path("id"));
        if (sessionId == null) {
            log.warn("Stripe webhook {}: no session id", type);
            return;
        }
        if (!"paid".equals(session.path("payment_status").asText(""))) {
            log.info("Stripe webhook {}: session {} not paid yet", type, sessionId);
            return;
        }
        Long purchaseId = null;
        String rawPurchaseId = text(session.path("metadata").path("purchase_id"));
        if (rawPurchaseId != null) {
            try {
                purchaseId = Long.valueOf(rawPurchaseId);
            } catch (NumberFormatException ignored) {
                // stays null; the session id alone decides
            }
        }
        String country = text(session.path("customer_details").path("address").path("country"));
        if (country != null) {
            country = country.toUpperCase(Locale.ROOT);
            if (!country.matches("[A-Z]{2}")) {
                country = null;
            }
        }
        Integer amount = session.path("amount_total").isIntegralNumber() ? session.path("amount_total").intValue() : null;
        String currency = text(session.path("currency"));
        recorder.recordPaid(new PurchasePaymentRecorder.Payment(sessionId, purchaseId,
                text(session.path("client_reference_id")), country, amount,
                currency == null ? null : currency.toUpperCase(Locale.ROOT), text(session.path("payment_intent"))));
    }

    /** charge.refunded: the charge's PaymentIntent, the refunded total so far and whether the whole charge is refunded. */
    private void handleRefund(JsonNode charge) {
        String paymentIntentId = text(charge.path("payment_intent"));
        if (paymentIntentId == null || !charge.path("amount_refunded").isIntegralNumber()) {
            log.warn("Stripe webhook {}: no payment intent or refunded amount", REFUND_EVENT);
            return;
        }
        recorder.recordRefund(new PurchasePaymentRecorder.Refund(paymentIntentId,
                charge.path("amount_refunded").longValue(), charge.path("refunded").asBoolean(false)));
    }

    private static String text(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }
}
