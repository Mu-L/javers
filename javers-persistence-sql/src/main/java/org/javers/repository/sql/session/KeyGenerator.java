package org.javers.repository.sql.session;

import org.javers.common.exception.JaversException;
import org.javers.common.exception.JaversExceptionCode;
import org.javers.repository.sql.session.KeyGeneratorDefinition.AutoincrementDefinition;
import org.javers.repository.sql.session.KeyGeneratorDefinition.SequenceDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.javers.repository.sql.session.Sequence.SEQUENCE_ALLOCATION_SIZE;

/**
 * forked from org.polyjdbc.core.key.KeyGenerator
 *
 * @author Adam Dubiel
 */
interface KeyGenerator {

    long generateKey(String sequenceName, Session session);

    long getKeyFromLastInsert(Session session);

    void reset();

    abstract class SequenceBasedGenerator implements KeyGenerator {
        protected final SequenceDefinition sequenceDefinition;

        SequenceBasedGenerator(SequenceDefinition sequenceDefinition) {
            this.sequenceDefinition = sequenceDefinition;
        }

        String nextFromSequenceAsSQLExpression(String seqName) {
            return sequenceDefinition.nextFromSequenceAsSQLExpression(seqName);
        }
    }

    class SequenceDirectCall extends SequenceBasedGenerator {

        SequenceDirectCall(SequenceDefinition sequenceDefinition) {
            super(sequenceDefinition);
        }

        @Override
        public long generateKey(String sequenceName, Session session) {
            return  SEQUENCE_ALLOCATION_SIZE * // multiplying by allocation size is to avoid collisions with SequenceAllocation strategy
                    session.executeQueryForLong(
                    new Select("SELECT next from seq "+ sequenceName,
                            sequenceDefinition.nextFromSequenceAsSelect(sequenceName)));
        }

        @Override
        public long getKeyFromLastInsert(Session session) {
            throw new JaversException(JaversExceptionCode.NOT_IMPLEMENTED);
        }

        @Override
        public void reset() {

        }
    }

    class SequenceAllocation extends SequenceBasedGenerator {
        private final Object lock = new Object();

        private Map<String, Sequence> sequences = new ConcurrentHashMap();

        private ThreadLocal<Long> lastKey = new ThreadLocal<>();

        SequenceAllocation(SequenceDefinition sequenceDefinition) {
            super(sequenceDefinition);
        }

        @Override
        public long generateKey(String sequenceName, Session session) {
            long nextVal = findSequence(sequenceName).nextValue(session);
            lastKey.set(nextVal);
            return nextVal;
        }

        private Sequence findSequence(String sequenceName) {
            if (!sequences.containsKey(sequenceName)) {
                synchronized (lock) {
                    //double check, condition could change while obtaining the lock
                    if (!sequences.containsKey(sequenceName)) {
                        Sequence sequence = new Sequence(sequenceName, sequenceDefinition);
                        sequences.put(sequenceName, sequence);
                    }
                }
            }

            return sequences.get(sequenceName);
        }

        @Override
        public long getKeyFromLastInsert(Session session) {
            return lastKey.get();
        }

        @Override
        public void reset() {
            synchronized (lock) {
                sequences.clear();
            }
        }
    }

    class AutoincrementGenerator implements KeyGenerator {
        private final AutoincrementDefinition autoincrementDefinition;

        AutoincrementGenerator(AutoincrementDefinition autoincrementDefinition) {
            this.autoincrementDefinition = autoincrementDefinition;
        }

        @Override
        public long generateKey(String sequenceName, Session session) {
            throw new RuntimeException("Not implemented. Can't generate key on AutoIncremented");
        }

        @Override
        public long getKeyFromLastInsert(Session session) {
            return session.executeQueryForLong(new Select("last autoincrementDefinition id", autoincrementDefinition.lastInsertedAutoincrement()));
        }

        public void reset() {
        }
    }
}
