package ee.kerrete.ainterview.liquibase;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-18: the real Liquibase changelog runs on in-memory H2 (PostgreSQL mode, like the local profile).
 * Demo seed changesets (context "demo") must run for local/dev and never for prod.
 */
class DemoSeedContextTest {

    private static final String H2 = "MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";

    @Nested
    @DataJpaTest
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    @TestPropertySource(properties = {
            "spring.datasource.url=jdbc:h2:mem:fp18_demo;" + H2,
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.liquibase.contexts=demo"
    })
    class DemoContext {
        @Autowired
        JdbcTemplate jdbc;

        @Test
        void seedsDemoUsersAndProgressWithValidRolesAndPassword() {
            List<String> roles = jdbc.queryForList("SELECT role FROM app_users WHERE email LIKE '%@demo.local'", String.class);
            assertThat(roles).hasSize(5).allMatch(r -> List.of("ADMIN", "USER", "CANDIDATE").contains(r));

            String hash = jdbc.queryForObject("SELECT password FROM app_users WHERE email = 'candidate1@demo.local'", String.class);
            assertThat(new BCryptPasswordEncoder().matches("Test1234!", hash)).isTrue();

            List<String> statuses = jdbc.queryForList("SELECT status FROM training_progress", String.class);
            assertThat(statuses).hasSize(3).allMatch(s -> List.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED").contains(s));
        }
    }

    @Nested
    @DataJpaTest
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    @TestPropertySource(properties = {
            "spring.datasource.url=jdbc:h2:mem:fp18_prod;" + H2,
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.liquibase.contexts=prod"
    })
    class ProdContext {
        @Autowired
        JdbcTemplate jdbc;

        @Test
        void seedsNothing() {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM training_progress", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '%seed%'", Integer.class)).isZero();
        }
    }
}
