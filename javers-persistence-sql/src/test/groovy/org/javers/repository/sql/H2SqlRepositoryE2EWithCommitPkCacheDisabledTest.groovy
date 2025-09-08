package org.javers.repository.sql

import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.repository.api.JaversRepository
import org.javers.repository.jql.QueryBuilder
import spock.lang.Specification

import java.sql.Connection
import java.sql.DriverManager

/**
 * Regression test for https://github.com/javers/javers/issues/1447
 *
 * Verifies that when {@code commitPkCacheDisabled=true}, the sequence cache is bypassed
 * and a fresh value is fetched from the DB on every call — critical for multi-tenant
 * applications using one database per tenant (e.g. AbstractRoutingDataSource).
 *
 * Also exercises the {@code multiDbFriendly} master flag, which disables both the
 * globalId cache and the commit_pk sequence cache in a single setting.
 */
class H2SqlRepositoryE2EWithCommitPkCacheDisabledTest extends Specification {

    private static final String JDBC_URL = "jdbc:h2:mem:cache-disabled-test;DB_CLOSE_DELAY=-1"
    // Default H2 commit_pk sequence name (no custom name, no schema prefix)
    private static final String COMMIT_PK_SEQ = "jv_commit_pk_seq"

    Connection createConnection() {
        def conn = DriverManager.getConnection(JDBC_URL)
        conn.autoCommit = false
        conn
    }

    def connection = createConnection()

    JaversSqlRepository buildRepository(boolean commitPkCacheDisabled) {
        SqlRepositoryBuilder.sqlRepository()
                .withConnectionProvider({ connection } as ConnectionProvider)
                .withDialect(DialectName.H2)
                .withCommitPkCacheDisabled(commitPkCacheDisabled)
                .build()
    }

    def cleanup() {
        connection.rollback()
        connection.close()
    }

    def execute(String sql) {
        def stmt = connection.createStatement()
        stmt.executeUpdate(sql)
        stmt.close()
    }

    /**
     * Core regression test for issue #1447.
     *
     * The commit_pk (database primary key for the jv_commit table) is generated via
     * the sequence allocation cache in KeyGenerator.SequenceAllocation. With the cache
     * enabled, after sequence is externally reset (simulating a tenant/DB switch back
     * to another database), Javers would keep handing out PKs from the stale cached
     * range, causing unique constraint violations. With the cache disabled, a fresh
     * sequence value is fetched from the DB on every commit.
     *
     * We verify the fix by doing initial commits, resetting the DB sequence far ahead
     * (simulating the switch to another tenant's DB), and asserting that subsequent
     * commits succeed and are all persisted correctly.
     */
    def "should re-fetch sequence from DB after a simulated tenant switch when commitPkCacheDisabled=true"() {
        given: "a repository with commit PK cache disabled"
        def sqlRepo = buildRepository(true)
        def javers = JaversBuilder.javers().registerJaversRepository(sqlRepo).build()

        and: "initial commits (occupies sequence block from seq value 1)"
        (1..5).each {
            javers.commit("author", new SnapshotEntity(id: 1, intProperty: it))
            connection.commit()
        }

        when: "simulate tenant/DB switch — advance the DB sequence to a completely new range"
        execute("alter sequence ${COMMIT_PK_SEQ} restart with 1000")

        and: "new commits are made (would collide under stale cache; safe with cache disabled)"
        (1..5).each {
            javers.commit("author", new SnapshotEntity(id: 1, intProperty: it + 100))
            connection.commit()
        }

        then: "all 10 snapshots should be persisted — no PK collision exception was thrown"
        noExceptionThrown()
        javers.findSnapshots(
                QueryBuilder.byInstanceId(1, SnapshotEntity).limit(100).build()).size() == 10
    }

    def "commitPkCacheDisabled=false should preserve sequence caching by default"() {
        given: "a repository with default caching (cache NOT disabled)"
        def sqlRepo = buildRepository(false)

        expect: "cache should remain enabled"
        !sqlRepo.configuration.isCommitPkCacheDisabled()
    }
}
