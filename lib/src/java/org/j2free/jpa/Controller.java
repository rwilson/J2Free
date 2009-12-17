/*
 * Controller.java
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
package org.j2free.jpa;

import java.io.Serializable;

import java.util.Collection;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.PropertyValueException;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.ejb.EntityManagerImpl;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import org.j2free.util.LaunderThrowable;
import org.j2free.util.KeyValuePair;

/**
 *
 * @author Ryan Wilson
 */
public final class Controller {

    private static final Log log = LogFactory.getLog(Controller.class);

    public static final String ATTRIBUTE_KEY = "controller";

    /*****************************************************
     * ThreadLocal for associating a Controller with each 
     * thread.
     ****************************************************/
    private static final ThreadLocal<Controller> threadLocal = new ThreadLocal<Controller>();

    /**
     * @return The <tt>Controller</tt> associated with the current <tt>Thread</tt>,
     *         or if there wasn't one, a new <tt>Controller</tt> that will
     *         be associated with the current <tt>Thread</tt>. The returned
     *         <tt>Controller</tt> will have an open transaction.
     *
     * Code using <tt>Controller.get()</tt> MUST make sure to call
     * <tt>release()</tt> when finished with the controller to avoid
     * a memory leak.
     *
     * e.g.
     * <pre>
     *      try {
     *          Controller controller = Controller.get();
     *          // ... do some business ... or play ...
     *      } finally {
     *          Controller.release();
     *      }
     * </pre>
     */
    public static Controller get() {
        return get(true);
    }

