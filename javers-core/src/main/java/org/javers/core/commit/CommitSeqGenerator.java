package org.javers.core.commit;

/**
 * Generates unique and monotonically increasing commit identifiers. <br>
 * Thread safe. Should not be used in distributed applications.
 *
 * @see DistributedCommitSeqGenerator
 * @author bartosz walacik
 */
import org.javers.core.CoreConfiguration;

class CommitSeqGenerator {
    private final CoreConfiguration javersCoreConfiguration;
    private HandedOutIds handedOut = new HandedOutIds();

    CommitSeqGenerator(CoreConfiguration javersCoreConfiguration) {
        this.javersCoreConfiguration = javersCoreConfiguration;
    }

    synchronized CommitId nextId(CommitId head)
    {
        if (javersCoreConfiguration.isCommitPkCacheDisabled()) {
            return new CommitId(getHeadMajorId(head) + 1, 0);
        }

        Long major = getHeadMajorId(head) + 1;

        CommitId lastReturned = handedOut.get(major);

        CommitId result;
        if (lastReturned == null){
            result = new CommitId(major,0);
        }
        else {
            result = new CommitId(major, lastReturned.getMinorId() + 1);
        }

        handedOut.put(result);
        return result;
    }

    long getHeadMajorId(CommitId head){
        if (head == null){
            return 0;
        }
        return head.getMajorId();
    }
}

