package org.javers.spring.boot.features

import org.javers.core.Javers
import org.javers.spring.boot.TestApplication
import org.javers.spring.boot.DummyEntity
import org.javers.spring.boot.sql.JaversSqlProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Integration test for https://github.com/javers/javers/issues/1447
 *
 * Verifies that sqlCommitPkCacheDisabled is correctly wired from Spring Boot properties.
 * The "test" profile sets sqlCommitPkCacheDisabled=true, so we verify that the
 * configured flag is respected and commits still work correctly when cache is disabled.
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class CommitPkCacheDisabledIntegrationTest extends Specification {

    @Autowired
    Javers javers

    @Autowired
    JaversSqlProperties javersSqlProperties

    def "sqlCommitPkCacheDisabled property is correctly wired from application-test.yml"() {
        expect: "the test profile enables this flag"
        javersSqlProperties.sqlCommitPkCacheDisabled
    }

    def "commits should succeed with commit_pk cache disabled"() {
        given:
        def entity = DummyEntity.random()

        when:
        def commit1 = javers.commit("author", entity)
        def commit2 = javers.commit("author", entity)

        then: "both commits are persisted successfully and have different ids"
        commit1 != null
        commit2 != null
        commit1.id != commit2.id
    }
}
