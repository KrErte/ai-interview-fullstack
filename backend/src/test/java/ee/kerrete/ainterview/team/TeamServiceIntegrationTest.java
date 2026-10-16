package ee.kerrete.ainterview.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.AiReadinessCatalog;
import ee.kerrete.ainterview.aireadiness.AiReadinessScorer;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.TeamStatus;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.team.TeamModels.CreateTeamRequest;
import ee.kerrete.ainterview.team.TeamModels.CreatedTeam;
import ee.kerrete.ainterview.team.TeamModels.ProfessionGroup;
import ee.kerrete.ainterview.team.TeamModels.TeamReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meeskonnaraport päris Liquibase'i tabelitega (H2, PostgreSQL režiim): loomine, anonüümsed vastused,
 * piirangud, raporti reeglid ja kustutamine.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:team-report;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.public-base-url=https://example.org"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({TeamService.class, AiReadinessCatalog.class, AiReadinessScorer.class})
class TeamServiceIntegrationTest {

    private static final Map<String, String> ANSWERS = Map.of("ai_usage", "never", "ai_depth", "none", "learning", "slow");
    private static final Map<String, String> AI_USERS = Map.of("ai_usage", "daily", "ai_depth", "builder", "learning", "fast");

    @Autowired
    TeamService service;
    @Autowired
    TeamRepository teams;
    @Autowired
    TeamSubmissionRepository submissions;
    @Autowired
    AiReadinessCatalog catalog;
    @Autowired
    AiReadinessScorer scorer;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper json;

    private CreatedTeam team() {
        return service.create(new CreateTeamRequest("Finance team", "owner@example.org"));
    }

    private TeamStatus answer(CreatedTeam team, String professionId, Map<String, String> answers) {
        Profession p = catalog.profession(professionId).orElseThrow();
        AssessmentResult result = scorer.score(p, answers);
        return service.record(result, answers, team.code(), null);
    }

    private void answers(CreatedTeam team, String professionId, int count) {
        for (int i = 0; i < count; i++) {
            answer(team, professionId, ANSWERS);
        }
    }

    @Test
    void createdTeamStoresOnlyTheTokenHash() {
        CreatedTeam created = team();
        assertThat(created.code()).matches("[A-Z2-7]{10}");
        assertThat(created.adminToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(created.inviteUrl()).isEqualTo("https://example.org/ai-test?team=" + created.code());
        assertThat(created.reportUrl()).isEqualTo("https://example.org/teams/report#" + created.adminToken());
        assertThat(created.maxMembers()).isEqualTo(50);

        Team stored = teams.findByCode(created.code()).orElseThrow();
        assertThat(stored.getAdminTokenHash()).isEqualTo(TeamService.sha256(created.adminToken()));
        Integer withToken = jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE admin_token_hash = ? OR code = ?",
                Integer.class, created.adminToken(), created.adminToken());
        assertThat(withToken).isZero();
    }

