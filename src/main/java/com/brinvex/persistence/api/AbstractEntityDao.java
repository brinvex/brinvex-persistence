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
package com.brinvex.persistence.api;

import com.brinvex.persistence.internal.GeneralDaoImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings({"unused", "resource"})
public abstract class AbstractEntityDao<ENTITY, ID extends Serializable> implements EntityDao<ENTITY, ID> {

    protected final Class<ENTITY> entityType;

    protected final Class<ID> idType;

    private EntityManager em;

    private CriteriaBuilder cb;

    private final GeneralDao generalDao;

    private SingularAttribute<? super ENTITY, ID> idAttribute;

    protected AbstractEntityDao(
            Class<ENTITY> entityType,
            Class<ID> idType
    ) {
        this(entityType, idType, GeneralDaoImpl.INSTANCE);
    }

    protected AbstractEntityDao(
            Class<ENTITY> entityType,
            Class<ID> idType,
            GeneralDao generalDao
    ) {
        this.entityType = entityType;
        this.idType = idType;
        this.generalDao = GeneralDaoImpl.INSTANCE;
    }

    protected abstract EntityManager entityManager();

    protected EntityManager em() {
        if (this.em == null) {
            this.em = entityManager();
            Objects.requireNonNull(this.em);
        }
        return this.em;
    }

    protected CriteriaBuilder cb() {
        if (this.cb == null) {
            this.cb = em().getCriteriaBuilder();
        }
        return this.cb;
    }

    protected SingularAttribute<? super ENTITY, ID> idAttribute() {
        if (idAttribute == null) {
            EntityType<ENTITY> entityMetamodel = em().getEntityManagerFactory().getMetamodel().entity(entityType);
            idAttribute = entityMetamodel.getId(idType);
        }
        return idAttribute;
    }

    @Override
    public ENTITY getById(ID id) {
        return generalDao.getById(em(), entityType, id);
    }

    @Override
    public ENTITY getByIdForUpdate(ID id, Duration lockDuration) {
        return generalDao.getByIdForUpdate(em(), entityType, id, lockDuration);
    }

    @Override
    public ENTITY getByIdAndCheckVersion(ID id, short optLockVersion, Function<ENTITY, Short> optLockVersionGetter) {
        return generalDao.getByIdAndCheckVersion(em(), entityType, id, optLockVersion, optLockVersionGetter);
    }

    @Override
    public ENTITY getByIdAndCheckVersion(ID id, int optLockVersion, Function<ENTITY, Integer> optLockVersionGetter) {
        return generalDao.getByIdAndCheckVersion(em(), entityType, id, optLockVersion, optLockVersionGetter);
    }

    @Override
    public List<ENTITY> findByIds(Collection<ID> ids) {
        return generalDao.findByIds(em(), entityType, ids, idAttribute());
    }

    @Override
    public ENTITY findByIdForUpdateSkipLocked(ID id) {
        return generalDao.findByIdForUpdateSkipLocked(em(), entityType, id, idAttribute());
    }

    @Override
    public ENTITY getReference(ID id) {
        return generalDao.getReference(em(), entityType, id);
    }

    @Override
    public <OTHER_ENTITY, OTHER_ID extends Serializable> OTHER_ENTITY getReference(
            Class<OTHER_ENTITY> entityType,
            OTHER_ID id
    ) {
        return generalDao.getReference(em(), entityType, id);
    }

    @Override
    public void persist(ENTITY entity) {
        generalDao.persist(em(), entity);
    }

    @Override
    public ENTITY merge(ENTITY entity) {
        return generalDao.merge(em(), entity);
    }

    @Override
    public void detach(ENTITY entity) {
        generalDao.detach(em(), entity);
    }

    @Override
    public void flush() {
        generalDao.flush(em());
    }

    @Override
    public void clear() {
        generalDao.clear(em());
    }

    @Override
    public void flushAndClear() {
        generalDao.flushAndClear(em());
    }

    @Override
    public void remove(ENTITY entity) {
        generalDao.remove(em(), entity);
    }

    @Override
    public int bulkDeleteByIds(Collection<ID> ids) {
        return generalDao.bulkDeleteByIds(em(), entityType, idAttribute(), ids);
    }

    protected <F, T> Join<F, T> fetchJoin(From<?, F> from, SingularAttribute<? super F, T> attribute) {
        return generalDao.fetchJoin(from, attribute);
    }

    protected <R> List<R> find(CriteriaQuery<R> query) {
        return generalDao.find(em(), query);
    }

