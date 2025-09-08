package org.javers.repository.sql.session;

import org.javers.repository.sql.SqlRepositoryConfiguration;

interface KeyGeneratorDefinition {

    KeyGenerator createKeyGenerator(SqlRepositoryConfiguration sqlRepositoryConfiguration);

    interface SequenceDefinition extends KeyGeneratorDefinition {
        String nextFromSequenceAsSQLExpression(String seqName);

        default String nextFromSequenceAsSelect(String seqName) {
            return "SELECT " + nextFromSequenceAsSQLExpression(seqName);
        }

        @Override
        default KeyGenerator createKeyGenerator(SqlRepositoryConfiguration sqlRepositoryConfiguration) {
            return new KeyGenerator.SequenceAllocation(this, sqlRepositoryConfiguration);
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
