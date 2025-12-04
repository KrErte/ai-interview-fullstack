package ee.kerrete.ainterview.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgreSQLDriverKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Custom PostgreSQL dialect that disables INSERT ... RETURNING support so that
 * Hibernate can interoperate with our H2 development database (H2 lacks support
 * for RETURNING even in PostgreSQL mode).
 */
public class H2FriendlyPostgreSQLDialect extends PostgreSQLDialect {

    public H2FriendlyPostgreSQLDialect() {
        super();
    }

    public H2FriendlyPostgreSQLDialect(DialectResolutionInfo info) {
        super(info);
    }

    public H2FriendlyPostgreSQLDialect(DatabaseVersion version) {
        super(version);
    }

    public H2FriendlyPostgreSQLDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
        super(version, driverKind);
    }

    @Override
    public boolean supportsInsertReturning() {
        return false;
    }
}

