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
 *   <li>{@code billing.dev-entitled-emails} / {@code billing.dev-entitled-roles}: users treated as entitled, honoured
 *   ONLY when the local or dev profile is active and prod is not (set in application-local.yml / -dev.yml).</li>
 * </ul>
 */
@Component
public class BillingSettings {

    private final boolean enabled;
    private final boolean devProfile;
    private final Set<String> devEntitledEmails;
    private final Set<String> devEntitledRoles;

    public BillingSettings(@Value("${billing.enabled:false}") boolean enabled,
                           @Value("${billing.dev-entitled-emails:}") String devEntitledEmails,
                           @Value("${billing.dev-entitled-roles:}") String devEntitledRoles,
                           Environment environment) {
        this.enabled = enabled;
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

    /** Dev entitlement: only in local/dev (never with prod), for listed emails or roles (e.g. "admin"). */
    public boolean devEntitled(String email, String role) {
        if (!devProfile) {
            return false;
        }
        return (email != null && devEntitledEmails.contains(email.toLowerCase(Locale.ROOT)))
                || (role != null && devEntitledRoles.contains(role.toLowerCase(Locale.ROOT)));
    }
}