    @Test
    void codesAreUnique() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            codes.add(team().code());
        }
        assertThat(codes).hasSize(20);
    }

    @Test
    void joinShowsNameAndWhetherTheTeamIsFull() {
        CreatedTeam created = team();
        assertThat(service.invite(created.code().toLowerCase())).hasValueSatisfying(i -> {
            assertThat(i.name()).isEqualTo("Finance team");
            assertThat(i.full()).isFalse();
        });
        assertThat(service.invite("NOSUCHCODE")).isEmpty();
    }

    @Test
    void submissionNeedsAValidCodeAndStopsAtTheLimit() {
        CreatedTeam created = team();
        Team stored = teams.findByCode(created.code()).orElseThrow();
        stored.setMaxMembers(2);
        teams.save(stored);

        assertThat(answer(created, "accountant", ANSWERS).status()).isEqualTo("added");
        assertThat(answer(created, "accountant", ANSWERS).status()).isEqualTo("added");
        TeamStatus third = answer(created, "accountant", ANSWERS);
        assertThat(third.status()).isEqualTo("full");
        assertThat(third.editToken()).isNull();
        assertThat(submissions.countByTeamId(stored.getId())).isEqualTo(2);
        assertThat(service.invite(created.code()).orElseThrow().full()).isTrue();

        AssessmentResult result = scorer.score(catalog.profession("accountant").orElseThrow(), ANSWERS);
        assertThat(service.record(result, ANSWERS, "NOSUCHCODE", null).status()).isEqualTo("not-found");
        assertThat(submissions.count()).isEqualTo(2);
    }

    @Test
    void submissionStoresOnlyTestContentAndADate() {
        CreatedTeam created = team();
        TeamStatus status = answer(created, "accountant", ANSWERS);
        assertThat(status.editToken()).isNotBlank();
        TeamSubmission row = submissions.findAll().get(0);
        assertThat(row.getEditTokenHash()).isEqualTo(TeamService.sha256(status.editToken()));
        assertThat(row.getProfessionId()).isEqualTo("accountant");
        assertThat(row.getSubmittedOn()).isNotNull();
        List<String> columns = jdbc.queryForList(
                "SELECT LOWER(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE LOWER(TABLE_NAME) = 'team_submission'",
                String.class);
        assertThat(columns).doesNotContain("email", "ip", "name", "user_agent", "created_at");
    }

    @Test
    void theEditTokenUpdatesTheSameAnswerWithTheSlidersShares() {
        CreatedTeam created = team();
        TeamStatus added = answer(created, "translator", ANSWERS);
        Profession translator = catalog.profession("translator").orElseThrow();
        Map<String, Integer> shares = new HashMap<>();
        translator.tasks().forEach(t -> shares.put(t.name(), 0));
        shares.put(translator.tasks().get(0).name(), 100);
        AssessmentResult mine = scorer.score(translator, ANSWERS, shares);

        TeamStatus updated = service.record(mine, ANSWERS, created.code(), added.editToken());
        assertThat(updated.status()).isEqualTo("updated");
        assertThat(updated.teamName()).isEqualTo("Finance team");
        assertThat(submissions.count()).isEqualTo(1);
        TeamSubmission row = submissions.findAll().get(0);
        assertThat(row.getTaskExposure()).isEqualTo(mine.taskExposureScore());
        assertThat(row.getTasks()).contains("\"share\":100");
    }

    @Test
    void belowFiveTheReportIsOnlyACounter() {
        CreatedTeam created = team();
        answers(created, "accountant", 4);
        TeamReport report = service.report(created.adminToken()).orElseThrow();
        assertThat(report.ready()).isFalse();
        assertThat(report.respondents()).isEqualTo(4);
        assertThat(report.minRespondents()).isEqualTo(5);
        assertThat(report.averageReplaceability()).isNull();
        assertThat(report.bands()).isEmpty();
        assertThat(report.topTasks()).isEmpty();
        assertThat(report.aiUse()).isEmpty();
        assertThat(report.professions()).isEmpty();
        assertThat(report.focus()).isEmpty();
    }

    @Test
    void fromFiveTheReportHasTheAggregate() {
        CreatedTeam created = team();
        answers(created, "accountant", 3);
        for (int i = 0; i < 2; i++) {
            answer(created, "accountant", AI_USERS);
        }
        TeamReport report = service.report(created.adminToken()).orElseThrow();
        assertThat(report.ready()).isTrue();
        assertThat(report.respondents()).isEqualTo(5);
        assertThat(report.firstDate()).isNotNull();
        assertThat(report.averageReplaceability()).isNotNull();
        assertThat(report.bands()).hasSize(4);
        assertThat(report.bands().stream().mapToInt(TeamModels.Percent::percent).sum()).isBetween(99, 101);
        assertThat(report.topTasks()).isNotEmpty().hasSizeLessThanOrEqualTo(10);
        assertThat(report.topTasks()).allSatisfy(t -> assertThat(t.respondents()).isGreaterThanOrEqualTo(5));
        assertThat(report.aiUse()).extracting(TeamModels.AnswerDistribution::questionId)
                .containsExactly("ai_usage", "ai_depth", "learning");
        // 3 of 5 never use AI: 60% >= 40%, so the literacy session is recommended
        assertThat(report.focus()).anyMatch(f -> f.startsWith("Basic AI literacy session for everyone: 60%"));
        assertThat(report.focus()).anyMatch(f -> f.startsWith("Start with AI for: "));
        assertThat(report.professions()).extracting(ProfessionGroup::name).containsExactly("Accountant");
    }

    @Test
    void professionGroupsUnderFiveAreMergedOrHidden() {
        CreatedTeam both = team();
        answers(both, "accountant", 5);
        answers(both, "translator", 5);
        assertThat(service.report(both.adminToken()).orElseThrow().professions())
                .extracting(ProfessionGroup::name).containsExactly("Accountant", "Translator");

        CreatedTeam smallRest = team();
        answers(smallRest, "accountant", 5);
        answers(smallRest, "translator", 2);
        // 2 others could be derived from the total minus the accountants, so no breakdown at all
        assertThat(service.report(smallRest.adminToken()).orElseThrow().professions()).isEmpty();

        CreatedTeam noGroup = team();
        answers(noGroup, "accountant", 4);
        answers(noGroup, "translator", 2);
        // no profession reaches 5, and "Other professions" alone would only repeat the totals
        assertThat(service.report(noGroup.adminToken()).orElseThrow().professions()).isEmpty();

        CreatedTeam other = team();
        answers(other, "accountant", 5);
        answers(other, "translator", 3);
        answers(other, "software-developer", 2);
        assertThat(service.report(other.adminToken()).orElseThrow().professions())
                .extracting(ProfessionGroup::name, ProfessionGroup::respondents)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Accountant", 5),
                        org.assertj.core.groups.Tuple.tuple("Other professions", 5));
    }

    @Test
    void tasksOfFewerThanFivePeopleAreLeftOut() {
        CreatedTeam created = team();
        answers(created, "accountant", 5);
        answers(created, "translator", 1);
        TeamReport report = service.report(created.adminToken()).orElseThrow();
        Set<String> translatorTasks = new HashSet<>();
        catalog.profession("translator").orElseThrow().tasks().forEach(t -> translatorTasks.add(t.name()));
        Set<String> accountantTasks = new HashSet<>();
        catalog.profession("accountant").orElseThrow().tasks().forEach(t -> accountantTasks.add(t.name()));
        translatorTasks.removeAll(accountantTasks);
        assertThat(report.topTasks()).noneMatch(t -> translatorTasks.contains(t.name()));
    }

    @Test
    void theReportJsonHasNoIndividualAnswersEmailOrTokens() throws Exception {
        CreatedTeam created = team();
        String editToken = answer(created, "accountant", ANSWERS).editToken();
        answers(created, "accountant", 5);
        String body = json.writeValueAsString(service.report(created.adminToken()).orElseThrow());
        assertThat(body).doesNotContain("owner@example.org", "ownerEmail", created.adminToken(), editToken,
                "editToken", "\"answers\"", "submittedOn");
    }

    @Test
    void assessWithATeamCodeAddsTheAnonymousResult() {
        CreatedTeam created = team();
        var readiness = new ee.kerrete.ainterview.aireadiness.AiReadinessService(catalog, scorer,
                new ee.kerrete.ainterview.aireadiness.AiReadinessCoach(json), service);
        var request = new ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentRequest(
                "accountant", ANSWERS, false, null, created.code(), null);
        AssessmentResult result = readiness.assess(request);
        assertThat(result.team().status()).isEqualTo("added");
        assertThat(result.team().teamName()).isEqualTo("Finance team");
        assertThat(result.replaceabilityScore())
                .isEqualTo(scorer.score(catalog.profession("accountant").orElseThrow(), ANSWERS).replaceabilityScore());

        var without = readiness.assess(new ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentRequest(
                "accountant", ANSWERS, false));
        assertThat(without.team()).isNull();
        assertThat(submissions.count()).isEqualTo(1);
    }

    @Test
    void wrongTokenFindsNothing() {
        team();
        assertThat(service.report("not-a-token")).isEmpty();
        assertThat(service.report("")).isEmpty();
        assertThat(service.delete("not-a-token")).isFalse();
    }

    @Test
    void deleteRemovesTheTeamAndEveryAnswer() {
        CreatedTeam created = team();
        CreatedTeam keep = team();
        answers(created, "accountant", 6);
        answers(keep, "accountant", 1);
        assertThat(service.delete(created.adminToken())).isTrue();
        assertThat(teams.findByCode(created.code())).isEmpty();
        assertThat(service.report(created.adminToken())).isEmpty();
        Long keptTeamId = teams.findByCode(keep.code()).orElseThrow().getId();
        assertThat(submissions.count()).isEqualTo(1);
        assertThat(submissions.countByTeamId(keptTeamId)).isEqualTo(1);
    }
}
