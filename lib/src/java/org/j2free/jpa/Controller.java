/*
 * Controller.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
public final class Controller
{
    private final Log log = LogFactory.getLog(Controller.class);

    private static Log getLog()
    {
        return LogFactory.getLog(Controller.class);
    }

    /**
     *
     */
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
    public static Controller get()
    {
        return get(true);
    }

    /**
     * @param create if true, and there is not already a Controller associated
     *        with this <tt>Thread</tt>, a new Controller will be created. 
     * 
     * @return The <tt>Controller</tt> associated with the current <tt>Thread</tt>
     *         with an active <tt>UserTransaction</tt> open. If there wasn't a
     *         <tt>Controller</tt> with this thread and <tt>create</tt> is true, 
     *         then a new <tt>Controller</tt> will be created and associated with 
     *         the current <tt>Thread</tt>.  If <tt>create</tt> is false, 
     *         <tt>null</tt> will be returned if there was not already a 
     *         <tt>Controller</tt> associated with the current <tt>Thread</tt>.
     *
     *         On the whole, this function guarantees to do one of four things:
     *         If <tt>create == false</tt>:
     *             - return a <tt>Controller</tt> with an open transaction that was 
     *               previously created and associated with the current thread.
     *             - return null, if no code previously called <tt>get(true)</tt>
     *         If <tt>create == true</tt>:
     *             - return a <tt>Controller</tt> with an open transaction that was 
     *               created as a result of this call to <tt>get(true)</tt>
     *             - throw a <tt>RuntimeException</tt>, if a new <tt>Controller</tt>
     *               could not be created, or if there was an exception thrown while
     *               starting a transaction in the new <tt>Controller</tt>
     *
     * @throws RuntimeException if create is <tt>true</tt> and there is an error
     *         creating the controller
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
    public static Controller get(boolean create)
    {
        Controller controller = threadLocal.get();  // Get the controller associated with this Thread
        if (controller != null)
        {
            if (controller.isTransactionOpen())
                return controller;                  // short-circuit, saves a branch
            else
            {
                threadLocal.remove();               // Sanity check, make sure an old controller isn't stuck on the Thread
                controller = null;
            }
        }
                                                    // At this point, controller == null
        if (create)                                 // If the user requested one be created
        {
            try
            {
                controller = new Controller(true);  // Try to create one... (specify true to start the TX)
                threadLocal.set(controller);        // and associate it with the current thread after we know the tx was opened successfully
            }
            catch (Exception e) {
                throw new RuntimeException(e);      // Wrap any problems in a RuntimeException
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
    public static Controller getIsolatedInstance()
    {
        try
        {
            // Only NamingException will be caught here, since the constructor
            // does not throw the other exceptions unless true is specified as
            // the argument.
            return new Controller(false);
        }
        catch (Exception e) {
            throw new RuntimeException("Error creating isolated Controller", e);
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
    public static void release()
    {
        Controller controller = threadLocal.get();     // Get the controller associated with the current-thread
        if (controller != null)                        // If there was one,
        {
            try
            {
                controller.end();                      // end the transaction
            }
            catch (RollbackException re) {
                getLog().debug("transaction marked for rollback");
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
    
    /**
     *
     */
    protected UserTransaction tx;
    /**
     *
     */
    protected EntityManager em;
    /**
     *
     */
    protected FullTextEntityManager fullTextEntityManager;

    /**
     *
     */
    protected Throwable problem;
    /**
     *
     */
    protected InvalidValue[] errors;

    private Controller(boolean beginTX) throws NamingException, NotSupportedException, SystemException
    {
        InitialContext ctx = new InitialContext();
        
        tx = (UserTransaction) ctx.lookup("UserTransaction");
        em = (EntityManager  ) ctx.lookup("java:comp/env/persistence/EntityManager");

        problem = null;

        if (beginTX) begin();
    }

    /**
     * @return The CMT <tt>UserTransaction</tt>
     */
    public UserTransaction getUserTransaction()
    {
        return tx;
    }

    /**
     * @return The underlying <tt>EntityManager</tt>
     */
    public EntityManager getEntityManager()
    {
        return em;
    }

    /**
     * Begins the container managed <tt>UserTransaction</tt> and clears fields used to
     * store problems / errors.
     *
     * @throws NotSupportedException
     * @throws SystemException
     */
    public void begin() throws NotSupportedException, SystemException
    {
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
    public void end() throws SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException
    {
        try
        {
            switch (tx.getStatus())
            {
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
        } 
        catch (InvalidStateException ise)
        {
            problem = ise;
            this.errors = ise.getInvalidValues();
        }
    }

    /**
     * @return true if <tt>begin</tt> has been called, otherwise false
     */
    public boolean isTransactionOpen()
    {
        try
        {
            return tx.getStatus() == Status.STATUS_ACTIVE;
        }
        catch (SystemException ex) {
            return false;
        }
    }

    /**
     * Clears the persistence context.
     * @see {@link EntityManager} clear
     */
    public void clear()
    {
        em.clear();
    }

    /**
     * Flush the persistence context, after which
     * @see {@link EntityManager} flush
     */
    public void flush()
    {
        try
        {
            em.flush();
            problem = null;
        }
        catch (InvalidStateException ise) {
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
     * @return the Hibernate Session
     */
    public Session getSession() {
        return ((EntityManagerImpl) em.getDelegate()).getSession();
    }

    /**
     * 
     * @param mode
     * @throws SystemException
     */
    public void setCacheMode(CacheMode mode) throws SystemException
    {
        if (!isTransactionOpen()) {
            return;
        }

        getSession().setCacheMode(mode);
    }

    /**
     * 
     * @param collectionName
     * @param primaryKey
     */
    public void evictCollection(String collectionName, Serializable primaryKey)
    {
        getSession().getSessionFactory().evictCollection(collectionName, primaryKey);
    }

    /**
     * 
     * @param clazz
     * @param primaryKey
     */
    public void evict(Class clazz, Serializable primaryKey)
    {
        getSession().getSessionFactory().evict(clazz, primaryKey);
    }

    /**
     * 
     * @param collection
     */
    public void evictCollection(String collection)
    {
        getSession().getSessionFactory().evictCollection(collection);
    }

    /**
     * 
     * @param clazz
     */
    public void evict(Class clazz)
    {
        getSession().getSessionFactory().evict(clazz);
    }

    /**
     * 
     * @param query
     * @return
     */
    public Query createQuery(String query)
    {
        return em.createQuery(query);
    }

    /**
     * 
     * @param query
     * @return
     */
    public Query createNativeQuery(String query)
    {
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
     * @param query
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

    /**
     *
     * @param <T>
     * @param entityClass
     * @param namedQuery
     * @param parameters
     * @return
     */
    public <T> int namedCount(Class<T> entityClass, String namedQuery,
                                             KeyValuePair<String, ? extends Object>... parameters) {
        return count(em.createNamedQuery(entityClass.getSimpleName() + "." + namedQuery), parameters);
    }

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param entityId
     * @return
     */
    public <T> T proxy(Class<T> entityClass, Object entityId)
    {
        return (T) em.getReference(entityClass, entityId);
    }

    /**
     * 
     * @param <T>
     * @param entity
     * @return
     */
    public <T> T merge(T entity)
    {
        entity = (T) em.merge(entity);
        return entity;
    }

    /**
     * 
     * @param <T>
     * @param entity
     */
    public <T> void refresh(T entity)
    {
        em.refresh(entity);
    }

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param entityId
     */
    public <T> void remove(Class<T> entityClass, Object entityId)
    {
        T entity = (T) findPrimaryKey(entityClass, entityId);

        if (entity == null)
            throw new IllegalStateException("Error removing " + entityClass.getSimpleName() + " with id = " + entityId + ". Entity not found!");

        em.remove(entity);
    }

    /**
     * 
     * @param <T>
     * @param entity
     */
    public <T> void remove(T entity)
    {

        if (entity == null)
            throw new IllegalStateException("Cannot remove null entity!");

        em.remove(entity);
    }

    /**
     * 
     * @param <T>
     * @param entity
     * @return
     */
    public <T> T persist(T entity)
    {
        return persist(entity, false);
    }

    /**
     * 
     * @param <T>
     * @param entity
     * @param flush
     * @return
     */
    public <T> T persist(T entity, boolean flush)
    {
        try {
            
            em.persist(entity);

            this.errors = null;

            if (flush) {
                em.flush();
            }

        } catch (InvalidStateException ise) {
            this.problem = ise;
            this.errors  = ise.getInvalidValues();

            if (log.isDebugEnabled())
            {
                for (InvalidValue error : ise.getInvalidValues())
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

    /**
     * 
     * @return
     */
    public boolean hasErrors()
    {
        return this.errors != null || this.problem != null;
    }

    /**
     * 
     * @return
     */
    public InvalidValue[] getErrors()
    {
        return this.errors;
    }

    /**
     * 
     */
    public void clearErrors()
    {
        this.errors = null;
    }

    /**
     * 
     * @return
     */
    public Throwable getLastException()
    {
        return problem;
    }
    
    /**
     * 
     * @param delimiter
     * @param debug
     * @return
     */
    public String getErrorsAsString(String delimiter, boolean debug)
    {
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
     *
     * @param <T>
     * @param entityClass
     * @param criteria 
     * @return
     */
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
     *
     * @param <T>
     * @param entityClass
     * @param numResults
     * @param criteria
     * @return
     */
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
     *
     * @param <T>
     * @param entityClass
     * @param firstResult
     * @param criteria
     * @param numResults
     * @return
     */
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
     *
     * @param <T>
     * @param entityClass
     * @param numResults
     * @param firstResult
     * @param criteria
     * @return
     */
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
     *
     * @param <T>
     * @param entityClass
     * @param criteria
     * @return
     */
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
     *
     * @param <T>
     * @param entityClass
     * @param criteria
     * @return
     */
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
     * @param <T> 
     * @param entityClass
     * @param naturalId 
     * @return
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
     *
     * @param <T>
     * @param entityClass
     * @param criteria
     * @return
     */
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

    /**
     * 
     * @param <T>
     * @param exampleInstance
     * @param excludeProperties
     * @return
     */
    public <T> List<T> listByExample(T exampleInstance, String... excludeProperties)
    {
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

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param example
     * @return
     */
    public <T> List<T> listByExample(Class<T> entityClass, Example example)
    {
        try {
            return (List<T>) getSession().createCriteria(entityClass).add(example).list();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * 
     * @param <T>
     * @param formula
     * @return
     */
    public <T> T queryByFormula(QueryFormula formula)
    {
        return (T) query(em.createQuery(formula.getQuery()), formula.getParametersAsPairArray());
    }

    /**
     * 
     * @param <T>
     * @param queryString
     * @param parameters
     * @return
     */
    public <T> T query(String queryString, KeyValuePair<String, ? extends Object>... parameters)
    {
        Query query = em.createQuery(queryString);
        return (T) query(query, parameters);
    }

    /**
     * 
     * @param <T>
     * @param query
     * @param parameters
     * @return
     */
    public <T> T query(Query query, KeyValuePair<String, ? extends Object>... parameters)
    {
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

    /**
     *
     * @param <T>
     * @param entityClass
     * @param queryString
     * @param parameters
     * @return
     */
    public <T> T query(Class<T> entityClass, String queryString,
                                      KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (T) query(em.createQuery(queryString), parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     *
     * @param <T>
     * @param entityClass
     * @param namedQuery
     * @param parameters
     * @return
     */
    public <T> T namedQuery(Class<T> entityClass, String namedQuery,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (T) query(em.createNamedQuery(entityClass.getSimpleName() + "." + namedQuery), parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     *
     * @param <T>
     * @param returnClass
     * @param namedQuery
     * @param parameters
     * @return
     */
    public <T> T namedScaler(Class<T> returnClass, String namedQuery,
                                            KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (T) query(em.createNamedQuery(namedQuery), parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * 
     * @param <T>
     * @param queryString
     * @param parameters
     * @return
     */
    public <T> List<T> list(String queryString, KeyValuePair<String, ? extends Object>... parameters)
    {
        return (List<T>) list(queryString, 0, -1, parameters);
    }

    /**
     *
     * @param <T>
     * @param queryString
     * @param start
     * @param limit
     * @param parameters
     * @return
     */
    public <T> List<T> list(String queryString, int start, int limit,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        Query query = em.createQuery(queryString);
        return (List<T>) list(query, start, limit, parameters);
    }

    /**
     * 
     * @param <T>
     * @param query
     * @param parameters
     * @return
     */
    public <T> List<T> list(Query query, KeyValuePair<String, ? extends Object>... parameters)
    {
        return (List<T>) list(query, 0, -1, parameters);
    }

    /**
     *
     * @param <T>
     * @param query
     * @param start
     * @param limit
     * @param parameters
     * @return
     */
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

    /**
     * 
     * @param namedQuery
     * @param parameters
     * @return
     */
    public List<Object[]> namedList(String namedQuery, KeyValuePair<String, ? extends Object>... parameters)
    {
        return namedList(namedQuery, -1, -1, parameters);
    }

    /**
     *
     * @param namedQuery
     * @param start
     * @param limit
     * @param parameters
     * @return
     */
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

    /**
     * 
     * @param namedQuery
     * @param parameters
     * @return
     */
    public List<Object[]> namedNativeList(String namedQuery, KeyValuePair<String, ? extends Object>... parameters)
    {
        return namedNativeList(namedQuery, -1, -1, parameters);
    }

    /**
     *
     * @param namedQuery
     * @param start
     * @param limit
     * @param parameters
     * @return
     */
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

    /**
     *
     * @param <T>
     * @param entityClass
     * @param queryString
     * @param parameters
     * @return
     */
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

    /**
     *
     * @param <T>
     * @param scalarType
     * @param queryString
     * @param parameters
     * @return
     */
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

    /**
     * 
     * @param queryString
     * @param parameters
     * @return
     */
    public List<Object[]> nativeList(String queryString, KeyValuePair<String, ? extends Object>... parameters)
    {
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

    /**
     * 
     * @param queryString
     * @param parameters
     * @return
     */
    public Object nativeScalar(String queryString, KeyValuePair<String, ? extends Object>... parameters)
    {
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

    /**
     * 
     * @param queryString
     * @param parameters
     * @return
     */
    public int nativeCount(String queryString, KeyValuePair<String, ? extends Object>... parameters)
    {
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

    /**
     *
     * @param <T>
     * @param entityClass
     * @param queryString
     * @param parameters
     * @return
     */
    public <T> List<T> list(Class<T> entityClass, String queryString,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        return list(entityClass, queryString, -1, -1, parameters);
    }

    /**
     *
     * @param <T>
     * @param entityClass
     * @param namedQuery
     * @param parameters
     * @return
     */
    public <T> List<T> namedList(Class<T> entityClass, String namedQuery,
                                                KeyValuePair<String, ? extends Object>... parameters) {
        return namedList(entityClass, namedQuery, -1, -1, parameters);
    }

    /**
     *
     * @param <T>
     * @param entityClass
     * @param queryString
     * @param start
     * @param limit
     * @param parameters
     * @return
     */
    public <T> List<T> list(Class<T> entityClass, String queryString, int start, int limit,
                                           KeyValuePair<String, ? extends Object>... parameters) {
        try {
            return (List<T>) list(em.createQuery(queryString), start, limit, parameters);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     *
     * @param <T>
     * @param entityClass
     * @param namedQuery
     * @param start
     * @param limit
     * @param parameters
     * @return
     */
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
     * @param <T> 
     * @param objects
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
     *
     * @param <T>
     * @param entityClass
     * @param batchSize
     */
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
     *
     * @param <T>
     * @param entityClass
     * @param batchSize
     */
    public <T> void hibernateSearchClearAndIndex(Class<T> entityClass, int batchSize) {
        FullTextSession fullTextSession = org.hibernate.search.Search.getFullTextSession(getSession());
        fullTextSession.purgeAll(entityClass);
        hibernateSearchIndex(entityClass, batchSize);
        fullTextSession.getSearchFactory().optimize(entityClass);
    }

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param entityId
     */
    public <T> void hibernateSearchRemove(Class<T> entityClass, int entityId)
    {
        FullTextSession fullTextSession = org.hibernate.search.Search.getFullTextSession(getSession());
        fullTextSession.purge(entityClass, entityId);
        fullTextSession.flush(); //index are written at commit time
    }

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param query
     * @param fields
     * @return
     * @throws ParseException
     */
    public <T> List<T> hibernateSearchResults(Class<T> entityClass, String query, String[] fields) throws ParseException
    {
        return hibernateSearchResults(entityClass, query, -1, -1, fields);
    }

    /**
     *
     * @param <T>
     * @param entityClass
     * @param query
     * @param limit
     * @param fields
     * @return
     * @throws ParseException
     */
    public <T> List<T> hibernateSearchResults(Class<T> entityClass, String query, int limit,
                                                             String[] fields) throws ParseException {
        return hibernateSearchResults(entityClass, query, -1, limit, fields);
    }

    /**
     *
     * @param <T>
     * @param entityClass
     * @param query
     * @param start
     * @param limit
     * @param fields
     * @return
     * @throws ParseException
     */
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

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param query
     * @param fields
     * @return
     * @throws ParseException
     */
    public <T> int hibernateSearchCount(Class<T> entityClass, String query, String[] fields) throws ParseException
    {
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

    /**
     * 
     * @param <T>
     * @param entityClass
     * @return
     */
    public <T> Criteria createCriteria(Class<T> entityClass)
    {
        return getSession().createCriteria(entityClass);
    }

    /**
     * 
     * @param <T>
     * @param entityClass
     * @param alias
     * @return
     */
    public <T> Criteria createCriteria(Class<T> entityClass, String alias)
    {
        return getSession().createCriteria(entityClass, alias);
    }

    /**
     * 
     * @param queryString
     * @param parameters
     * @return
     */
    public int update(String queryString, KeyValuePair<String, ? extends Object>... parameters)
    {
        Query query = em.createQuery(queryString);
        return update(query, parameters);
    }

    /**
     * 
     * @param queryString
     * @param parameters
     * @return
     */
    public int nativeUpdate(String queryString, KeyValuePair<String, ? extends Object>... parameters)
    {
        Query query = em.createNativeQuery(queryString);
        return update(query, parameters);
    }

    /**
     * 
     * @param query
     * @param parameters
     * @return
     */
    public int update(Query query, KeyValuePair<String, ? extends Object>... parameters)
    {
        if (parameters != null) {
            for (KeyValuePair<String, ? extends Object> parameter : parameters) {
                query.setParameter(parameter.key, parameter.value);
            }
        }
        return query.executeUpdate();
    }

    /**
     * 
     * @param <T>
     * @param collection
     * @param filterString
     * @return
     */
    public <T> Object filterSingle(Collection<T> collection, String filterString)
    {
        List<Object> list = filter(collection, filterString, 0, 1);
        return list.size() > 0 ? list.get(0) : null;
    }

    /**
     * 
     * @param <T>
     * @param collection
     * @param filterString
     * @param params
     * @return
     */
    public <T> Object filterSingle(Collection<T> collection, String filterString, KeyValuePair<String, ? extends Object>... params)
    {
        List<Object> list = filter(collection, filterString, 0, 1, params);
        return list.size() > 0 ? list.get(0) : null;
    }

    /**
     * 
     * @param <T>
     * @param collection
     * @param filterString
     * @return
     */
    public <T> List filter(Collection<T> collection, String filterString)
    {
        return filter(collection, filterString, 0, -1);
    }

    /**
     * 
     * @param <T>
     * @param collection
     * @param filterString
     * @param start
     * @param limit
     * @return
     */
    public <T> List filter(Collection<T> collection, String filterString, int start, int limit)
    {
        org.hibernate.Query query = getSession().createFilter(collection, filterString).setFirstResult(start);
        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return query.list();
    }

    /**
     * 
     * @param <T>
     * @param collection
     * @param filterString
     * @param params
     * @return
     */
    public <T> List filter(Collection<T> collection, String filterString, KeyValuePair<String, ? extends Object>... params)
    {
        return filter(collection, filterString, 0, -1, params);
    }

    /**
     *
     * @param <T>
     * @param collection
     * @param filterString
     * @param start
     * @param limit
     * @param params
     * @return
     */
    public <T> List filter(Collection<T> collection, String filterString, int start, int limit,
                                             KeyValuePair<String, ? extends Object>... params) {
        org.hibernate.Query query = getSession().createFilter(collection, filterString).setFirstResult(start);
        if (limit > 0) {
            query.setMaxResults(limit);
        }

        for (KeyValuePair<String, ? extends Object> param : params) {
            query.setParameter(param.key, param.value);
        }

        return query.list();
    }

    /**
     * 
     * @return
     * @throws SystemException
     */
    public String getTransactionStatus() throws SystemException
    {
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