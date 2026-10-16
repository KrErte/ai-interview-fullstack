package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.billing.BillingSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FP-61: prod + billing on needs STRIPE_SECRET_KEY and STRIPE_PRICE_ID. */
class CheckoutStartupGuardTest {

    private static MockEnvironment env(String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    private static BillingSettings billing(boolean enabled, String priceId) {
        return new BillingSettings(enabled, "", false, priceId, "€19");
    }

    private static CheckoutSettings checkout(String key) {
        return new CheckoutSettings(key, 1900, "eur", "http://localhost:4200", 5000, 20000);
    }

    @Test
    void prodWithBillingNeedsKeyAndPrice() {
        assertThatThrownBy(() -> new CheckoutStartupGuard(billing(true, ""), checkout(""), env("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_SECRET_KEY").hasMessageContaining("STRIPE_PRICE_ID");
        assertThatThrownBy(() -> new CheckoutStartupGuard(billing(true, "price_1"), checkout(""), env("prod")))
                .hasMessageContaining("STRIPE_SECRET_KEY").hasMessageNotContaining("STRIPE_PRICE_ID");
        assertThatThrownBy(() -> new CheckoutStartupGuard(billing(true, ""), checkout("sk_live_x"), env("prod")))
                .hasMessageContaining("STRIPE_PRICE_ID").hasMessageNotContaining("sk_live_x");
        assertThatCode(() -> new CheckoutStartupGuard(billing(true, "price_1"), checkout("sk_live_x"), env("prod")))
                .doesNotThrowAnyException();
    }

    @Test
    void billingOffOrNotProdStarts() {
        assertThatCode(() -> new CheckoutStartupGuard(billing(false, ""), checkout(""), env("prod"))).doesNotThrowAnyException();
        assertThatCode(() -> new CheckoutStartupGuard(billing(true, ""), checkout(""), env("local"))).doesNotThrowAnyException();
        assertThatCode(() -> new CheckoutStartupGuard(billing(true, ""), checkout(""), env())).doesNotThrowAnyException();
    }
}
