package ee.kerrete.ainterview.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-30: /actuator/health is public and shows only {"status"}; no other actuator endpoint is exposed;
 * the H2 console exists only when the local profile enables it.
 * <p>
 * Full application on a random port (the H2 console is its own servlet, which MockMvc never reaches),
 * with an in-memory H2 database per profile. The profile data seeders are mocked out.
 */
class ActuatorAndH2ConsoleSecurityTest {

    private static final String H2 = ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final ObjectMapper JSON = new ObjectMapper();

    abstract static class Base {
        @Autowired
        TestRestTemplate rest;
        @Autowired
        MockMvc mvc;
        @MockBean
        DataSeeder dataSeeder;
        @MockBean
        ProdDataSeeder prodDataSeeder;

        @Test
        void healthIsPublicAndShowsOnlyStatus() throws Exception {
            ResponseEntity<String> res = rest.getForEntity("/actuator/health", String.class);

            assertThat(res.getStatusCode().value()).isEqualTo(200);
            JsonNode body = JSON.readTree(res.getBody());
            assertThat(body.get("status").asText()).isEqualTo("UP");
            assertThat(body.size()).as("only 'status', no details/components: %s", body).isEqualTo(1);
        }

        @Test
        void healthSubPathsAreNotPublic() {
            assertThat(rest.getForEntity("/actuator/health/db", String.class).getStatusCode().value()).isEqualTo(401);
        }

        @Test
        void otherActuatorEndpointsNeedAuth() {
            for (String path : new String[]{"/actuator", "/actuator/info", "/actuator/mappings", "/actuator/env", "/actuator/beans"}) {
                assertThat(rest.getForEntity(path, String.class).getStatusCode().value()).as(path).isEqualTo(401);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void otherActuatorEndpointsAreNotExposedEvenForAdmins() throws Exception {
            for (String path : new String[]{"/actuator/info", "/actuator/mappings", "/actuator/env", "/actuator/beans",
                    "/actuator/configprops", "/actuator/loggers"}) {
                mvc.perform(get(path)).andExpect(status().isNotFound());
            }
        }
    }

    abstract static class ConsoleDisabled extends Base {
        @Test
        void h2ConsoleIsNotReachable() {
            for (String path : new String[]{"/h2-console", "/h2-console/", "/h2-console/login.jsp"}) {
                int code = rest.getForEntity(path, String.class).getStatusCode().value();
                assertThat(code).as(path).isIn(401, 404);
            }
        }

        @Test
        void framesAreDenied() {
            ResponseEntity<String> res = rest.getForEntity("/actuator/health", String.class);
            assertThat(res.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            // application.yml itself has no change-log setting (only the profiles do), see FP-30 report
            "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
            "spring.datasource.url=jdbc:h2:mem:fp30-default" + H2,
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password="
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("default")
    class DefaultProfile extends ConsoleDisabled {
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "spring.datasource.url=jdbc:h2:mem:fp30-prod" + H2,
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            // FP-28: the prod profile fails fast without a DB password and JWT secret
            "spring.datasource.password=test-only",
            "JWT_SECRET=test-only-jwt-secret-0123456789abcdef0123456789abcdef",
            // FP-61: prod + billing on needs Stripe keys; pinned so BILLING_ENABLED=true in the env doesn't break this
            "billing.enabled=false"
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    class ProdProfile extends ConsoleDisabled {
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "spring.datasource.url=jdbc:h2:mem:fp30-dev" + H2,
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password="
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    class DevProfile extends ConsoleDisabled {
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "spring.datasource.url=jdbc:h2:mem:fp30-local" + H2,
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password="
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("local")
    class LocalProfile extends Base {
        @Test
        void h2ConsoleIsReachableAndFramesAreSameOrigin() {
            ResponseEntity<String> res = rest.getForEntity("/h2-console/", String.class);

            assertThat(res.getStatusCode().value()).isEqualTo(200);
            assertThat(res.getBody()).containsIgnoringCase("H2 Console");
            assertThat(res.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        }
    }
}
