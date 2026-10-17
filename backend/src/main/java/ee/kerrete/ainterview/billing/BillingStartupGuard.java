package ee.kerrete.ainterview.billing;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Fails startup when the dev entitlement (free roadmap for ADMIN users and listed emails) is switched on while the
 * prod profile is active. The message names only the setting, never any email.
 */
@Component
public class BillingStartupGuard {

    public BillingStartupGuard(BillingSettings settings, Environment environment) {
        if (settings.devEntitlementEnabled() && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("billing.dev-entitlement (BILLING_DEV_ENTITLEMENT) must not be true with "
                    + "the prod profile: it would give the paid roadmap away. Unset it or set it to false.");
        }
    }
}
