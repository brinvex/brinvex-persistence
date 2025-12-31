/*
 * Copyright © 2023 Brinvex (dev1@brinvex.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.brinvex.persistence.internal;

import com.brinvex.persistence.api.GeneralDao;
import com.brinvex.persistence.api.Filter;
import com.brinvex.persistence.api.QueryCacheMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.LockMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.SpecHints;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("DuplicatedCode")
public class GeneralDaoImpl implements GeneralDao {

    private static final Map<String, Database> PU_2_DATABASE = new ConcurrentHashMap<>();

    @Override
    public <ENTITY, ID extends Serializable> ENTITY getById(EntityManager em, Class<ENTITY> entityType, ID id) {
        return em.find(entityType, id);
    }

    @Override
    public <ENTITY, ID extends Serializable> ENTITY getByIdForUpdate(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            Duration lockTimeout
    ) {
        setTransactionScopedLockTimeout(em, lockTimeout);
        ENTITY entity = em.find(entityType, id, LockModeType.PESSIMISTIC_WRITE);
        setTransactionScopedLockTimeout(em, Duration.ZERO);
        return entity;
    }

    @Override
    public <ENTITY, ID extends Serializable> ENTITY findByIdForUpdateSkipLocked(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            SingularAttribute<? super ENTITY, ID> idAttribute
    ) {
        if (id == null) {
            throw new IllegalArgumentException("Required non-null id");
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ENTITY> q = cb.createQuery(entityType);
        Root<ENTITY> r = q.from(entityType);
        q.where(cb.equal(r.get(idAttribute), id));
        return findFirstResultForUpdateSkipLocked(em, q);
    }

    @Override
    public <ENTITY, ID extends Serializable> List<ENTITY> findByIds(
            EntityManager em,
            Class<ENTITY> entityType,
            Collection<ID> ids,
            SingularAttribute<? super ENTITY, ID> idAttribute
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ENTITY> q = cb.createQuery(entityType);
        Root<ENTITY> r = q.from(entityType);
        q.where(inCollection(cb, r.get(idAttribute), ids));
        return find(em, q);
    }

    @Override
    public <ENTITY, ID extends Serializable> ENTITY getByIdAndCheckVersion(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            short optLockVersion,
            Function<ENTITY, Short> optLockVersionGetter
    ) {
        return getByIdAndCheckVersion(em, entityType, id, (int) optLockVersion, ent -> optLockVersionGetter.apply(ent).intValue());
    }

    @Override
    public <ENTITY, ID extends Serializable> ENTITY getByIdAndCheckVersion(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            int optLockVersion,
            Function<ENTITY, Integer> optLockVersionGetter
    ) {
        ENTITY ent = getById(em, entityType, id);
        if (ent == null) {
            return null;
        }
        if (optLockVersionGetter == null) {
            throw new NullPointerException("optLockVersionGetter must be non-null");
        }
        int currentOptLockVersion = optLockVersionGetter.apply(ent);
        if (currentOptLockVersion == optLockVersion) {
            return ent;
        } else {
            throw new OptimisticLockException(String.format(
                    "Stale state: entityType=%s, id=%s, staleOptLockVersion=%s, currentOptLockVersion=%s",
                    entityType.getName(), id, optLockVersion, currentOptLockVersion));
        }
    }

    @Override
    public <ENTITY, ID, DTO> DTO findByIdAsDTO(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            SingularAttribute<? super ENTITY, ID> idAttribute,
            Class<DTO> dtoType,
            List<SingularAttribute<ENTITY, ?>> constructorParameters
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<DTO> q = cb.createQuery(dtoType);
        Root<ENTITY> r = q.from(entityType);

        q.where(cb.equal(r.get(idAttribute), id));

        Selection<?>[] selections = constructorParameters.stream().map(r::get).toArray(Selection[]::new);
        q.select(cb.construct(dtoType, selections));

        return findFirst(em, q);
    }

    @Override
    public <ENTITY, ID extends Serializable> ENTITY getReference(
            EntityManager em, Class<ENTITY> entityType, ID id
    ) {
        if (id == null) {
            throw new IllegalArgumentException("Required non-null id");
        }
        return em.getReference(entityType, id);
    }

    @Override
    public <ENTITY> void persist(EntityManager em, ENTITY entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Required non-null entity");
        }
        em.persist(entity);
    }

    @Override
    public <ENTITY> ENTITY merge(EntityManager em, ENTITY entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Required non-null entity");
        }
        return em.merge(entity);
    }

    @Override
    public <ENTITY> void detach(EntityManager em, ENTITY entity) {
        em.detach(entity);
    }

    @Override
    public void flush(EntityManager em) {
        em.flush();
    }

    @Override
    public void clear(EntityManager em) {
        em.clear();
    }

    @Override
    public void flushAndClear(EntityManager em) {
        em.flush();
        em.clear();
    }

    @Override
    public <ENTITY> void remove(EntityManager em, ENTITY entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Required non-null entity");
        }
        em.remove(entity);
    }

    @Override
    public <ENTITY, ID extends Serializable> int bulkDeleteByIds(
            EntityManager em,
            Class<ENTITY> entityType,
            SingularAttribute<? super ENTITY, ID> idAttribute,
            Collection<ID> ids
    ) {
        if (ids == null) {
            throw new IllegalArgumentException("Required non-null ids collection");
        }
        if (ids.isEmpty()) {
            return 0;
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<ENTITY> q = cb.createCriteriaDelete(entityType);
        Root<ENTITY> r = q.from(entityType);
        q.where(r.get(idAttribute).in(ids));
        return em.createQuery(q).executeUpdate();
    }

    @Override
    public <F, T> Join<F, T> fetchJoin(From<?, F> from, SingularAttribute<? super F, T> attribute) {
        @SuppressWarnings("unchecked")
        Join<F, T> join = (Join<F, T>) from.fetch(attribute);
        return join;
    }

    @Override
    public <R> List<R> find(EntityManager em, CriteriaQuery<R> query) {
        return em.createQuery(query).getResultList();
    }

    @Override
    public <R> List<R> find(EntityManager em, CriteriaQuery<R> query, Integer offset, Integer limit) {
        TypedQuery<R> typedQuery = em.createQuery(query);
        applyOffsetAndLimit(typedQuery, offset, limit);
        return typedQuery.getResultList();
    }

    @Override
    public <R> List<R> find(EntityManager em, CriteriaQuery<R> query, QueryCacheMode queryCacheMode) {
        TypedQuery<R> typedQuery = em.createQuery(query);
        applyQueryCacheHint(typedQuery, queryCacheMode);
        return typedQuery.getResultList();
    }

    @Override
    public <R> List<R> find(
            EntityManager em,
            CriteriaQuery<R> query,
            Integer offset,
            Integer limit,
            QueryCacheMode queryCacheMode
    ) {
        TypedQuery<R> typedQuery = em.createQuery(query);
        applyQueryCacheHint(typedQuery, queryCacheMode);
        applyOffsetAndLimit(typedQuery, offset, limit);
        return typedQuery.getResultList();
    }

    @Override
    public <R> CriteriaQuery<R> applySelections(
            CriteriaBuilder cb,
            CriteriaQuery<R> q,
            Class<R> resultType,
            Collection<Selection<?>> selections
    ) {
        int selSize = selections.size();
        if (selSize == 0) {
            throw new IllegalArgumentException("Expecting non-empty selections");
        }
        if (selSize == 1) {
            @SuppressWarnings("unchecked")
            Selection<? extends R> typedSelection = (Selection<? extends R>) selections.iterator().next();
            return q.select(typedSelection);
        } else if (resultType.isArray()) {
            @SuppressWarnings("rawtypes")
            CompoundSelection rawSelection = cb.array(selections.toArray(Selection[]::new));
            @SuppressWarnings("unchecked")
            CompoundSelection<R> selection = rawSelection;
            return q.select(selection);
        } else {
            return q.select(cb.construct(resultType, selections.toArray(Selection[]::new)));
        }
    }

    @Override
    public <ENTITY, ID extends Serializable> long count(
            EntityManager em,
            CriteriaQuery<Long> query,
            QueryCacheMode queryCacheMode,
            SingularAttribute<? super ENTITY, ID> idAttribute
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        @SuppressWarnings("unchecked")
        Root<ENTITY> r = (Root<ENTITY>) query.getRoots().iterator().next();
        query.select(cb.count(r.get(idAttribute)));
        TypedQuery<Long> typedQuery = em.createQuery(query);
        applyQueryCacheHint(typedQuery, queryCacheMode);
        return typedQuery.getSingleResult();
    }

    @Override
    public <R> R findFirst(EntityManager em, CriteriaQuery<R> q) {
        return findFirst(em, q, QueryCacheMode.BYPASS_QUERY_CACHE);
    }

    @Override
    public <R> R findFirst(EntityManager em, CriteriaQuery<R> q, Integer offset) {
        TypedQuery<R> typedQuery = em
                .createQuery(q)
                .setFirstResult(offset == null ? 0 : offset)
                .setMaxResults(1);

        List<R> records = typedQuery.getResultList();
        int recordSize = records.size();
        return switch (recordSize) {
            case 0 -> null;
            case 1 -> records.getFirst();
            default -> throw new AssertionError(format("Expecting zero or one record, but found %s", recordSize));
        };
    }

    @Override
    public <R> R findFirst(EntityManager em, CriteriaQuery<R> q, QueryCacheMode queryCacheMode) {
        TypedQuery<R> typedQuery = em
                .createQuery(q)
                .setMaxResults(1);

        applyQueryCacheHint(typedQuery, queryCacheMode);

        List<R> records = typedQuery.getResultList();
        int recordSize = records.size();
        return switch (recordSize) {
            case 0 -> null;
            case 1 -> records.getFirst();
            default -> throw new AssertionError(format("Expecting zero or one record, but found %s", recordSize));
        };
    }

    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    @Override
    public void setTransactionScopedLockTimeout(EntityManager em, Duration timeout) {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Illegal timeout: " + timeout);
        }
        Database database = getDatabase(em);
        if (Database.POSTGRESQL.equals(database)) {
            /*
            Abort any statement that waits longer than the specified amount of time while attempting
            to acquire a lock on a table, index, row, or other database object.
            The effects of SET LOCAL last only till the end of the current transaction
             */
            /*
            We must check whether the largest exact time unit of the given timeout parameter is Second,
            because:
            1. We call: set local lock_timeout = timeoutInMillis
            2. Hibernate later calls: select current_setting('lock_timeout', true)
               and Postgresql returns the value converted into the largest exact human-readable canonical unit
            3. Hibernate validates: assert value.endsWith( "s" );
            4. Hibernate parses: Integer.parseInt( value.substring( 0, value.length() - 1 ) );
            See org.hibernate.dialect.lock.internal.PostgreSQLLockingSupport.getLockTimeout.
            We can remove this restriction when Hibernate/Postgresql will better cooperate here.
            The code from Hibernate 7.2:
                assert value.endsWith( "s" );
				final int secondsValue = Integer.parseInt( value.substring( 0, value.length() - 1 ) );
				return Timeout.seconds( secondsValue );
             */
            if (!timeout.isZero() && !largestExactUnitIsSecond(timeout)) {
                throw new IllegalArgumentException("The largest exact unit must be Seconds: " + timeout);
            }
            long timeoutInMillis = timeout.toMillis();
            em.createNativeQuery("set local lock_timeout = " + timeoutInMillis).executeUpdate();
        } else {
            throw new IllegalStateException("Unsupported database: " + database);
        }
    }

    private static boolean largestExactUnitIsSecond(Duration d) {
        // must be whole seconds
        if (d.getNano() != 0) {
            return false;
        }
        long seconds = Math.abs(d.getSeconds());
        // NOT exactly divisible by 60 → otherwise minutes would be the largest exact unit
        return seconds % 60 != 0;
    }

    /**
     * JPA Pessimistic Locking is not properly supported.
     * 2023-01-11 Postgresql 15.2 + jdbc-driver-postgresql-42.5 + Hibernate 6.2.
     * 2024-05-24 Postgresql 16.3 + jdbc-driver-postgresql-42.7 + Hibernate 6.5.
     * 2024-12-14 Postgresql 17.2 + jdbc-driver-postgresql-42.7 + Hibernate 6.6.
     * 2025-12-02 Postgresql 18.1 + jdbc-driver-postgresql-42.7 + Hibernate 7.1.
     * <p>
     * If the query-scoped timeout hint "jakarta.persistence.lock.timeout" is greater than 0, then it is just ignored,
     * and the default wait_forever=-1 is applied.
     * <p>
     * As a workaround to this problem, we will use transaction scoped lock timeouts.
     * <ul>
     * <li><a href="https://blog.mimacom.com/handling-pessimistic-locking-jpa-oracle-mysql-postgresql-derbi-h2/">blog.mimacom.com/handling-pessimistic-locking-jpa-oracle-mysql-postgresql-derbi-h2</a>
     * <li><a href="https://stackoverflow.com/questions/20963450/controlling-duration-of-postgresql-lock-waits">stackoverflow.com/questions/20963450/controlling-duration-of-postgresql-lock-waits</a>
     * </ul>
     * <p>
     * Here is code from Hibernate for PostgresSQLDialect vs OracleDialect:
     * <pre>
     *    org.hibernate.dialect.PostgreSQLDialect
     *    private String withTimeout(String lockString, int timeout) {
     *        switch (timeout) {
     *            case LockOptions.NO_WAIT:
     *                return supportsNoWait() ? lockString + " nowait" : lockString;
     *            case LockOptions.SKIP_LOCKED:
     *                return supportsSkipLocked() ? lockString + " skip locked" : lockString;
     *            default:
     *                return lockString;
     *        }
     *     }
     *
     *    org.hibernate.dialect.OracleDialect
     *    private String withTimeout(String lockString, int timeout) {
     *        switch ( timeout ) {
     *            case NO_WAIT:
     *                return supportsNoWait() ? lockString + " nowait" : lockString;
     *            case SKIP_LOCKED:
     *                return supportsSkipLocked() ? lockString + " skip locked" : lockString;
     *            case WAIT_FOREVER:
     *                return lockString;
     *            default:
     *                return supportsWait() ? lockString + " wait " + Math.round(timeout / 1e3f) : lockString;
     *       }
     *    }
     * </pre>
     */
    @Override
    public <R> R findFirstForUpdate(
            EntityManager em,
            CriteriaQuery<R> q,
            Duration lockTimeout
    ) {
        requireNonNull(lockTimeout, "Expecting non-null lockTimeout");

        setTransactionScopedLockTimeout(em, lockTimeout);
        TypedQuery<R> typedQuery = em
                .createQuery(q)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, Long.toString(lockTimeout.toMillis()));

        List<R> records = typedQuery.getResultList();
        int recordSize = records.size();
        R result = switch (recordSize) {
            case 0 -> null;
            case 1 -> records.getFirst();
            default -> throw new AssertionError(format("Expecting zero or one record, but found %s", recordSize));
        };
        setTransactionScopedLockTimeout(em, Duration.ZERO);
        return result;
    }

    @Override
    public <R> R findFirstForPessimisticRead(
            EntityManager em,
            CriteriaQuery<R> q,
            Duration lockTimeout
    ) {
        requireNonNull(lockTimeout, "Expecting non-null lockTimeout");

        setTransactionScopedLockTimeout(em, lockTimeout);
        TypedQuery<R> typedQuery = em
                .createQuery(q)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, Long.toString(lockTimeout.toMillis()));

        List<R> records = typedQuery.getResultList();
        int recordSize = records.size();
        R result = switch (recordSize) {
            case 0 -> null;
            case 1 -> records.getFirst();
            default -> throw new AssertionError(format("Expecting zero or one record, but found %s", recordSize));
        };
        setTransactionScopedLockTimeout(em, Duration.ZERO);
        return result;
    }

    @Override
    public <R> R findFirstResultForUpdateSkipLocked(EntityManager em, CriteriaQuery<R> q) {
        TypedQuery<R> typedQuery = em
                .createQuery(q)
                .setMaxResults(1);

        asHibernateQuery(typedQuery).setHibernateLockMode(LockMode.UPGRADE_SKIPLOCKED);

        List<R> records = typedQuery.getResultList();
        int recordSize = records.size();
        return switch (recordSize) {
            case 0 -> null;
            case 1 -> records.getFirst();
            default -> throw new AssertionError(format("Expecting zero or one record, but found %s", recordSize));
        };
    }

    @Override
    public <R> Query<R> asHibernateQuery(TypedQuery<R> typedQuery) {
        return (Query<R>) typedQuery;
    }

    @Override
    public <R> void applyQueryCacheHint(TypedQuery<R> typedQuery, QueryCacheMode queryCacheMode) {
        if (queryCacheMode != null) {
            switch (queryCacheMode) {
                case USE_QUERY_CACHE: {
                    typedQuery.setHint(HibernateHints.HINT_CACHEABLE, "true");
                    break;
                }
                case BYPASS_QUERY_CACHE: {
                    //no-op
                    break;
                }
                default:
                    throw new IllegalStateException("Unsupported value: " + queryCacheMode);
            }
        }
    }

    @Override
    public <NUMBER extends Number> Predicate asPredicate(
            CriteriaBuilder cb,
            Expression<NUMBER> attribute,
            Filter numberFilter
    ) {
        if (numberFilter == null) {
            return cb.conjunction();
        } else if (numberFilter instanceof Filter.BiggerThanNumberFilter(Number number)) {
            return cb.gt(attribute, number);
        } else {
            throw new IllegalStateException("Unsupported filter: " + numberFilter);
        }
    }

    @Override
    public <E> Predicate inCollection(
            CriteriaBuilder cb,
            Expression<E> attribute,
            Collection<E> filterItems
    ) {
        if (filterItems == null) {
            return cb.conjunction();
        } else {
            int size = filterItems.size();
            return switch (size) {
                case 0 -> cb.disjunction();
                case 1 -> cb.equal(attribute, filterItems.iterator().next());
                default -> attribute.in(filterItems);
            };
        }
    }

    @Override
    public Predicate betweenLeftInclRightExcl(
            CriteriaBuilder cb,
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDate testDate
    ) {
        if (testDate == null) {
            return cb.conjunction();
        }
        return cb.and(
                cb.lessThanOrEqualTo(leftAttribute.as(LocalDate.class), testDate),
                cb.greaterThan(rightAttribute.as(LocalDate.class), testDate)
        );
    }

    @Override
    public Predicate betweenLeftInclRightExcl(
            CriteriaBuilder cb,
            Path<LocalDateTime> leftAttribute,
            Path<LocalDateTime> rightAttribute,
            LocalDateTime testDate
    ) {
        if (testDate == null) {
            return cb.conjunction();
        }
        return cb.and(
                cb.lessThanOrEqualTo(leftAttribute, testDate),
                cb.greaterThan(rightAttribute, testDate)
        );
    }

    @Override
    public <T extends Number> Expression<T> sum(
            CriteriaBuilder cb,
            Expression<T> expression1,
            Expression<T> expression2,
            Expression<T> expression3
    ) {
        return cb.sum(expression1, cb.sum(expression2, expression3));
    }

    @Override
    public Expression<Integer> least(CriteriaBuilder cb, Integer literal1, Expression<Integer> expression2) {
        return cb.function("least", Integer.class, cb.literal(literal1), expression2);
    }

    @Override
    public Expression<Integer> greatest(CriteriaBuilder cb, Integer literal1, Expression<Integer> expression2) {
        return cb.function("greatest", Integer.class, cb.literal(literal1), expression2);
    }


    @Override
    public JpaFunction<Integer> day(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression) {
        return hcb(cb).day(datetimeExpression);
    }

    @Override
    public JpaFunction<Integer> month(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression) {
        return hcb(cb).month(datetimeExpression);
    }

    @Override
    public JpaFunction<Integer> year(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression) {
        return hcb(cb).year(datetimeExpression);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public <T extends Temporal> JpaExpression<Duration> durationBetween(
            CriteriaBuilder cb,
            Expression<T> leftDatetimeExpression,
            Expression<T> rightDatetimeExpression
    ) {
        return hcb(cb).durationBetween(leftDatetimeExpression, rightDatetimeExpression);
    }

    @Override
    public HibernateCriteriaBuilder hcb(CriteriaBuilder cb) {
        return (HibernateCriteriaBuilder) cb;
    }

    @Override
    public Database getDatabase(EntityManager em) {
        Map<String, Object> emfProps = em.getEntityManagerFactory().getProperties();
        String puName = (String) emfProps.get(AvailableSettings.PERSISTENCE_UNIT_NAME);
        requireNonNull(puName);
        return PU_2_DATABASE.computeIfAbsent(puName, _ -> detectDatabase(em));
    }

    @Override
    public Database detectDatabase(EntityManager em) {
        Dialect dialect = ((SessionImpl) em.getDelegate()).getJdbcServices().getDialect();
        Class<? extends Dialect> dialectClass = dialect.getClass();
        String dialectSimpleName = dialectClass.getSimpleName();
        if (dialectSimpleName.contains("PostgreSQL")) {
            return Database.POSTGRESQL;
        } else if (dialectSimpleName.contains("SQLServer")) {
            return Database.SQLSERVER;
        } else {
            throw new IllegalStateException("Unsupported dialect: " + dialectClass.getName());
        }
    }

    @Override
    public <R> void applyOffsetAndLimit(TypedQuery<R> typedQuery, Integer offset, Integer limit) {
        if (offset != null) {
            typedQuery.setFirstResult(offset);
        }
        if (limit != null) {
            typedQuery.setMaxResults(limit);
        }
    }


}
