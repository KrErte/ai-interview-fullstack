package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-16: runs the real Liquibase changelog on in-memory H2 (PostgreSQL mode, as the local profile)
 * and checks that the reserved current_role column was renamed and UserProfile round-trips.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:fp16_user_profile;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml"
})
class UserProfileRepositoryH2Test {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseRenamesReservedCurrentRoleColumn() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'user_profile'",
                String.class);

        assertThat(columns).contains("current_job_role");
        assertThat(columns).noneMatch(c -> c.equalsIgnoreCase("current_role"));
    }

    @Test
    void savesAndLoadsCurrentRole() {
        userProfileRepository.saveAndFlush(UserProfile.builder()
                .email("fp16@example.com")
                .fullName("FP16 Test")
                .currentRole("Mid-level Software Engineer")
                .targetRole("Senior Engineer")
                .build());

        UserProfile loaded = userProfileRepository.findByEmail("fp16@example.com").orElseThrow();
        assertThat(loaded.getCurrentRole()).isEqualTo("Mid-level Software Engineer");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_job_role FROM user_profile WHERE email = 'fp16@example.com'", String.class))
                .isEqualTo("Mid-level Software Engineer");
    }
}
