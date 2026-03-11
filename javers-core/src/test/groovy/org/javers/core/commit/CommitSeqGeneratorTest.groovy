package org.javers.core.commit

import org.javers.core.CoreConfigurationBuilder
import spock.lang.Specification

/**
 * @author bartosz walacik
 */
class CommitSeqGeneratorTest extends Specification {

    def config = CoreConfigurationBuilder.coreConfiguration().build()
    def configCacheDisabled = CoreConfigurationBuilder.coreConfiguration().withCommitPkCacheDisabled(true).build()

    def "should return 1.0 when first commit"() {
        when:
        def gen1 = new CommitSeqGenerator(config).nextId(null)

        then:
        gen1.value() == "1.00"
    }

    def "should inc minor and assign 0 to minor when seq calls"() {
        given:
        def head = new CommitId(1,5)
        def commitSeqGenerator = new CommitSeqGenerator(config)

        when:
        def gen1 = commitSeqGenerator.nextId(head)

        then:
        gen1.value() == "2.00"

        when:
        def gen2 = commitSeqGenerator.nextId(gen1)

        then:
        gen2.value() == "3.00"
    }

    def "should inc minor when the same head"() {
        given:
        def commitSeqGenerator = new CommitSeqGenerator(config)
        def commit1 = commitSeqGenerator.nextId(null)     //1.0
        def commit2 = commitSeqGenerator.nextId(commit1)  //2.0

        expect:
        commitSeqGenerator.nextId(commit1)  == new CommitId(2,1)
        commitSeqGenerator.nextId(commit2)  == new CommitId(3,0)
        commitSeqGenerator.nextId(commit1)  == new CommitId(2,2)
        commitSeqGenerator.nextId(commit2)  == new CommitId(3,1)
    }

    def "should provide chronological ordering for commitIds"() {
        given:
        def commitSeqGenerator = new CommitSeqGenerator(config)
        def head = commitSeqGenerator.nextId(null)

        when:
        def commits = []
        15.times {
            commits << commitSeqGenerator.nextId(head)
        }

        commits.each {
            println it.valueAsNumber()
        }

        then:
        14.times {
            assert commits[it].isBeforeOrEqual(commits[it])
            assert commits[it].isBeforeOrEqual(commits[it + 1])
        }
    }

    def "should not use cache when commitPkCache is disabled"() {
        given: "a generator with cache DISABLED and a starting head at (1,5)"
        def commitSeqGenerator = new CommitSeqGenerator(configCacheDisabled)
        def head = new CommitId(1, 5)

        when: "nextId is called twice with the exact same head"
        def gen1 = commitSeqGenerator.nextId(head)
        def gen2 = commitSeqGenerator.nextId(head)

        then: "both calls return the same result — purely derived from head.majorId+1 with no state"
        gen1 == new CommitId(2, 0)
        gen2 == new CommitId(2, 0)  // not (2,1) — minorId stays at 0, cache has no effect

        when: "nextId is advanced to a new head"
        def gen3 = commitSeqGenerator.nextId(gen2)

        then: "result is again stateless — purely head.majorId+1, minor always 0"
        gen3 == new CommitId(3, 0)

        and: "going back to the original head still yields the same result - (2, 0)"
        commitSeqGenerator.nextId(head) == new CommitId(2, 0)
    }

    def "cache-enabled generator accumulates state for the same head"() {
        given: "a generator with cache ENABLED"
        def commitSeqGenerator = new CommitSeqGenerator(config)
        def head = new CommitId(1, 5)

        when: "nextId is called twice with the exact same head"
        def gen1 = commitSeqGenerator.nextId(head)
        def gen2 = commitSeqGenerator.nextId(head)

        then: "minorId increments — the cache tracks previously handed-out ids for this major"
        gen1 == new CommitId(2, 0)
        gen2 == new CommitId(2, 1)  // minor incremented because (2,0) was already handed out
    }
}
