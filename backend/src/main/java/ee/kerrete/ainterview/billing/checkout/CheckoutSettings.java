package ee.kerrete.ainterview.billing.checkout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Stripe Checkout settings (FP-61). Secrets come only from env vars and are never logged or exposed.
 * <ul>
 *   <li>{@code billing.stripe.secret-key} (env STRIPE_SECRET_KEY): test-mode key ({@code sk_test_…}) outside prod.</li>
 *   <li>{@code billing.fallback-amount-cents} / {@code billing.currency}: used as {@code price_data} only while
 *   STRIPE_PRICE_ID is empty (local/dev), and as the expected amount on the pending row until Stripe reports the
 *   charged amount.</li>
 *   <li>{@code billing.frontend-base-url} (env FRONTEND_BASE_URL): base of the success/cancel urls.</li>
 *   <li>{@code billing.stripe.connect-timeout-ms} / {@code read-timeout-ms}: Stripe HTTP timeouts.</li>
 * </ul>
 */
@Component
public class CheckoutSettings {

    private final String secretKey;
    private final int fallbackAmountCents;
    private final String currency;
    private final String frontendBaseUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public CheckoutSettings(@Value("${billing.stripe.secret-key:}") String secretKey,
                            @Value("${billing.fallback-amount-cents:1900}") int fallbackAmountCents,
                            @Value("${billing.currency:eur}") String currency,
                            @Value("${billing.frontend-base-url:http://localhost:4200}") String frontendBaseUrl,
                            @Value("${billing.stripe.connect-timeout-ms:5000}") int connectTimeoutMs,
                            @Value("${billing.stripe.read-timeout-ms:20000}") int readTimeoutMs) {
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.fallbackAmountCents = fallbackAmountCents;
        this.currency = currency == null ? "" : currency.trim().toLowerCase(Locale.ROOT);
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        this.frontendBaseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    String secretKey() {
        return secretKey;
    }

    public boolean hasSecretKey() {
        return !secretKey.isEmpty();
    }

    public int fallbackAmountCents() {
        return fallbackAmountCents;
    }

    public String currency() {
        return currency;
    }

    public String successUrl() {
        return frontendBaseUrl + "/roadmap?paid=1";
    }

    public String cancelUrl() {
        return frontendBaseUrl + "/risk";
    }

    public int connectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int readTimeoutMs() {
        return readTimeoutMs;
    }

    @Override
    public String toString() {
        return "CheckoutSettings[secretKey=" + (hasSecretKey() ? "***" : "<unset>") + "]";
    }
}
