package ee.kerrete.ainterview.billing.webhook;

import com.stripe.net.Webhook;

/** Builds Stripe-Signature headers ("t=…,v1=HMAC-SHA256(secret, t + '.' + payload)") with a test secret; no network. */
final class StripeSignatureFixture {

    static final String TEST_SECRET = "whsec_test_fp62_fixture_secret";

    private StripeSignatureFixture() {
    }

    static String sign(String payload) {
        return sign(payload, TEST_SECRET, System.currentTimeMillis() / 1000);
    }

    static String sign(String payload, String secret, long timestamp) {
        try {
            return "t=" + timestamp + ",v1=" + Webhook.Util.computeHmacSha256(secret, timestamp + "." + payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** No client_reference_id, 1900 eur: matches the test purchases. */
    static String sessionEvent(String eventId, String type, String sessionId, String paymentStatus, String purchaseId) {
        return sessionEvent(eventId, type, sessionId, paymentStatus, purchaseId, null, 1900, "eur");
    }

    static String sessionEvent(String eventId, String type, String sessionId, String paymentStatus, String purchaseId,
                               String clientReferenceId, long amountTotal, String currency) {
        String clientRef = clientReferenceId == null ? "" : "\"client_reference_id\":\"" + clientReferenceId + "\",";
        return """
                {"id":"%s","object":"event","api_version":"2024-06-20","created":1760600000,"livemode":false,
                 "type":"%s","data":{"object":{"id":"%s","object":"checkout.session","mode":"payment",
                 "payment_status":"%s","amount_total":%d,"currency":"%s",%s
                 "metadata":{"purchase_id":"%s","product":"roadmap"},
                 "customer_details":{"email":"payer@example.com","name":"Pat Payer","address":{"country":"ee"}}}}}
                """.formatted(eventId, type, sessionId, paymentStatus, amountTotal, currency, clientRef, purchaseId);
    }
}
