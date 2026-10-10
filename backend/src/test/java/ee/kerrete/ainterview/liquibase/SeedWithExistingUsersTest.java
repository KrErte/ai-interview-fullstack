package ee.kerrete.ainterview.liquibase;

import liquibase.ChecksumVersion;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-18 (QA repro): a database that already has users must never get demo seed data, even when the app
 * is later started with the "demo" context (local/dev profile). Before the fix the old
 * 007-seed-test-data.xml (inverted precondition: runs only if app_users is NOT empty) inserted
 * interviewer@/hr@demo.local with INTERVIEWER/HR roles in exactly this situation.
 * <p>
 * Runs the real master changelog with the Liquibase API on in-memory H2 (PostgreSQL mode), so the same
 * database can be migrated twice with different contexts.
 */
class SeedWithExistingUsersTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    private String url;
    /** connection for test SQL; each Liquibase run gets its own connection, like an app restart */
    private Connection connection;

    @BeforeEach
    void open() throws Exception {
        url = "jdbc:h2:mem:fp18-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(url, "sa", "");
    }

    @AfterEach
    void close() throws Exception {
        connection.close();
    }

    @FunctionalInterface
    private interface LiquibaseAction<T> {
        T run(Liquibase liquibase) throws Exception;
    }

    private <T> T withLiquibase(LiquibaseAction<T> action) throws Exception {
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            return action.run(new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), db));
        }
    }

    private void migrate(String contexts) throws Exception {
        withLiquibase(lb -> {
            lb.update(new Contexts(contexts), new LabelExpression());
            return null;
        });
    }

    private void exec(String sql) throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    private long count(String sql) throws Exception {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private List<String> strings(String sql) throws Exception {
        List<String> out = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    private void insertRealUser() throws Exception {
        exec("INSERT INTO app_users (email, password, full_name, role, enabled, created_at, updated_at) "
                + "VALUES ('real.user@example.com', 'x', 'Real User', 'CANDIDATE', TRUE, NOW(), NOW())");
    }

    @Test
    void prodStartThenRealUserThenDemoStartSeedsNothing() throws Exception {
        migrate("prod");          // fresh DB started with the prod profile
        insertRealUser();         // a real user registers
        migrate("demo");          // later started with the local/dev profile

        assertThat(strings("SELECT email FROM app_users")).containsExactly("real.user@example.com");
        assertThat(count("SELECT COUNT(*) FROM app_users WHERE role IN ('INTERVIEWER', 'HR')")).isZero();
        assertThat(count("SELECT COUNT(*) FROM training_progress")).isZero();

        // 007 is not part of the changelog any more
        assertThat(count("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '007-%'")).isZero();
        // the FP-18 demo seeds were evaluated but skipped by their preconditions
        assertThat(strings("SELECT EXECTYPE FROM DATABASECHANGELOG WHERE ID LIKE '2026-10-10-seed-demo-%' ORDER BY ID"))
                .containsExactly("MARK_RAN", "MARK_RAN");
    }

    @Test
    void existingUsersAndDemoContextOnFirstStartSeedNothing() throws Exception {
        // schema only (prod), user added, then the very first demo-context run
        migrate("prod");
        insertRealUser();
        exec("INSERT INTO app_users (email, password, full_name, role, enabled, created_at, updated_at) "
                + "VALUES ('second@example.com', 'x', 'Second', 'USER', TRUE, NOW(), NOW())");
        migrate("demo");

        assertThat(count("SELECT COUNT(*) FROM app_users WHERE email LIKE '%@demo.local'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM app_users")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM training_progress")).isZero();
    }

    @Test
    void freshDemoDatabaseStillGetsDemoUsersAndProgress() throws Exception {
        migrate("demo");

        assertThat(count("SELECT COUNT(*) FROM app_users WHERE email LIKE '%@demo.local'")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM training_progress")).isEqualTo(3);
        assertThat(strings("SELECT EXECTYPE FROM DATABASECHANGELOG WHERE ID LIKE '2026-10-10-seed-demo-%' ORDER BY ID"))
                .containsExactly("EXECUTED", "EXECUTED");
    }

    @Test
    void databaseWithLegacy007RowsStillMigratesAndValidates() throws Exception {
        migrate("prod");
        String filename = strings("SELECT FILENAME FROM DATABASECHANGELOG WHERE ID = '2025-12-06-create-roadmap-task-table'")
                .get(0).replace("2025-12-06-create-roadmap-task-table.xml", "007-seed-test-data.xml");
        // what an older database looks like: 007 was recorded (MARK_RAN) by a previous app version
        for (String id : List.of("007-seed-demo-users", "007-seed-related-data")) {
            exec("INSERT INTO DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, EXECTYPE, MD5SUM) "
                    + "VALUES ('" + id + "', 'seed', '" + filename + "', NOW(), 99, 'MARK_RAN', '9:00000000000000000000000000000000')");
        }

        withLiquibase(lb -> {
            lb.validate();
            return null;
        });
        migrate("demo");
        migrate("prod");

        assertThat(count("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '007-%'")).isEqualTo(2);
        List<ChangeSet> unrun = withLiquibase(lb -> lb.listUnrunChangeSets(new Contexts("demo"), new LabelExpression()));
        assertThat(unrun).isEmpty();
    }

    /**
     * The extra precondition on the demo progress seed must not change its checksum, otherwise databases
     * that already ran it (e.g. from feat/FP-22) would fail validation on startup. Liquibase 4 computes
     * checksums from the changes only (not from preconditions or comments).
     */
    @Test
    void demoSeedChecksumsAreUnchanged() throws Exception {
        // values computed from the file as first committed (before the extra precondition)
        Map<String, String> expected = Map.of(
                "2026-10-10-seed-demo-users", "9:56f7e655b0e4dbae2222e4bf13309aed",
                "2026-10-10-seed-demo-training-progress", "9:aad706e24ccf35cb695de1f49484dfc7");
        DatabaseChangeLog changeLog = withLiquibase(Liquibase::getDatabaseChangeLog);
        Map<String, String> actual = new HashMap<>();
        for (ChangeSet cs : changeLog.getChangeSets()) {
            if (expected.containsKey(cs.getId())) {
                actual.put(cs.getId(), cs.generateCheckSum(ChecksumVersion.latest()).toString());
            }
        }
        assertThat(actual).isEqualTo(expected);
    }
}
