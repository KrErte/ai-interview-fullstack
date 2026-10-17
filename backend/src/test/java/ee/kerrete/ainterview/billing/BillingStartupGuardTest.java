package ee.kerrete.ainterview.billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FP-60d: prod + billing.dev-entitlement=true must not start. */
class BillingStartupGuardTest {

    private static MockEnvironment env(String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    private static BillingSettings settings(boolean devEntitlement) {
        return new BillingSettings(true, "secret.person@example.com", devEntitlement, "");
    }

    @Test
    void prodWithTheFlagFailsWithoutNamingEmails() {
        assertThatThrownBy(() -> new BillingStartupGuard(settings(true), env("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("billing.dev-entitlement")
                .hasMessageNotContaining("@");
        assertThatThrownBy(() -> new BillingStartupGuard(settings(true), env("local", "prod")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prodWithoutTheFlagAndLocalWithTheFlagStart() {
        assertThatCode(() -> new BillingStartupGuard(settings(false), env("prod"))).doesNotThrowAnyException();
        assertThatCode(() -> new BillingStartupGuard(settings(true), env("local"))).doesNotThrowAnyException();
        assertThatCode(() -> new BillingStartupGuard(settings(true), env())).doesNotThrowAnyException();
    }

    @Test
    void springContextFailsOnlyForProdWithTheFlag() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(BillingSettings.class, BillingStartupGuard.class);
        runner.withPropertyValues("spring.profiles.active=prod", "billing.dev-entitlement=true",
                        "billing.dev-entitled-emails=secret.person@example.com")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause().hasMessageNotContaining("@");
                });
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(ctx -> assertThat(ctx).hasNotFailed());
        runner.withPropertyValues("spring.profiles.active=local", "billing.dev-entitlement=true")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }
}
