package org.javers.repository.sql.session;

import org.javers.repository.sql.session.KeyGeneratorDefinition.SequenceDefinition;
import org.polyjdbc.core.exception.SequenceLimitReachedException;

/**
 * Sequence pre-allocation strategy: fetches one DB sequence value per 100 PKs,
 * mapping DB sequence value N to the PK block [N*100 .. N*100+99].
 * PKs within a block are handed out in-memory without hitting the DB.
 * <p>
 * Example:
 * <pre>
 *   DB sequence value  →  PK block issued
 *        1                [100 .. 199]
 *        2                [200 .. 299]
 *        3                [300 .. 399]
 * </pre>
 * On restart, the in-flight block is abandoned — gaps in the PK column are expected and harmless.
 * <p>
 * <b>Known limitations of this strategy:</b>
 * <ul>
 * <li><b>Multi-tenant / multiple databases</b> (see <a href="https://github.com/javers/javers/issues/1447">#1447</a>):
 *     When the same Javers instance serves multiple databases (e.g. via {@code AbstractRoutingDataSource}),
 *     the in-memory block is shared across all of them. Example: DB1 fetches sequence value 4,
 *     allocating block [400..499]. A subsequent write to DB2 (whose sequence is at 100) still
 *     draws PKs 401, 402, … from the cached block — colliding with existing rows in DB2.
 * </li>
 * <li><b>DB backup/restore or sequence reset</b> (see <a href="https://github.com/javers/javers/issues/1378">#1378</a>):
 *     If the DB sequence is reset to a lower value (e.g. after a restore), the in-memory block
 *     still holds the old, higher range. New PKs issued from that stale block conflict with rows
 *     that were written after the restore.
 * </li>
 * </ul>
 *
 * forked from org.polyjdbc.core.key.Sequence
 *
 * @author Adam Dubiel
 */
final class Sequence {
    static final long SEQUENCE_ALLOCATION_SIZE = 100;

    private final String sequenceName;
    private final SequenceDefinition sequenceGenerator;

    private long currentValue;
    private long currentLimit = -1;

    Sequence(String sequenceName, SequenceDefinition sequenceGenerator) {
        this.sequenceName = sequenceName;
        this.sequenceGenerator = sequenceGenerator;
    }

    synchronized long nextValue(Session session) {
        if (recalculationNeeded()) {
            long currentSequenceValue = session.executeQueryForLong(
                    new Select("SELECT next from seq "+ sequenceName,
                            sequenceGenerator.nextFromSequenceAsSelect(sequenceName)));
            recalculate(currentSequenceValue);
        }
        return nextLocalValue();
    }

    long nextLocalValue() {
        if(recalculationNeeded()) {
            throw new SequenceLimitReachedException("Sequence " + sequenceName + " has reached its limit of " + currentLimit + ". "
                    + "Before fetching value, check if recalculation is needed using recalculationNeeded() method.");
        }
        currentValue++;
        return currentValue - 1;
    }

    void recalculate(long currentSequenceValue) {
        currentValue = SEQUENCE_ALLOCATION_SIZE * currentSequenceValue ;
        currentLimit = SEQUENCE_ALLOCATION_SIZE * (currentSequenceValue + 1) - 1;
    }

    boolean recalculationNeeded() {
        return currentValue > currentLimit;
    }
}
