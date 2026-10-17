package ee.kerrete.ainterview.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Billing switches (FP-60).
 * <ul>
 *   <li>{@code billing.enabled} (env BILLING_ENABLED, default false): while false nothing is gated and nothing is
 *   stripped from public responses, so deployments without Stripe behave exactly as before.</li>
 *   <li>{@code billing.stripe-price-id} (env STRIPE_PRICE_ID): the Stripe Price used at checkout (FP-61); when
 *   empty, checkout falls back to price_data from billing.fallback-amount-cents/currency (CheckoutSettings).</li>
 *   <li>{@code billing.display-price} (env BILLING_DISPLAY_PRICE, default "€19"): teaser text only.</li>
 *   <li>{@code billing.dev-entitlement} (env BILLING_DEV_ENTITLEMENT, default false, independent of the profile):
 *   when true, ADMIN users and the emails in {@code billing.dev-entitled-emails} (env BILLING_DEV_ENTITLED_EMAILS)
 *   count as entitled; when false, nobody does. Never set in a committed application*.yml.</li>
 * </ul>
 */
@Component
public class BillingSettings {

    /** Current withdrawal-waiver text version (FP-61 checkout body withdrawalWaiverVersion, stored in waiver_text_version). */
    public static final String CURRENT_WAIVER_VERSION = "waiver-v1-2026-10-16";

    /** Accepted waiver versions; anything else is 400 at checkout (FP-61). */
    public static final Set<String> WAIVER_VERSIONS = Set.of(CURRENT_WAIVER_VERSION);

    private final boolean enabled;
    private final String stripePriceId;
    /** Role that is entitled when the dev entitlement is switched on. */
    static final String DEV_ENTITLED_ROLE = "ADMIN";

    private final String displayPrice;
    private final boolean devEntitlement;
    private final Set<String> devEntitledEmails;

    public BillingSettings(@Value("${billing.enabled:false}") boolean enabled,
                           @Value("${billing.dev-entitled-emails:}") String devEntitledEmails,
                           @Value("${billing.dev-entitlement:false}") boolean devEntitlement,
                           @Value("${billing.stripe-price-id:}") String stripePriceId,
                           @Value("${billing.display-price:€19}") String displayPrice) {
        this.displayPrice = displayPrice;
        this.enabled = enabled;
        this.stripePriceId = stripePriceId == null ? "" : stripePriceId.trim();
        this.devEntitlement = devEntitlement;
        this.devEntitledEmails = split(devEntitledEmails == null ? "" : devEntitledEmails);
    }

    private static Set<String> split(String csv) {
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean enabled() {
        return enabled;
    }

    public static boolean isKnownWaiverVersion(String version) {
        return version != null && WAIVER_VERSIONS.contains(version);
    }

    /** Price text shown on public teasers (display only; the charged price comes from the Stripe Price). */
    public String displayPrice() {
        return displayPrice;
    }

    /** Stripe Price id for checkout (FP-61); empty when unset. */
    public String stripePriceId() {
        return stripePriceId;
    }

    /** Whether billing.dev-entitlement is switched on (see {@link BillingStartupGuard}). */
    public boolean devEntitlementEnabled() {
        return devEntitlement;
    }

    /** Dev entitlement: only when billing.dev-entitlement=true; then ADMIN users and the listed emails. */
    public boolean devEntitled(String email, String role) {
        if (!devEntitlement) {
            return false;
        }
        return DEV_ENTITLED_ROLE.equals(role)
                || (email != null && devEntitledEmails.contains(email.toLowerCase(Locale.ROOT)));
    }
}
