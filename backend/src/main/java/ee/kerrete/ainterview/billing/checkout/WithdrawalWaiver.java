package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.billing.BillingSettings;

import java.util.Map;
import java.util.Optional;

/**
 * Confirmation text of each withdrawal-waiver version (FP-61). It goes into the Stripe PaymentIntent description and
 * the invoice footer, so the buyer's receipt records the waiver. A new wording needs a new version here and in
 * {@link BillingSettings#WAIVER_VERSIONS}; an existing text is never edited.
 */
public final class WithdrawalWaiver {

    private static final Map<String, String> CONFIRMATIONS = Map.of(
            BillingSettings.CURRENT_WAIVER_VERSION,
            "Customer agreed to immediate delivery and waived the 14-day right of withdrawal ("
                    + BillingSettings.CURRENT_WAIVER_VERSION + ").");

    private WithdrawalWaiver() {
    }

    public static Optional<String> confirmation(String version) {
        return Optional.ofNullable(version == null ? null : CONFIRMATIONS.get(version));
    }
}
