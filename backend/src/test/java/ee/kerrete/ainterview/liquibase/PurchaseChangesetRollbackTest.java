package ee.kerrete.ainterview.liquibase;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-60 QA: the purchase changesets roll back and re-apply on H2, and adding the rollback block left the
 * checksums equal to the ones QA's PostgreSQL database recorded (so already-migrated databases still validate).
 * <p>
 * Uses H2's default upper-case identifiers: with DATABASE_TO_UPPER=false Liquibase does not find its own
 * DATABASECHANGELOG on a second command (the known H2 second-start problem in the backlog).
 */
class PurchaseChangesetRollbackTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    @Test
    void purchaseChangesetsRollBackAndReapplyWithUnchangedChecksums() throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:h2:mem:fp60-rollback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), db);
            Contexts prod = new Contexts("prod");

            liquibase.update(prod, new LabelExpression());
            assertThat(tableExists(c)).isTrue();
            assertThat(md5(c, "2026-10-16-create-purchase-table")).isEqualTo("9:f88a59b157c7da1a5883f751e4ae1fe1");
            assertThat(md5(c, "2026-10-16-purchase-refund-waiver-columns")).isEqualTo("9:05f889140d6d83f4fa0c11fcfdc34cbf");
            assertThat(md5(c, "2026-10-16-purchase-one-paid-per-product")).as("postgresql only").isNull();

            // the two purchase changesets are the last ones applied under the prod context on H2
            liquibase.rollback(2, prod, new LabelExpression());
            assertThat(tableExists(c)).isFalse();
            assertThat(md5(c, "2026-10-16-create-purchase-table")).isNull();

            liquibase.update(prod, new LabelExpression());
            assertThat(tableExists(c)).isTrue();
            assertThat(columnExists(c, "refund_amount_cents")).isTrue();
        }
    }

    private static boolean tableExists(Connection c) throws Exception {
        try (ResultSet rs = c.getMetaData().getTables(null, null, "PURCHASE", null)) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection c, String column) throws Exception {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, "PURCHASE", column.toUpperCase())) {
            return rs.next();
        }
    }

    private static String md5(Connection c, String id) throws Exception {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT \"MD5SUM\" FROM \"DATABASECHANGELOG\" WHERE \"ID\" = '" + id + "'")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
