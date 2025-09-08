package org.javers.core.commit

import org.javers.core.CoreConfiguration
import spock.lang.Specification

/**
 * @author bartosz walacik
 */
class CommitSeqGeneratorTest extends Specification {

    def "should return 1.0 when first commit"() {
        given:
        def config = Stub(CoreConfiguration)
        
        when:
        def gen1 = new CommitSeqGenerator(config).nextId(null)

        then:
        gen1.value() == "1.00"
    }

    def "should inc minor and assign 0 to minor when seq calls"() {
        given:
        def head = new CommitId(1,5)
        def config = Stub(CoreConfiguration)
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
        def config = Stub(CoreConfiguration)
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
        def config = Stub(CoreConfiguration)
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
        given:
        def config = Stub(CoreConfiguration)
        config.isCommitPkCacheDisabled() >> true
        def commitSeqGenerator = new CommitSeqGenerator(config)
        def head = new CommitId(1,5)

        when:
        def gen1 = commitSeqGenerator.nextId(head)
        def gen2 = commitSeqGenerator.nextId(head)
        def gen3 = commitSeqGenerator.nextId(gen2)

        then:
        gen1 == new CommitId(2,0)
        gen2 == new CommitId(2,0)
        gen3 == new CommitId(3,0)
    }
}
