package ee.kerrete.ainterview.billing;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/** Dev entitlement is honoured only with the local or dev profile, and never together with prod. */
class BillingSettingsTest {

    private static BillingSettings settings(String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return new BillingSettings(true, "dev@demo.local, Other@Demo.local", "ADMIN", env);
    }

    @Test
    void localAndDevHonourRolesAndEmails() {
        for (String profile : new String[]{"local", "dev"}) {
            BillingSettings s = settings(profile);
            assertThat(s.devEntitled("someone@demo.local", "ADMIN")).isTrue();
            assertThat(s.devEntitled("DEV@demo.local", "CANDIDATE")).isTrue();
            assertThat(s.devEntitled("other@demo.local", null)).isTrue();
            assertThat(s.devEntitled("someone@demo.local", "CANDIDATE")).isFalse();
        }
    }

    @Test
    void prodNeverHonoursDevEntitlement() {
        assertThat(settings("prod").devEntitled("dev@demo.local", "ADMIN")).isFalse();
        assertThat(settings("local", "prod").devEntitled("dev@demo.local", "ADMIN")).isFalse();
        assertThat(settings().devEntitled("dev@demo.local", "ADMIN")).isFalse();
    }

    @Test
    void emptyListsEntitleNobody() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        assertThat(new BillingSettings(true, "", "", env).devEntitled("a@b.c", "ADMIN")).isFalse();
    }
}
