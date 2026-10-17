package ee.kerrete.ainterview.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Dev entitlement depends only on billing.dev-entitlement (no profile check). */
class BillingSettingsTest {

    @Test
    void switchedOnEntitlesAdminsAndListedEmails() {
        BillingSettings s = new BillingSettings(true, "dev@demo.local, Other@Demo.local", true, "", "€19");
        assertThat(s.devEntitled("someone@example.com", "ADMIN")).isTrue();
        assertThat(s.devEntitled("DEV@demo.local", "CANDIDATE")).isTrue();
        assertThat(s.devEntitled("other@demo.local", null)).isTrue();
        assertThat(s.devEntitled("someone@example.com", "CANDIDATE")).isFalse();
        assertThat(s.devEntitled("someone@example.com", "USER")).isFalse();
    }

    @Test
    void switchedOffEntitlesNobody() {
        BillingSettings s = new BillingSettings(true, "dev@demo.local", false, "", "€19");
        assertThat(s.devEntitled("dev@demo.local", "ADMIN")).isFalse();
        assertThat(s.devEntitled("someone@example.com", "ADMIN")).isFalse();
    }

    @Test
    void onlyTheCurrentWaiverVersionIsAccepted() {
        assertThat(BillingSettings.CURRENT_WAIVER_VERSION).isEqualTo("waiver-v1-2026-10-16");
        assertThat(BillingSettings.WAIVER_VERSIONS).containsExactly("waiver-v1-2026-10-16");
        assertThat(BillingSettings.isKnownWaiverVersion("waiver-v1-2026-10-16")).isTrue();
        assertThat(BillingSettings.isKnownWaiverVersion("waiver-v0")).isFalse();
        assertThat(BillingSettings.isKnownWaiverVersion(null)).isFalse();
    }
}