    protected <R> List<R> find(CriteriaQuery<R> query, Integer offset, Integer limit) {
        return generalDao.find(em(), query, offset, limit);
    }

    protected <R> List<R> find(CriteriaQuery<R> query, QueryCacheMode queryCacheUsage) {
        return generalDao.find(em(), query, queryCacheUsage);
    }

    protected <R> List<R> find(
            CriteriaQuery<R> query,
            Integer offset,
            Integer limit,
            QueryCacheMode queryCacheUsage
    ) {
        return generalDao.find(em(), query, offset, limit, queryCacheUsage);
    }

    protected long count(CriteriaQuery<Long> query, QueryCacheMode queryCacheUsage) {
        return generalDao.count(em(), query, queryCacheUsage, idAttribute());
    }

    protected long count(CriteriaQuery<Long> query) {
        return generalDao.count(em(), query, QueryCacheMode.BYPASS_QUERY_CACHE, idAttribute());
    }

    protected <R> R findFirst(CriteriaQuery<R> query) {
        return generalDao.findFirst(em(), query);
    }

    protected <R> R findFirst(CriteriaQuery<R> query, Integer offset) {
        return generalDao.findFirst(em(), query, offset);
    }

    protected <R> R findFirst(CriteriaQuery<R> query, QueryCacheMode queryCachemode) {
        return generalDao.findFirst(em(), query, queryCachemode);
    }

    protected <R> R findFirstForUpdate(CriteriaQuery<R> query, Duration lockTimeout) {
        return generalDao.findFirstForUpdate(em(), query, lockTimeout);
    }

    protected <R> R findFirstForPessimisticRead(CriteriaQuery<R> query, Duration lockTimeout) {
        return generalDao.findFirstForPessimisticRead(em(), query, lockTimeout);
    }

    protected <R> R findFirstForUpdateSkipLocked(CriteriaQuery<R> query) {
        return generalDao.findFirstResultForUpdateSkipLocked(em(), query);
    }

    protected <NUMBER extends Number> Predicate asPredicate(Expression<NUMBER> attribute, Filter numberFilter) {
        return generalDao.asPredicate(cb(), attribute, numberFilter);
    }

    protected <E> Predicate inCollection(Expression<E> attribute, Collection<E> filterItems) {
        return generalDao.inCollection(cb(), attribute, filterItems);
    }

    protected Predicate betweenLeftInclRightExcl(
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDate testDate
    ) {
        return generalDao.betweenLeftInclRightExcl(cb(), leftAttribute, rightAttribute, testDate);
    }

    protected Predicate betweenLeftInclRightExcl(
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDateTime testDate
    ) {
        return generalDao.betweenLeftInclRightExcl(cb(), leftAttribute, rightAttribute, testDate);
    }

    protected <T extends Number> Expression<T> sum(
            Expression<T> expression1,
            Expression<T> expression2,
            Expression<T> expression3
    ) {
        return generalDao.sum(cb(), expression1, expression2, expression3);
    }

    protected Expression<Integer> least(Integer literal1, Expression<Integer> expression2) {
        return generalDao.least(cb(), literal1, expression2);
    }

    protected Expression<Integer> greatest(Integer literal1, Expression<Integer> expression2) {
        return generalDao.greatest(cb(), literal1, expression2);
    }

    protected Expression<Integer> day(Expression<? extends TemporalAccessor> datetimeExpression) {
        return generalDao.day(cb(), datetimeExpression);
    }

    protected Expression<Integer> month(Expression<? extends TemporalAccessor> datetimeExpression) {
        return generalDao.month(cb(), datetimeExpression);
    }

    protected Expression<Integer> year(Expression<? extends TemporalAccessor> datetimeExpression) {
        return generalDao.year(cb(), datetimeExpression);
    }

    protected <T extends Temporal> Expression<Duration> durationBetween(
            Expression<T> leftDatetimeExpr,
            Expression<T> rightDatetimeExpr
    ) {
        return generalDao.durationBetween(cb(), leftDatetimeExpr, rightDatetimeExpr);
    }

    protected <DTO> DTO findByIdAsDTO(
            Class<DTO> dtoType,
            ID id,
            List<SingularAttribute<ENTITY, ?>> constructorParameters
    ) {
        return generalDao.findByIdAsDTO(em(), entityType, id, idAttribute(), dtoType, constructorParameters);
    }

    protected <R> CriteriaQuery<R> applySelections(
            CriteriaQuery<R> q,
            Class<R> resultType,
            Collection<Selection<?>> selections
    ) {
        return generalDao.applySelections(cb(), q, resultType, selections);
    }

}
