package com.brinvex.persistence.api;

import com.brinvex.persistence.internal.GeneralDaoImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.dialect.Database;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFunction;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface GeneralDao {

    GeneralDao INSTANCE = new GeneralDaoImpl();

    <ENTITY, ID extends Serializable> ENTITY getById(EntityManager em, Class<ENTITY> entityType, ID id);

    <ENTITY, ID extends Serializable> ENTITY getByIdForUpdate(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            Duration lockTimeout
    );

    <ENTITY, ID extends Serializable> ENTITY getByIdForUpdateSkipLocked(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id
    );

    <ENTITY, ID extends Serializable> List<ENTITY> findByIds(
            EntityManager em,
            Class<ENTITY> entityType,
            Collection<ID> ids,
            SingularAttribute<? super ENTITY, ID> idAttribute
    );

    <ENTITY, ID extends Serializable> ENTITY getByIdAndCheckVersion(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            short optLockVersion,
            Function<ENTITY, Short> optLockVersionGetter
    );

    <ENTITY, ID extends Serializable> ENTITY getByIdAndCheckVersion(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            int optLockVersion,
            Function<ENTITY, Integer> optLockVersionGetter
    );

    <ENTITY, ID, DTO> DTO findByIdAsDTO(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            SingularAttribute<? super ENTITY, ID> idAttribute,
            Class<DTO> dtoType,
            List<SingularAttribute<ENTITY, ?>> constructorParameters
    );

    <ENTITY, ID extends Serializable> ENTITY getReference(
            EntityManager em, Class<ENTITY> entityType, ID id
    );

    <ENTITY> void persist(EntityManager em, ENTITY entity);

    <ENTITY> ENTITY merge(EntityManager em, ENTITY entity);

    <ENTITY> void detach(EntityManager em, ENTITY entity);

    void flush(EntityManager em);

    void clear(EntityManager em);

    void flushAndClear(EntityManager em);

    <ENTITY> void remove(EntityManager em, ENTITY entity);

    <ENTITY, ID extends Serializable> int bulkDeleteByIds(
            EntityManager em,
            Class<ENTITY> entityType,
            SingularAttribute<? super ENTITY, ID> idAttribute,
            Collection<ID> ids
    );

    <F, T> Join<F, T> fetchJoin(From<?, F> from, SingularAttribute<? super F, T> attribute);

    <R> List<R> getResults(EntityManager em, CriteriaQuery<R> query);

    <R> List<R> getResults(EntityManager em, CriteriaQuery<R> query, Integer offset, Integer limit);

    <R> List<R> getResults(EntityManager em, CriteriaQuery<R> query, QueryCacheMode queryCacheMode);

    <R> List<R> getResults(
            EntityManager em,
            CriteriaQuery<R> query,
            Integer offset,
            Integer limit,
            QueryCacheMode queryCacheMode
    );

    <R> CriteriaQuery<R> applySelections(
            CriteriaBuilder cb,
            CriteriaQuery<R> q,
            Class<R> resultType,
            Collection<Selection<?>> selections
    );

    <ENTITY, ID extends Serializable> long count(
            EntityManager em,
            CriteriaQuery<Long> query,
            QueryCacheMode queryCacheMode,
            SingularAttribute<? super ENTITY, ID> idAttribute
    );

    <R> R getUniqueResult(EntityManager em, CriteriaQuery<R> q);

    <R> R getUniqueResult(EntityManager em, CriteriaQuery<R> q, QueryCacheMode queryCacheMode);

    <R> R getFirstResult(EntityManager em, CriteriaQuery<R> q);

    <R> R getFirstResult(EntityManager em, CriteriaQuery<R> q, Integer offset);

    <R> R getFirstResult(EntityManager em, CriteriaQuery<R> q, QueryCacheMode queryCacheMode);

    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    void setTransactionScopedLockTimeout(EntityManager em, Duration timeout);

    <R> R getFirstResultForUpdate(
            EntityManager em,
            CriteriaQuery<R> q,
            Duration lockTimeout
    );

    <R> R getFirstResultForUpdateSkipLocked(EntityManager em, CriteriaQuery<R> q);

    <R> Query<R> asHibernateQuery(TypedQuery<R> typedQuery);

    <R> void applyQueryCacheHint(TypedQuery<R> typedQuery, QueryCacheMode queryCacheMode);

    <NUMBER extends Number> Predicate asPredicate(
            CriteriaBuilder cb,
            Expression<NUMBER> attribute,
            Filter numberFilter
    );

    <E> Predicate inCollection(
            CriteriaBuilder cb,
            Expression<E> attribute,
            Collection<E> filterItems
    );

    Predicate betweenLeftInclRightExcl(
            CriteriaBuilder cb,
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDate testDate
    );

    Predicate betweenLeftInclRightExcl(
            CriteriaBuilder cb,
            Path<LocalDateTime> leftAttribute,
            Path<LocalDateTime> rightAttribute,
            LocalDateTime testDate
    );

    <T extends Number> Expression<T> sum(
            CriteriaBuilder cb,
            Expression<T> expression1,
            Expression<T> expression2,
            Expression<T> expression3
    );

    Expression<Integer> least(CriteriaBuilder cb, Integer literal1, Expression<Integer> expression2);

    Expression<Integer> greatest(CriteriaBuilder cb, Integer literal1, Expression<Integer> expression2);

    JpaFunction<Integer> day(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression);

    JpaFunction<Integer> month(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression);

    JpaFunction<Integer> year(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression);

    <T extends Temporal> JpaExpression<Duration> durationBetween(
            CriteriaBuilder cb,
            Expression<T> leftDatetimeExpression,
            Expression<T> rightDatetimeExpression
    );

    HibernateCriteriaBuilder hcb(CriteriaBuilder cb);

    Database getDatabase(EntityManager em);

    Database detectDatabase(EntityManager em);

    <R> void applyOffsetAndLimit(TypedQuery<R> typedQuery, Integer offset, Integer limit);
}
