package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.billing.BillingSettings;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * With the prod profile and billing.enabled=true, startup fails unless STRIPE_SECRET_KEY and STRIPE_PRICE_ID are set
 * (production charges the Stripe Price, never the price_data fallback). The message names settings only, no values.
 */
@Component
public class CheckoutStartupGuard {

    public CheckoutStartupGuard(BillingSettings billing, CheckoutSettings settings, Environment environment) {
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
        if (!missing.isEmpty()) {
            throw new IllegalStateException("BILLING_ENABLED=true with the prod profile needs " + String.join(" and ", missing)
                    + ". Set them or set BILLING_ENABLED=false.");
        }
    }
}
