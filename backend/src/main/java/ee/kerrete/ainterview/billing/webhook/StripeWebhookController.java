package ee.kerrete.ainterview.billing.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * POST /api/billing/webhook (FP-62): public in SecurityConfig; authenticity comes only from the Stripe-Signature
 * header, verified against STRIPE_WEBHOOK_SECRET over the raw body (default 300 s tolerance). Any verification
 * problem is a bare 400; everything verified is acknowledged with 200 so Stripe stops retrying.
 */
@RestController
@RequestMapping("/api/billing")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final String webhookSecret;
    private final StripeWebhookService service;

    public StripeWebhookController(@Value("${billing.stripe.webhook-secret:}") String webhookSecret,
                                   StripeWebhookService service) {
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.service = service;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> webhook(@RequestBody(required = false) String payload,
                                                       @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (webhookSecret.isEmpty() || payload == null || signature == null || signature.isBlank()) {
            return invalid();
        }
        try {
            Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException | RuntimeException e) {
            log.warn("Stripe webhook rejected: {}", e.getClass().getSimpleName());
            return invalid();
        }
        service.handle(payload);
        return ResponseEntity.ok(Map.of("received", true));
    }

    private static ResponseEntity<Map<String, Object>> invalid() {
        return ResponseEntity.badRequest().body(Map.of("error", "invalid_signature"));
    }
}
