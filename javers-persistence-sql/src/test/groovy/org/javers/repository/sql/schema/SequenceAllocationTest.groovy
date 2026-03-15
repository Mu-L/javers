package org.javers.repository.sql.schema

import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.repository.sql.H2RepositoryBuilder
import spock.lang.Specification

class SequenceAllocationTest extends Specification {

    def "should allocate sequence values in batches with default settings"() {
        given:
        def repositoryBuilder = new H2RepositoryBuilder()
        def sqlRepository = repositoryBuilder.build("SequenceAllocationTest")
        def conn = repositoryBuilder.conn
        def tableNames = new TableNameProvider(sqlRepository.configuration)

        def javers = JaversBuilder.javers()
                .registerJaversRepository(sqlRepository)
                .build()

        when:
        javers.commit("author", new SnapshotEntity(id: 1, intProperty: 1))
        javers.commit("author", new SnapshotEntity(id: 2, intProperty: 2))

        then:
        def commitPks   = queryPks(conn, tableNames.commitTableNameWithSchema,   "commit_pk")
        def snapshotPks = queryPks(conn, tableNames.snapshotTableNameWithSchema, "snapshot_pk")
        def globalIdPks = queryPks(conn, tableNames.globalIdTableNameWithSchema, "global_id_pk")

        commitPks[1]   - commitPks[0]   == 1L    // same pre-allocated batch
        snapshotPks[1] - snapshotPks[0] == 100L  // snapshot always direct-calls sequence
        globalIdPks[1] - globalIdPks[0] == 1L    // same pre-allocated batch

        cleanup:
        conn.close()
    }

    def "should skip sequence allocation if sequenceAllocationEnabled is false"() {
        given:
        def repositoryBuilder = new H2RepositoryBuilder()
        def sqlRepository = repositoryBuilder
                    .withSequenceAllocationEnabled(false)
                    .build("SequenceAllocationTest")
        def conn = repositoryBuilder.conn
        def tableNames = new TableNameProvider(sqlRepository.configuration)

        def javers = JaversBuilder.javers()
                .registerJaversRepository(sqlRepository)
                .build()

        when:
        javers.commit("author", new SnapshotEntity(id: 1, intProperty: 1))
        javers.commit("author", new SnapshotEntity(id: 2, intProperty: 2))

        then:
        def commitPks   = queryPks(conn, tableNames.commitTableNameWithSchema,   "commit_pk")
        def snapshotPks = queryPks(conn, tableNames.snapshotTableNameWithSchema, "snapshot_pk")
        def globalIdPks = queryPks(conn, tableNames.globalIdTableNameWithSchema, "global_id_pk")

        commitPks[1]   - commitPks[0]   == 100L  // each insert fetches fresh NEXTVAL × 100
        snapshotPks[1] - snapshotPks[0] == 100L  // same as always
        globalIdPks[1] - globalIdPks[0] == 100L  // each insert fetches fresh NEXTVAL × 100


        cleanup:
        conn.close()
    }

    List<Long> queryPks(conn, String table, String pkCol) {
        def rs = conn.createStatement().executeQuery("SELECT $pkCol FROM $table ORDER BY $pkCol")
        def pks = []
        while (rs.next()) pks << rs.getLong(1)
        pks
    }
}
