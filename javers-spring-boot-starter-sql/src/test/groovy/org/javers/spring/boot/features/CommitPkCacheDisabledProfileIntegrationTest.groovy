package org.javers.spring.boot.features;

import org.javers.core.Javers;
import org.javers.spring.boot.TestApplication
import org.javers.spring.boot.DummyEntity
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import spock.lang.Specification;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("commitpk-disabled")
class CommitPkCacheDisabledProfileIntegrationTest extends Specification {

    @Autowired
    Javers javers;

    def "should not use commit id cache when disabled"() {
        given:
        def entity = DummyEntity.random();

        when:
        def commit1 = javers.commit("author", entity);
        def commit2 = javers.commit("author", entity);

        then:
        new BigDecimal(commit2.id.value()) - new BigDecimal(commit1.id.value()) == 1;
    }
}
