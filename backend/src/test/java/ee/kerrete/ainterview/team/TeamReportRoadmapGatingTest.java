package ee.kerrete.ainterview.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.AiReadinessCatalog;
import ee.kerrete.ainterview.aireadiness.AiReadinessScorer;
import ee.kerrete.ainterview.billing.BillingSettings;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.team.TeamModels.CreateTeamRequest;
import ee.kerrete.ainterview.team.TeamModels.CreatedTeam;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** FP-66: the anonymous team report (token link) carries no general-roadmap items while the roadmap is locked. */
class TeamReportRoadmapGatingTest {

    private static final Map<String, String> NEVER = Map.of("ai_usage", "never", "ai_depth", "none", "learning", "slow");

    @DataJpaTest(properties = {
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.public-base-url=https://example.org"
    })
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    @ImportAutoConfiguration(JacksonAutoConfiguration.class)
    @Import({TeamService.class, AiReadinessCatalog.class, AiReadinessScorer.class, BillingSettings.class,
            EntitlementService.class})
    abstract static class Base {
        @Autowired
        TeamService service;
        @Autowired
        AiReadinessCatalog catalog;
        @Autowired
        AiReadinessScorer scorer;
        @Autowired
        ObjectMapper json;

        String reportJson() throws Exception {
            CreatedTeam team = service.create(new CreateTeamRequest("Ops", "owner@example.org"));
            for (int i = 0; i < 5; i++) {
                service.record(scorer.score(catalog.profession("accountant").orElseThrow(), NEVER), NEVER, team.code(), null);
            }
            return json.writeValueAsString(service.report(team.adminToken()).orElseThrow());
        }

        List<String> generalItems() {
            return catalog.generalRoadmap().stream().flatMap(s -> s.items().stream()).toList();
        }
    }

    @Nested
    @TestPropertySource(properties = {"billing.enabled=true",
            "spring.datasource.url=jdbc:h2:mem:fp66-team-on;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"})
    class BillingOn extends Base {
        @Test
        void reportHasStageTitlesButNoRoadmapItems() throws Exception {
            String body = reportJson();
            assertThat(body).contains("Training content: ");
            for (String item : generalItems()) {
                assertThat(body).doesNotContain(item);
            }
        }
    }

    @Nested
    @TestPropertySource(properties = {"billing.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:fp66-team-off;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"})
    class BillingOff extends Base {
        @Test
        void reportStillCarriesTheFirstTwoItemsAsBefore() throws Exception {
            String body = reportJson();
            assertThat(generalItems().stream().filter(body::contains).count()).isEqualTo(2);
        }
    }
}
