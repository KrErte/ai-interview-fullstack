package ee.kerrete.ainterview.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
 *   <li>{@code billing.stripe-price-id} (env STRIPE_PRICE_ID): the Stripe Price used at checkout (FP-61). No amount
 *   or currency is configured in code; purchase.amount_cents/currency only record what Stripe charged.</li>
 *   <li>{@code billing.dev-entitled-emails} / {@code billing.dev-entitled-roles}: users treated as entitled, honoured
 *   ONLY when the local or dev profile is active and prod is not (set in application-local.yml / -dev.yml).</li>
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
    private final boolean devProfile;
    private final Set<String> devEntitledEmails;
    private final Set<String> devEntitledRoles;

    public BillingSettings(@Value("${billing.enabled:false}") boolean enabled,
                           @Value("${billing.dev-entitled-emails:}") String devEntitledEmails,
                           @Value("${billing.dev-entitled-roles:}") String devEntitledRoles,
                           @Value("${billing.stripe-price-id:}") String stripePriceId,
                           Environment environment) {
        this.enabled = enabled;
        this.stripePriceId = stripePriceId == null ? "" : stripePriceId.trim();
        this.devProfile = environment.acceptsProfiles(Profiles.of("(local | dev) & !prod"));
        this.devEntitledEmails = split(devEntitledEmails);
        this.devEntitledRoles = split(devEntitledRoles);
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

    /** Stripe Price id for checkout (FP-61); empty when unset. */
    public String stripePriceId() {
        return stripePriceId;
    }

    /** Dev entitlement: only in local/dev (never with prod), for listed emails or roles (e.g. "admin"). */
    public boolean devEntitled(String email, String role) {
        if (!devProfile) {
            return false;
        }
        return (email != null && devEntitledEmails.contains(email.toLowerCase(Locale.ROOT)))
                || (role != null && devEntitledRoles.contains(role.toLowerCase(Locale.ROOT)));
    }
}
