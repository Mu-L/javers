package org.javers.repository.sql.session;

import org.javers.repository.sql.SqlRepositoryConfiguration;

import static org.javers.repository.sql.session.Sequence.SEQUENCE_ALLOCATION_SIZE;

interface KeyGeneratorDefinition {

    KeyGenerator createKeyGenerator(SqlRepositoryConfiguration sqlRepositoryConfiguration);

    interface SequenceDefinition extends KeyGeneratorDefinition {
        String nextFromSequenceAsSQLExpression(String seqName);

        default String nextFromSequenceAsSelect(String seqName) {
            return "SELECT " + nextFromSequenceAsSQLExpression(seqName);
        }

        @Override
        default KeyGenerator createKeyGenerator(SqlRepositoryConfiguration sqlRepositoryConfiguration) {
            if (!sqlRepositoryConfiguration.isSequenceAllocationEnabled()){
                return new KeyGenerator.SequenceDirectCall(this);
            }
            return new KeyGenerator.SequenceAllocation(this);
        }
    }

    interface AutoincrementDefinition extends KeyGeneratorDefinition {
        String lastInsertedAutoincrement();

        @Override
        default KeyGenerator createKeyGenerator(SqlRepositoryConfiguration sqlRepositoryConfiguration) {
            return new KeyGenerator.AutoincrementGenerator(this);
        }
    }
}
