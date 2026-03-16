package org.javers.repository.sql.session;

import org.javers.repository.sql.ConnectionProvider;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.SqlRepositoryConfiguration;

public class SessionFactory {
    private final Dialect dialect;
    private final ConnectionProvider connectionProvider;
    private final KeyGenerator keyGenerator;

    public SessionFactory(DialectName dialectName, ConnectionProvider connectionProvider, SqlRepositoryConfiguration sqlRepositoryConfiguration) {
        this.dialect = Dialects.fromName(dialectName);
        this.connectionProvider = connectionProvider;
        this.keyGenerator = dialect.getKeyGeneratorDefinition().createKeyGenerator(sqlRepositoryConfiguration);
    }

    public Session create(String sessionName) {
        return new Session(dialect, keyGenerator, connectionProvider, sessionName);
    }

    public void resetKeyGeneratorCache() {
        keyGenerator.reset();
    }
}
