package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.billing.BillingSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
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
        return checkout(key, "https://app.example.com");
    }

    private static CheckoutSettings checkout(String key, String baseUrl) {
        return new CheckoutSettings(key, 1900, "eur", baseUrl, 5000, 20000);
    }

    @Test
    void prodWithBillingNeedsAnExplicitHttpsPublicBaseUrl() {
        for (String bad : new String[]{"", "  ", "http://app.example.com", "https://localhost:4200", "https://127.0.0.1",
                "http://localhost:4200", "https://LOCALHOST", "https://[::1]:8080", "not a url", "https://"}) {
            assertThatThrownBy(() -> new CheckoutStartupGuard(billing(true, "price_1"), checkout("sk_x", bad), env("prod")))
                    .as(bad).isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_PUBLIC_BASE_URL")
                    .hasMessageNotContaining("STRIPE_PRICE_ID");
        }
        assertThatCode(() -> new CheckoutStartupGuard(billing(true, "price_1"), checkout("sk_x", "https://careerrisk.ee/"), env("prod")))
                .doesNotThrowAnyException();
        // non-prod keeps the localhost default
        assertThatCode(() -> new CheckoutStartupGuard(billing(true, ""), checkout("", "http://localhost:4200"), env("local")))
                .doesNotThrowAnyException();
    }

    @Test
    void prodYamlHasNoLocalhostDefaultSoAnUnsetUrlFailsStartup() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withInitializer(ctx -> {
                    try {
                        for (var ps : new org.springframework.boot.env.YamlPropertySourceLoader()
                                .load("prod", new org.springframework.core.io.ClassPathResource("application-prod.yml"))) {
                            ctx.getEnvironment().getPropertySources().addLast(ps);
                        }
                    } catch (java.io.IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .withPropertyValues("spring.profiles.active=prod", "billing.enabled=true", "billing.stripe-price-id=price_1",
                        "billing.stripe.secret-key=sk_test_x")
                .withUserConfiguration(BillingSettings.class, CheckoutSettings.class, CheckoutStartupGuard.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("APP_PUBLIC_BASE_URL");
                });
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
