package org.javers.repository.sql.schema

import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.javers.repository.sql.H2RepositoryBuilder
import spock.lang.Specification

class SequenceAllocationTest extends Specification {

    def "should allocate sequence values in batches with default settings"() {
        given:
        def repositoryBuilder = new H2RepositoryBuilder()
        def sqlRepository = repositoryBuilder.build()
        def conn = repositoryBuilder.conn
        def tableNames = new TableNameProvider(sqlRepository.configuration)

        def javers = JaversBuilder.javers()
                .registerJaversRepository(sqlRepository)
                .build()

        when:
        javers.commit("author", new SnapshotEntity(id: 1, intProperty: 1))
        javers.commit("author", new SnapshotEntity(id: 2, intProperty: 2))

        then:
        queryPks(conn, tableNames.commitTableNameWithSchema,   "commit_pk")    ==       [100L, 101L]
        queryPks(conn, tableNames.snapshotTableNameWithSchema, "snapshot_pk")  ==       [100L, 200L]
        queryPks(conn, tableNames.globalIdTableNameWithSchema, "global_id_pk") ==       [100L, 101L]

        cleanup:
        conn.close()
    }

    def "should skip sequence allocation if sequenceAllocationEnabled is false"() {
        given:
        def repositoryBuilder = new H2RepositoryBuilder()
        def sqlRepository = repositoryBuilder
                    .withSequenceAllocationEnabled(false)
                    .build()
        def conn = repositoryBuilder.conn
        def tableNames = new TableNameProvider(sqlRepository.configuration)

        def javers = JaversBuilder.javers()
                .registerJaversRepository(sqlRepository)
                .build()

        when:
        javers.commit("author", new SnapshotEntity(id: 1, intProperty: 1))
        javers.commit("author", new SnapshotEntity(id: 2, intProperty: 2))

        then:
        queryPks(conn, tableNames.commitTableNameWithSchema,   "commit_pk")    == [100L, 200L]
        queryPks(conn, tableNames.snapshotTableNameWithSchema, "snapshot_pk")  == [100L, 200L]
        queryPks(conn, tableNames.globalIdTableNameWithSchema, "global_id_pk") == [100L, 200L]


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
