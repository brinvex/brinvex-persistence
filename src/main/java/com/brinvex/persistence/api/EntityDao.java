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

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface EntityDao<ENTITY, ID extends Serializable> {

    ENTITY getById(ID id);

    ENTITY getByIdForUpdate(ID id, Duration lockDuration);

    ENTITY getByIdForUpdateSkipLocked(ID id);

    ENTITY getByIdAndCheckVersion(ID id, short optLockVersion, Function<ENTITY, Short> optLockVersionGetter);

    ENTITY getByIdAndCheckVersion(ID id, int optLockVersion, Function<ENTITY, Integer> optLockVersionGetter);

    List<ENTITY> findByIds(Collection<ID> ids);

    ENTITY getReference(ID id);

    <OTHER_ENTITY, OTHER_ID extends Serializable> OTHER_ENTITY getReference(
            Class<OTHER_ENTITY> entityType,
            OTHER_ID id
    );

    void persist(ENTITY entity);

    ENTITY merge(ENTITY entity);

    void detach(ENTITY entity);

    void flush();

    void clear();

    void flushAndClear();

    void remove(ENTITY entity);

    int bulkDeleteByIds(Collection<ID> ids);
}