    /**
     * @param create if true, and there is not already a Controller associated
     *        with this <tt>Thread</tt>, a new Controller will be created. 
     *        <tt>null</tt> will never be returned if create == <tt>true</tt>
     * 
     * @return The <tt>Controller</tt> associated with the current <tt>Thread</tt>.
     *         If there wasn't one, and <tt>create</tt> is true, then a new
     *         <tt>Controller</tt> will be created and associated with the current
     *         <tt>Thread</tt>.  If <tt>create</tt> is false, null will be returned
     *         if there was not already a <tt>Controller</tt> associated with this
     *         <tt>Thread</tt>.
     *
     * @throws RuntimeException if there is an error creating the controller
     *
     * Exceptions normally thrown by beginning a UserTransaction are laundered
     * from checked exceptions to unchecked RuntimeExceptions so that code calling
     * Controller.get() does not ALWAYS have to catch the exception.  However,
     * code using <tt>Controller.get()</tt> MUST make sure to call
     * <tt>release()</tt> when finished with the controller to avoid
     * a memory leak.
     *
     * e.g.
     * <pre>
     *      try {
     *          Controller controller = Controller.get();
     *          // ... do some business ... or play ...
     *      } finally {
     *          Controller.release();
     *      }
     * </pre>
     */
    public static Controller get(boolean create) {

        Controller controller = threadLocal.get();      // Get the controller associated with this Thread

        if (controller == null && create) {             // If there wasn't one and the user requested one be created
            // don't set the ThreadLocal until after we know the tx was opened successfully
            try {
                controller = new Controller();          // Try to create one...
                controller.begin();                     // and start the transaction...
                threadLocal.set(controller);            // and associate it with the current thread
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return controller;
    }

    /**
     * @return a new <tt>Controller</tt> instance that is not associated with
     *         any Thread.  Callers MUST call <tt>end()</tt> on the controller. 
     *         This controller does not have a transaction open; it's up to
     *         the caller to manage the transaction.
     *
     * @throws RuntimeException if there is an error creating the controller
     */
    public static Controller getIsolatedInstance() {
        try {
            return new Controller();
        } catch (NamingException ne) {
            throw new RuntimeException("Error creating isolated Controller", ne);
        }
    }

    /**
     * Releases a Controller associated with the current thread, if there was one.
     * 
     * <tt>release()</tt> will internally call <tt>end()</tt> on the instance
     * associated with the current thread, if it was found.
     *
     * @throws RuntimeException if there is an exception ending the UserTransaction
     */
    public static void release() {
        
        Controller controller = threadLocal.get();     // Get the controller associated with the current-thread
        if (controller != null) {                      // If there was one,
            try {
                controller.end();                      // end the transaction
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                threadLocal.remove();                  // and ALWAYS disassociate it with the current-thread
            }
        }
    }


    //------------------------------------------------------------//
    // Instance implementation
    //------------------------------------------------------------//
    
    protected UserTransaction tx;
    protected EntityManager em;
    protected FullTextEntityManager fullTextEntityManager;

    protected Throwable problem;
    protected InvalidValue[] errors;

    private Controller() throws NamingException {

        InitialContext ctx = new InitialContext();
        
        tx = (UserTransaction) ctx.lookup("UserTransaction");
        em = (EntityManager) ctx.lookup("java:comp/env/persistence/EntityManager");

        problem = null;
    }

    /**
     * @return The CMT <tt>UserTransaction</tt>
     */
    public UserTransaction getUserTransaction() {
        return tx;
    }

    /**
     * @return The underlying <tt>EntityManager</tt>
     */
    public EntityManager getEntityManager() {
        return em;
    }

    /**
     * Clears the persistence context.
     * @see {@link EntityManager} clear
     */
    public void clear() {
        em.clear();
    }

    /**
     * Flush the persistence context, after which
     * @see {@link EntityManager} flush
     */
    public void flush() {
        try {
            em.flush();
            problem = null;
        } catch (InvalidStateException ise) {
            this.errors = ise.getInvalidValues();
        }
    }

    /**
     * Equivalent to:
     * <pre>
     *      controller.flush();
     *      controller.clear();
     * </pre>
     */
    public void flushAndClear() {
        try {
            em.flush();
            em.clear();
            problem = null;
        } catch (InvalidStateException ise) {
            this.errors = ise.getInvalidValues();
        }
    }

    /**
     * Marks the current transaction to be rolled back.
     */
    public void markForRollback() {
        try {
            tx.setRollbackOnly();
        } catch (Exception e) {
            throw LaunderThrowable.launderThrowable(e);
        }
    }

    /**
     * @return true if the current transaction has been
     *         marked for rollback, otherwise false
     */
    public boolean isMarkedForRollback() {
        try {
            return tx.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (Exception e) {
            throw LaunderThrowable.launderThrowable(e);
        }
    }

    /**
     * @return a <tt>FullTextEntityManager</tt> for Hibernate Search
     */
    public FullTextEntityManager getFullTextEntityManager() {

        if (fullTextEntityManager == null || !fullTextEntityManager.isOpen()) {
            fullTextEntityManager = Search.getFullTextEntityManager(getEntityManager());
        }
        return fullTextEntityManager;
    }

    /**
     * Begins the container managed <tt>UserTransaction</tt> and clears fields used to
     * store problems / errors.
     *
     * @throws NotSupportedException
     * @throws SystemException
     */
    public void begin() throws NotSupportedException, SystemException {
        
        // Make sure a transaction isn't already in progress
        if (tx.getStatus() == Status.STATUS_ACTIVE)
            return;

        // Start the transaction
        tx.begin();

        // make sure the entity manager knows the transaction has begun
        em.joinTransaction();

        // make sure that a transaction always starts clean
        problem = null;
        errors  = null;
    }

    /**
     * Ends the contatiner managed <tt>UserTransaction</tt>, committing
     * or rolling back as necessary.
     *
     * @throws SystemException
     * @throws RollbackException
     * @throws HeuristicMixedException
     * @throws HeuristicRollbackException
     */
    public void end() throws SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        try {

            switch (tx.getStatus()) {
                case Status.STATUS_MARKED_ROLLBACK:
                    tx.rollback();
                    break;
                case Status.STATUS_ACTIVE:
                    tx.commit();
                    break;
                case Status.STATUS_COMMITTED:
                    // somebody called end() twice
                    break;
                case Status.STATUS_COMMITTING:
                    log.warn("uh oh, concurrency problem! end() called when transaction already committing");
                    break;
                case Status.STATUS_ROLLEDBACK:
                    // somebody called end() twice
                    break;
                case Status.STATUS_ROLLING_BACK:
                    log.warn("uh oh, concurrency problem! end() called when transaction already rolling back");
                    break;
                default:
                    throw new IllegalStateException("Unknown status in endTransaction: " + getTransactionStatus());
            }

            problem = null;
            errors  = null;
            
        } catch (InvalidStateException ise) {
            problem = ise;
            this.errors = ise.getInvalidValues();
        }
    }

    /**
     * @return true if <tt>begin</tt> has been called, otherwise false
     */
    public boolean isTransactionOpen() {
        try {
            return tx.getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException ex) {
            return false;
        }
    }

    /**
     * @return the Hibernate Session
     */
    public Session getSession() {
        return ((EntityManagerImpl) em.getDelegate()).getSession();
    }

    public void setCacheMode(CacheMode mode) throws SystemException {
        if (!isTransactionOpen()) {
            return;
        }

        getSession().setCacheMode(mode);
    }

    public void evictCollection(String collectionName, Serializable primaryKey) {
        getSession().getSessionFactory().evictCollection(collectionName, primaryKey);
    }

    public void evict(Class clazz, Serializable primaryKey) {
        getSession().getSessionFactory().evict(clazz, primaryKey);
    }

    public void evictCollection(String collection) {
        getSession().getSessionFactory().evictCollection(collection);
    }

    public void evict(Class clazz) {
        getSession().getSessionFactory().evict(clazz);
    }

    public Query createQuery(String query) {
        return em.createQuery(query);
    }

    public Query createNativeQuery(String query) {
        return em.createNativeQuery(query);
    }

    /**
     * @param <T> The type of entity to fetch
     * @param entityClass The class of the entity to fetch
     * @param entityId The id of the entity to fetch
     * @return The entity, or null if it was not found
     */
    public <T> T findPrimaryKey(Class<T> entityClass, Object entityId) {
        return (T) em.find(entityClass, entityId);
    }

    /**
     * Equivalent to:
     * <pre>
     *      list(entityClass, -1, -1);
     * </pre>
     *
     * @param <T> The type of entity to fetch
     * @param entityClass The class of the entity to fetch
     * @return a <tt>List</tt> of entities found
     */
    public <T> List<T> list(Class<T> entityClass) {
        return list(entityClass, -1, -1);
    }

    /**
     * @param <T> The type of entity to fetch
     * @param entityClass The class of the entity to fetch
     * @param start The first entity to fetch
     * @param limit How many entities to fetch
     * @return a <tt>List</tt> of entities found
     */
    public <T> List<T> list(Class<T> entityClass, int start, int limit) {
        Query query = em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e");

        if (start > 0) {
            query.setFirstResult(start);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return (List<T>) query.getResultList();
    }

    /**
     * @param <T> The type of entity to fetch
     * @param entityClass The class of the entity to count
     * @return The number of entities of the specified type
     */
    public <T> int count(Class<T> entityClass) {

        Object o = null;

        try {
            Query query = em.createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e");

            o = query.getSingleResult();

            return o == null ? -1 : ((Long) o).intValue();

        } catch (ClassCastException cce) {
            return o == null ? -1 : ((java.math.BigInteger) o).intValue();
        }
    }

    /**
     * @param <T> The type of entity to fetch
     * @param entityClass The class of the entity to count
     * @param parameters the {@link KeyValuePair}s for the variables referenced in the query
     * @return The number of entities found
     */
    public <T> int count(Query query, KeyValuePair<String, ? extends Object>... parameters) {
        int count = -1;
        Object o = null;
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        try {
            o = query.getSingleResult();
            count = ((Long) o).intValue();
        } catch (NoResultException nre) {
            count = 0;
        } catch (ClassCastException cce) {
            count = ((java.math.BigInteger) o).intValue();
        }
        return count;
    }

    /**
     * @param queryString A JPQL/HQL query string
     * @param parameters the {@link KeyValuePair}s for the variables referenced in the query
     * @return The number of entities found
     */
    public int count(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        return count(em.createQuery(queryString), parameters);
    }

    public <T> int namedCount(Class<T> entityClass, String namedQuery,
                                             KeyValuePair<String, ? extends Object>... parameters) {
        return count(em.createNamedQuery(entityClass.getSimpleName() + "." + namedQuery), parameters);
    }

    public <T> T proxy(Class<T> entityClass, Object entityId) {
        return (T) em.getReference(entityClass, entityId);
    }

    public <T> T merge(T entity) {
        entity = (T) em.merge(entity);
        return entity;
    }

    public <T> void refresh(T entity) {
        em.refresh(entity);
    }

    public <T> void remove(Class<T> entityClass, Object entityId) {
        T entity = (T) findPrimaryKey(entityClass, entityId);

        if (entity == null)
            throw new IllegalStateException("Error removing " + entityClass.getSimpleName() + " with id = " + entityId + ". Entity not found!");

        em.remove(entity);
    }

    public <T> void remove(T entity) {

        if (entity == null)
            throw new IllegalStateException("Cannot remove null entity!");

        em.remove(entity);
    }

    public <T> T persist(T entity) {
        return persist(entity, false);
    }

    public <T> T persist(T entity, boolean flush) {
        try {
            
            em.persist(entity);

            this.errors = null;

            if (flush) {
                em.flush();
            }

        } catch (InvalidStateException ise) {
            this.problem = ise;
            this.errors  = ise.getInvalidValues();

            for (InvalidValue error : ise.getInvalidValues()) {
                log.warn("Invalid Value: " + error.getBeanClass() + "." + error.getPropertyName() + " = " + error.getValue() + " | " + error.getMessage());
            }

            markForRollback();

        } catch (ConstraintViolationException cve) {
            this.problem = cve;
            markForRollback();
        } catch (PropertyValueException pve) {
            this.problem = pve;
            markForRollback();
        }
        return entity;
    }

    public boolean hasErrors() {
        return this.errors != null || this.problem != null;
    }

    public InvalidValue[] getErrors() {
        return this.errors;
    }

    public void clearErrors() {
        this.errors = null;
    }

    public Throwable getLastException() {
        return problem;
    }
    
    public String getErrorsAsString(String delimiter, boolean debug) {
        StringBuilder errorStringBuilder = new StringBuilder();
        boolean first = true;
        for (InvalidValue error : this.errors) {
            if (debug) {
                errorStringBuilder.append((first ? "" : delimiter) + "Invalid Value: " + error.getBeanClass() + "." + error.
                        getPropertyName() + " = " + error.getValue() + ", message = " + error.getMessage());
            } else {
                errorStringBuilder.append((first ? "" : delimiter) + error.getMessage());
            }

            if (first) {
                first = false;
            }
        }
        return errorStringBuilder.toString();
    }

    /**
     * Find all objects matching criteria
     * 
     * Sample criteria include:
     * Restrictions.eq("property", variable)
     * Restrictions.eq("subclass.property", variable)
     * Order.desc("property")
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/v3/api/org/hibernate/criterion/Restrictions.html
     * 
     **/
    public <T> List<T> listByCriterions(Class<T> entityClass, Object... criteria) {
        return listByCriterions(entityClass, -1, -1, criteria);
    }

    /**
     * Find numResults objects matching criteria
     * 
     * Sample criteria include:
     * Restrictions.eq("property", variable)
     * Restrictions.eq("subclass.property", variable)
     * Order.desc("property")
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/v3/api/org/hibernate/criterion/Restrictions.html
     * 
     **/
    public <T> List<T> listByCriterions(Class<T> entityClass, int numResults, Object... criteria) {
        return listByCriterions(entityClass, 0, numResults, criteria);
    }

    /**
     * Find numResults objects matching criteria starting at firstResult
     * 
     * Sample criteria include:
     * Restrictions.eq("property", variable)
     * Restrictions.eq("subclass.property", variable)
     * Order.desc("property")
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/v3/api/org/hibernate/criterion/Restrictions.html
     * 
     **/
    public <T> List<T> listByCriterions(Class<T> entityClass, int firstResult, int numResults,
                                                       Object... criteria) {
        try {
            Criteria search = getSession().createCriteria(entityClass);
            for (Object c : criteria) {
                if (c instanceof Criterion || c.getClass() == Criterion.class) {
                    search.add((Criterion) c);
                }
                if (c instanceof Order || c.getClass() == Order.class) {
                    search.addOrder((Order) c);
                }
            }
            if (firstResult > 0) {
                search.setFirstResult(firstResult);
            }
            if (numResults > 0) {
                search.setMaxResults(numResults);
            }
            return (List<T>) search.list();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find numResults objects matching criteria starting at firstResult
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/v3/api/org/hibernate/criterion/Restrictions.html
     * 
     **/
    public <T> List<T> listByCriterea(Class<T> entityClass, int firstResult, int numResults,
                                                     Criteria criteria) {
        if (firstResult > 0) {
            criteria.setFirstResult(firstResult);
        }
        if (numResults > 0) {
            criteria.setMaxResults(numResults);
        }
        return listByCriteria(entityClass, criteria);
    }

    /**
     * Find numResults objects matching criteria starting at firstResult
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/v3/api/org/hibernate/criterion/Restrictions.html
     * 
     **/
    public <T> List<T> listByCriteria(Class<T> entityClass, Criteria criteria) {
        try {
            return (List<T>) criteria.list();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find a single object matching criteria
     * 
     * Sample criteria include:
     * Restrictions.eq("property", variable)
     * Restrictions.eq("subclass.property", variable)
     * Order.desc("property")
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/v3/api/org/hibernate/criterion/Restrictions.html
     * 
     **/
    public <T> T findByCriterion(Class<T> entityClass, Object... criteria) {
        try {
            Criteria search = getSession().createCriteria(entityClass);
            for (Object c : criteria) {
                if (c instanceof Criterion || c.getClass() == Criterion.class) {
                    search.add((Criterion) c);
                }
                if (c instanceof Order || c.getClass() == Order.class) {
                    search.addOrder((Order) c);
                }
            }
            return (T) search.setMaxResults(1).uniqueResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find a single object matching criteria using a naturalId lookup with the queryByFormula cache
     * 
     * Sample criteria include:
     * Restrictions.naturalId().set("email",request.getRemoteUser());
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/reference/en/html/querycriteria.html#queryByFormula-criteria-naturalid
     */
    public <T> T findNaturalId(Class<T> entityClass, NaturalIdentifier naturalId) {
        try {
            return (T) (getSession().createCriteria(entityClass).add(naturalId).setCacheable(true).uniqueResult());
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find the count of object matching the criteria
     * 
     * Sample criteria include:
     * Restrictions.eq("property", variable)
     * Restrictions.eq("subclass.property", variable)
     * Order.desc("property")
     * 
     * See for more examples:
     * http://www.hibernate.org/hib_docs/v3/api/org/hibernate/criterion/Restrictions.html
     * 
     **/
    public <T> int count(Class<T> entityClass, Object... criteria) {
        try {
            Criteria search = getSession().createCriteria(entityClass);
            for (Object c : criteria) {
                if (c instanceof Criterion || c.getClass() == Criterion.class) {
                    search.add((Criterion) c);
                }
            }
            search.setProjection(Projections.rowCount());
            return ((Integer) search.list().get(0)).intValue();
        } catch (NoResultException e) {
            return 0;
        }
    }

    public <T> List<T> listByExample(T exampleInstance, String... excludeProperties) {
        try {
            Example example = Example.create(exampleInstance);
            for (String prop : excludeProperties) {
                example.excludeProperty(prop);
            }

            return (List<T>) getSession().createCriteria(exampleInstance.getClass()).add(example).list();
        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> List<T> listByExample(Class<T> entityClass, Example example) {
        try {
            return (List<T>) getSession().createCriteria(entityClass).add(example).list();
        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> T queryByFormula(QueryFormula formula) {
        return (T) query(em.createQuery(formula.getQuery()), formula.getParametersAsPairArray());
    }

    public <T> T query(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createQuery(queryString);
        return (T) query(query, parameters);
    }

    public <T> T query(Query query, KeyValuePair<String, ? extends Object>... parameters) {
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        try {
            return (T) query.setMaxResults(1).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public <T> T query(Class<T> entityClass, String queryString,
                                      KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (T) query(em.createQuery(queryString), parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> T namedQuery(Class<T> entityClass, String namedQuery,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (T) query(em.createNamedQuery(entityClass.getSimpleName() + "." + namedQuery), parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> T namedScaler(Class<T> returnClass, String namedQuery,
                                            KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (T) query(em.createNamedQuery(namedQuery), parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> List<T> list(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        return (List<T>) list(queryString, 0, -1, parameters);
    }

    public <T> List<T> list(String queryString, int start, int limit,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createQuery(queryString);
        return (List<T>) list(query, start, limit, parameters);
    }

    public <T> List<T> list(Query query, KeyValuePair<String, ? extends Object>... parameters) {
        return (List<T>) list(query, 0, -1, parameters);
    }

    public <T> List<T> list(Query query, int start, int limit,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        if (start > 0) {
            query.setFirstResult(start);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        try {
            return (List<T>) query.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public List<Object[]> namedList(String namedQuery, KeyValuePair<String, ? extends Object>... parameters) {
        return namedList(namedQuery, -1, -1, parameters);
    }

    public List<Object[]> namedList(String namedQuery, int start, int limit,
                                    KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createNamedQuery(namedQuery);
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        if (start > 0) {
            query.setFirstResult(start);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        try {
            return (List<Object[]>) query.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public List<Object[]> namedNativeList(String namedQuery, KeyValuePair<String, ? extends Object>... parameters) {
        return namedNativeList(namedQuery, -1, -1, parameters);
    }

    public List<Object[]> namedNativeList(String namedQuery, int start, int limit,
                                          KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createNativeQuery(namedQuery);
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        if (start > 0) {
            query.setFirstResult(start);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        try {
            return (List<Object[]>) query.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public <T> List<T> nativeList(Class<T> entityClass, String queryString,
                                                 KeyValuePair<String, ? extends Object>... parameters) {

        Session session = getSession();

        SQLQuery query = session.createSQLQuery(queryString).addEntity(entityClass);

        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }

        try {
            return (List<T>) query.list();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public <T> List<T> nativeScalerList(Class<T> scalarType, String queryString,
                                                       KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createNativeQuery(queryString);
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        try {
            return (List<T>) query.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public List<Object[]> nativeList(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createNativeQuery(queryString);
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        try {
            return (List<Object[]>) query.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public Object nativeScalar(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createNativeQuery(queryString);
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        try {
            return query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public int nativeCount(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createNativeQuery(queryString);

        int count = 0;
        Object o = null;
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        
        try {
            o = query.getSingleResult();
            count = ((Long) o).intValue();
        } catch (NoResultException nre) {
            count = 0;
        } catch (ClassCastException cce) {
            count = ((java.math.BigInteger) o).intValue();
        }
        
        return count;
    }

    public <T> List<T> list(Class<T> entityClass, String queryString,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        return list(entityClass, queryString, -1, -1, parameters);
    }

    public <T> List<T> namedList(Class<T> entityClass, String namedQuery,
                                                KeyValuePair<String, ? extends Object>... parameters) {
        return namedList(entityClass, namedQuery, -1, -1, parameters);
    }

    public <T> List<T> list(Class<T> entityClass, String queryString, int start, int limit,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (List<T>) list(em.createQuery(queryString), start, limit, parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> List<T> namedList(Class<T> entityClass, String namedQuery, int start, int limit,
                                                KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (List<T>) list(em.createNamedQuery(entityClass.getSimpleName() + "." + namedQuery), start, limit,
                                  parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * @deprecated
     * will cause memory problems when mapping large numbers of entities, use the alternate version with batch sizes
     **/
    public <T> void hibernateSearchIndex(List<T> objects) {
        this.getFullTextEntityManager();
        for (T o : objects) {
            fullTextEntityManager.index(o);
        }
    }

    /**
     * It is critical that batchSize matches the hibernate.search.worker.batch_size you set
     **/
    public <T> void hibernateSearchIndex(Class<T> entityClass, int batchSize) {
        FullTextSession fullTextSession = org.hibernate.search.Search.getFullTextSession(getSession());
        fullTextSession.setFlushMode(FlushMode.MANUAL);
        fullTextSession.setCacheMode(CacheMode.IGNORE);

        ScrollableResults results = fullTextSession.createCriteria(entityClass).setFetchSize(batchSize).scroll(
                ScrollMode.FORWARD_ONLY);

        try {
            int index = 0;
            while (results.next()) {
                index++;
                fullTextSession.index(results.get(0)); //index each element

                //clear every batchSize since the queue is processed
                if (index % batchSize == 0) {
                    fullTextSession.flushToIndexes();
                    fullTextSession.clear();
                }
            }
        } finally {
            results.close();
        }
    }

    /**
     * It is critical that batchSize matches the hibernate.search.worker.batch_size you set
     **/
    public <T> void hibernateSearchClearAndIndex(Class<T> entityClass, int batchSize) {
        FullTextSession fullTextSession = org.hibernate.search.Search.getFullTextSession(getSession());
        fullTextSession.purgeAll(entityClass);
        hibernateSearchIndex(entityClass, batchSize);
        fullTextSession.getSearchFactory().optimize(entityClass);
    }

    public <T> void hibernateSearchRemove(Class<T> entityClass, int entityId) {
        FullTextSession fullTextSession = org.hibernate.search.Search.getFullTextSession(getSession());
        fullTextSession.purge(entityClass, entityId);
        fullTextSession.flush(); //index are written at commit time
    }

    public <T> List<T> hibernateSearchResults(Class<T> entityClass, String query, String[] fields) throws ParseException {
        return hibernateSearchResults(entityClass, query, -1, -1, fields);
    }

    public <T> List<T> hibernateSearchResults(Class<T> entityClass, String query, int limit,
                                                             String[] fields) throws ParseException {
        return hibernateSearchResults(entityClass, query, -1, limit, fields);
    }

    public <T> List<T> hibernateSearchResults(Class<T> entityClass, String query, int start, int limit,
                                                             String[] fields) throws ParseException {
        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new StandardAnalyzer());

            //org.apache.lucene.search.Query luceneQuery = parser.parse(query.trim().replaceAll(" ","~ ").replaceAll(" [Aa][Nn][Dd]~ "," AND ").replaceAll(" [Oo][Rr]~ "," OR ") + "~");
            //org.apache.lucene.search.Query luceneQuery = parser.parse(query.trim());
            org.apache.lucene.search.Query luceneQuery;

            query = filterLuceneQuery(query);
            luceneQuery = parser.parse(query);

            org.hibernate.search.jpa.FullTextQuery hibQuery = getFullTextEntityManager().createFullTextQuery(luceneQuery,
                                                                                                             entityClass);

            if (start >= 0 && limit > 0) {
                return hibQuery.setFirstResult(start).setMaxResults(limit).getResultList();
            } else if (limit > 0) {
                return hibQuery.setMaxResults(limit).getResultList();
            } else if (start >= 0) {
                return hibQuery.setFirstResult(start).getResultList();
            } else {
                return hibQuery.getResultList();
            }

        } catch (NoResultException e) {
            return null;
        }
    }

    public <T> int hibernateSearchCount(Class<T> entityClass, String query, String[] fields) throws ParseException {
        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new StandardAnalyzer());

            //org.apache.lucene.search.Query luceneQuery = parser.parse(query.trim().replaceAll(" ","~ ").replaceAll(" [Aa][Nn][Dd]~ "," AND ").replaceAll(" [Oo][Rr]~ "," OR ") + "~");
            //org.apache.lucene.search.Query luceneQuery = parser.parse(query.trim());
            org.apache.lucene.search.Query luceneQuery;

            query = filterLuceneQuery(query);
            luceneQuery = parser.parse(query);

            org.hibernate.search.jpa.FullTextQuery hibQuery = getFullTextEntityManager().createFullTextQuery(luceneQuery,
                                                                                                             entityClass);
            return hibQuery.getResultSize();
            
        } catch (NoResultException e) {
            return 0;
        }
    }

    private String filterLuceneQuery(String queryOrig) {

        String query = queryOrig;

        // Make words wildcard for partial matches
        //query = query.trim().replaceAll("\\s+", "* ");
        // Make last word wildcard for partial matches
        //query += "*";

        // Escape all the special chars that shouldnt be next to a *
        /*
        query = query.replaceAll(" [Aa][Nn][Dd]\\* ", " AND ").replaceAll(" [Oo][Rr]\\* ", " OR ").replaceAll("[-]\\*","-").
                replaceAll("[)]\\*", ")").replaceAll("[(]\\*", "(").replaceAll("[!]\\*", "!").replaceAll("[?]\\*", "?").
                replaceAll("[:]\\*", ":").replaceAll("[+]\\*", "+");
        */

        query = query.replaceAll("[-]", "\\\\-").replaceAll("[)]", "\\\\)").replaceAll("[(]", "\\\\(").replaceAll("[!]",
                                                                                                                  "\\\\!").
                replaceAll("[?]", "\\\\?").replaceAll("[:]", "\\\\:").replaceAll("[+]", "\\\\+");

        return query;
    }

    public <T> Criteria createCriteria(Class<T> entityClass) {
        return getSession().createCriteria(entityClass);
    }

    public <T> Criteria createCriteria(Class<T> entityClass, String alias) {
        return getSession().createCriteria(entityClass, alias);
    }

    public int update(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createQuery(queryString);
        return update(query, parameters);
    }

    public int nativeUpdate(String queryString, KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createNativeQuery(queryString);
        return update(query, parameters);
    }

    public int update(Query query, KeyValuePair<String, ? extends Object>... parameters) {
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        return query.executeUpdate();
    }

    public <T> T filterSingle(Collection<T> collection, String filterString) {
        List<T> list = filter(collection, filterString, 0, 1);
        return list.size() > 0 ? list.get(0) : null;
    }

    public <T> T filterSingle(Collection<T> collection, String filterString, KeyValuePair<String, ? extends Object>... params) {
        List<T> list = filter(collection, filterString, 0, 1, params);
        return list.size() > 0 ? list.get(0) : null;
    }

    public <T> List<T> filter(Collection<T> collection, String filterString) {
        return filter(collection, filterString, 0, -1);
    }

    public <T> List<T> filter(Collection<T> collection, String filterString, int start, int limit) {
        org.hibernate.Query query = getSession().createFilter(collection, filterString).setFirstResult(start);
        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return (List<T>) query.list();
    }

    public <T> List<T> filter(Collection<T> collection, String filterString, KeyValuePair<String, ? extends Object>... params) {
        return filter(collection, filterString, 0, -1, params);
    }

    public <T> List<T> filter(Collection<T> collection, String filterString, int start, int limit,
                                             KeyValuePair<String, ? extends Object>... params) {
        org.hibernate.Query query = getSession().createFilter(collection, filterString).setFirstResult(start);
        if (limit > 0) {
            query.setMaxResults(limit);
        }

        for (KeyValuePair<String, ? extends Object> param : params) {
            query.setParameter(param.key, param.value);
        }

        return (List<T>) query.list();
    }

    public String getTransactionStatus() throws SystemException {
        if (tx == null) {
            return "Null transaction";
        }
        
        switch (tx.getStatus()) {
            case Status.STATUS_ACTIVE:
                return "Active";
            case Status.STATUS_COMMITTED:
                return "Committed";
            case Status.STATUS_COMMITTING:
                return "Committing";
            case Status.STATUS_MARKED_ROLLBACK:
                return "Marked for rollback";
            case Status.STATUS_NO_TRANSACTION:
                return "No Transaction";
            case Status.STATUS_PREPARED:
                return "Prepared";
            case Status.STATUS_ROLLEDBACK:
                return "Rolledback";
            case Status.STATUS_ROLLING_BACK:
                return "Rolling back";
            case Status.STATUS_UNKNOWN:
                return "Declared Unknown";
            default:
                return "Undeclared Unknown Status";
        }
    }
}