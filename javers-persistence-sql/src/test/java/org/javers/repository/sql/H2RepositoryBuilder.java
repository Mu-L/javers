package org.javers.repository.sql;

import org.javers.repository.sql.codecs.CdoSnapshotStateCodec;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author bartosz.walacik
 */
public class H2RepositoryBuilder {

    private final SqlRepositoryBuilder sqlRepository;
    private Connection conn;

    public H2RepositoryBuilder() {
        sqlRepository = SqlRepositoryBuilder.sqlRepository();
    }

    public H2RepositoryBuilder withCdoSnapshotStateCodec(CdoSnapshotStateCodec cdoSnapshotStateCodec) {
        sqlRepository.withCdoSnapshotStateCodec(cdoSnapshotStateCodec);
        return this;
    }

    public H2RepositoryBuilder withSequenceAllocationEnabled(boolean sequenceAllocationEnabled) {
        sqlRepository.withSequenceAllocationEnabled(sequenceAllocationEnabled);
        return this;
    }

    public Connection getConn() {
        return conn;
    }


    public JaversSqlRepository build() {
        try {
            conn = DriverManager.getConnection("jdbc:h2:mem:test;");

            return sqlRepository.
                    withConnectionProvider(() -> conn).
                    withDialect(DialectName.H2).
                    build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
