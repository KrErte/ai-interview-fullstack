package ee.kerrete.ainterview.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** FP-28: prod has no default DB password and fails fast when it is missing. */
class ProdDatabasePasswordGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProdDatabasePasswordGuard.class);

    @Test
    void failsWhenPasswordMissing() {
        runner.withPropertyValues("spring.profiles.active=prod", "spring.datasource.password=")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("DB_PASSWORD is not set"));
    }

    @Test
    void startsWhenPasswordSet() {
        runner.withPropertyValues("spring.profiles.active=prod", "spring.datasource.password=from-env")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void prodYamlHasNoDefaultPassword() throws Exception {
        String yaml = new String(getClass().getResourceAsStream("/application-prod.yml").readAllBytes());
        assertThat(yaml).doesNotContain("ai123").contains("${DB_PASSWORD:${DB_PASS:}}");
    }
}
