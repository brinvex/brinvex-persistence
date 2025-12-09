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
package test.com.brinvex.persistence.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTest {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected EntityManagerFactory emf;

    @BeforeEach
    void init() {
        emf = Persistence.createEntityManagerFactory("brinvex-persistence-test");
    }

    @AfterEach
    void destroy() {
        emf.close();
    }

    protected <T> T doInTx(TxTemplate<T> txTemplate) {
        T result;
        EntityTransaction tx = null;
        try (EntityManager em = emf.createEntityManager()) {
            txTemplate.beforeTransactionCompletion();
            tx = em.getTransaction();
            tx.begin();
            result = txTemplate.apply(em);
            if (!tx.getRollbackOnly()) {
                tx.commit();
            } else {
                try {
                    tx.rollback();
                } catch (Exception e) {
                    LOG.error("Rollback failure", e);
                }
            }
        } catch (Throwable t) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception e) {
                    LOG.error("Rollback failure", e);
                }
            }
            throw t;
        } finally {
            txTemplate.afterTransactionCompletion();
        }
        return result;
    }

    protected void doInTx(VoidTxTemplate voidTxTemplate) {
        EntityTransaction txn = null;
        try (EntityManager entityManager = emf.createEntityManager()) {
            voidTxTemplate.beforeTransactionCompletion();
            txn = entityManager.getTransaction();
            txn.begin();
            voidTxTemplate.accept(entityManager);
            if (!txn.getRollbackOnly()) {
                txn.commit();
            } else {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOG.error("Rollback failure", e);
                }
            }
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    txn.rollback();
                } catch (Exception e) {
                    LOG.error("Rollback failure", e);
                }
            }
            throw t;
        } finally {
            voidTxTemplate.afterTransactionCompletion();
        }
    }

    protected static void sleep(double seconds) {
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


}
