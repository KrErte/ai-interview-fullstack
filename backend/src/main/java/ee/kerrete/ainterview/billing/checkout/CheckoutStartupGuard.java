package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.billing.BillingSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * With the prod profile and billing.enabled=true, startup fails unless STRIPE_SECRET_KEY, STRIPE_PRICE_ID and
 * STRIPE_WEBHOOK_SECRET (FP-62, else no purchase could ever be marked paid) are set and app.public-base-url
 * (APP_PUBLIC_BASE_URL) is an explicit https, non-localhost url (Stripe returns buyers there)
 * (production charges the Stripe Price, never the price_data fallback). The message names settings only, no values.
 */
@Component
public class CheckoutStartupGuard {

    @Autowired
    public CheckoutStartupGuard(BillingSettings billing, CheckoutSettings settings, Environment environment) {
        this(billing, settings, environment.getProperty("billing.stripe.webhook-secret", ""), environment);
    }

    CheckoutStartupGuard(BillingSettings billing, CheckoutSettings settings, String webhookSecret, Environment environment) {
        if (!billing.enabled() || !environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        List<String> missing = new ArrayList<>();
        if (!settings.hasSecretKey()) {
            missing.add("STRIPE_SECRET_KEY");
        }
        if (billing.stripePriceId().isEmpty()) {
            missing.add("STRIPE_PRICE_ID");
        }
        if (webhookSecret == null || webhookSecret.isBlank()) {
            missing.add("STRIPE_WEBHOOK_SECRET");
        }
        if (!isPublicHttpsUrl(settings.publicBaseUrl())) {
            missing.add("APP_PUBLIC_BASE_URL (https, not localhost)");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("BILLING_ENABLED=true with the prod profile needs " + String.join(", ", missing)
                    + ". Set them or set BILLING_ENABLED=false.");
        }
    }

    /** https with a real host: not localhost, 127.0.0.1, ::1 or 0.0.0.0. */
    static boolean isPublicHttpsUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            java.net.URI uri = new java.net.URI(url.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) {
                return false;
            }
            host = host.toLowerCase(java.util.Locale.ROOT);
            return !(host.equals("localhost") || host.endsWith(".localhost") || host.startsWith("127.")
                    || host.equals("[::1]") || host.equals("::1") || host.equals("0.0.0.0"));
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }
}
