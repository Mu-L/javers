package org.javers.spring.auditable.aspect.springdatajpa;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.javers.core.Javers;
import org.javers.spring.auditable.AdvancedCommitPropertiesProvider;
import org.javers.spring.auditable.AuthorProvider;
import org.javers.spring.auditable.CommitPropertiesProvider;
import org.javers.spring.auditable.aspect.springdata.AbstractSpringAuditableRepositoryAspect;
import org.springframework.core.annotation.Order;

/**
 * Commits all arguments passed to save(), delete() and saveAndFlush() methods
 * in Spring Data JpaRepository
 * when repositories are annotated with (class-level) @JaversSpringDataAuditable.
 */
@Aspect
@Order(0)
public class JaversSpringDataJpaAuditableRepositoryAspect extends AbstractSpringAuditableRepositoryAspect {
    public JaversSpringDataJpaAuditableRepositoryAspect(Javers javers, AuthorProvider authorProvider, CommitPropertiesProvider commitPropertiesProvider, AdvancedCommitPropertiesProvider advancedCommitPropertiesProvider) {
        super(javers, authorProvider, commitPropertiesProvider, advancedCommitPropertiesProvider);
    }

    /**
     * for backward compatibility after introducing AdvancedCommitPropertiesProvider
     */
    public JaversSpringDataJpaAuditableRepositoryAspect(Javers javers, AuthorProvider authorProvider, CommitPropertiesProvider commitPropertiesProvider) {
        this(javers, authorProvider, commitPropertiesProvider, AdvancedCommitPropertiesProvider.empty());
    }

    @AfterReturning("execution(public * delete(..)) && this(org.springframework.data.repository.CrudRepository)")
    public void onDeleteExecuted(JoinPoint pjp) {
        onDelete(pjp);
    }

    @AfterReturning("execution(public * deleteById(..)) && this(org.springframework.data.repository.CrudRepository)")
    public void onDeleteByIdExecuted(JoinPoint pjp) {
        onDelete(pjp);
    }

    @AfterReturning("execution(public * deleteAll(..)) && this(org.springframework.data.repository.CrudRepository)")
    public void onDeleteAllExecuted(JoinPoint pjp) {
        onDelete(pjp);
    }

    @AfterReturning(value = "execution(public * save(..)) && this(org.springframework.data.repository.CrudRepository)", returning = "responseEntity")
    public void onSaveExecuted(JoinPoint pjp, Object responseEntity) {
        onSave(pjp, responseEntity);
    }

    @AfterReturning(value = "execution(public * saveAll(..)) && this(org.springframework.data.repository.CrudRepository)", returning = "responseEntity")
    public void onSaveAllExecuted(JoinPoint pjp, Object responseEntity) {
        onSave(pjp, responseEntity);
    }

    @AfterReturning(value = "execution(public * saveAndFlush(..)) && this(org.springframework.data.jpa.repository.JpaRepository)", returning = "responseEntity")
    public void onSaveAndFlushExecuted(JoinPoint pjp, Object responseEntity) {
        onSave(pjp, responseEntity);
    }

    @AfterReturning("execution(public * deleteInBatch(..)) && this(org.springframework.data.jpa.repository.JpaRepository)")
    public void onDeleteInBatchExecuted(JoinPoint pjp) {
        onDelete(pjp);
    }
}
