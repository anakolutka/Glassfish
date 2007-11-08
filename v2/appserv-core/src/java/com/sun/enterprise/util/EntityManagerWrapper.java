/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.enterprise.util;

import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.admin.monitor.callflow.EntityManagerMethod;
import java.io.Serializable;
import java.util.logging.*;
import java.util.Map;

import javax.persistence.*;

import com.sun.enterprise.Switch;
import com.sun.enterprise.J2EETransactionManager;
import com.sun.enterprise.distributedtx.J2EETransaction;
import com.sun.ejb.ContainerFactory;
import com.sun.enterprise.deployment.types.EntityManagerReference;

import com.sun.logging.*;

/**
 * Implementation of a container-managed entity manager.  
 * A new instance of this class will be created for each injected
 * EntityManager reference or each lookup of an EntityManager
 * reference within the component jndi environment. 
 * The underlying EntityManager object does not support concurrent access.
 * Likewise, this wrapper does not support concurrent access.  
 *
 * @author Kenneth Saks
 */
public class EntityManagerWrapper implements EntityManager, Serializable {

    static Logger _logger=LogDomains.getLogger(LogDomains.UTIL_LOGGER);

    static private LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(EntityManagerWrapper.class);

    // Serializable state

    private String unitName;
    private PersistenceContextType contextType;
    private Map emProperties;

    // transient state

    transient private EntityManagerFactory entityManagerFactory;
    transient private J2EETransactionManager txManager;
    transient private ContainerFactory containerFactory;
    
    // Only used to cache entity manager with EXTENDED persistence context
    transient private EntityManager extendedEntityManager;

    // set and cleared after each non-tx, non EXTENDED call to _getDelegate()
    transient private EntityManager nonTxEntityManager;
    
    public EntityManagerWrapper(EntityManagerReference 
                                referenceDescriptor) {
        this.unitName = referenceDescriptor.getUnitName();
        this.contextType = referenceDescriptor.getPersistenceContextType();
        this.emProperties = referenceDescriptor.getProperties();
    }

    private void init() {

        entityManagerFactory = EntityManagerFactoryWrapper.
            lookupEntityManagerFactory(unitName);
        
        if( entityManagerFactory == null ) {
            throw new IllegalStateException
                ("Unable to retrieve EntityManagerFactory for unitName "
                 + unitName);
        }
        
        txManager = Switch.getSwitch().getTransactionManager();
        containerFactory = Switch.getSwitch().getContainerFactory();

    }

    private void doTransactionScopedTxCheck() {
        
        if( contextType != PersistenceContextType.TRANSACTION) {
            return;
        }
        
        doTxRequiredCheck();

    }

    private void doTxRequiredCheck() {

        if( entityManagerFactory == null ) {
            init();
        }

        J2EETransaction tx = null;
        try {
            tx = (J2EETransaction) txManager.getTransaction();
        } catch(Exception e) {
            throw new IllegalStateException("exception retrieving tx", e);
        }
            
        if( tx == null ) {
            throw new TransactionRequiredException();
        }

    }
    private EntityManager _getDelegate() {

        // Populate any transient objects the first time 
        // this method is called.

        if( entityManagerFactory == null ) {
            init();
        }

        EntityManager delegate = null;

        if( nonTxEntityManager != null ) {
            cleanupNonTxEntityManager();
        }

        if( contextType == PersistenceContextType.TRANSACTION ) {

            J2EETransaction tx = null;
            try {
                tx = (J2EETransaction) txManager.getTransaction();
            } catch(Exception e) {
                throw new IllegalStateException("exception retrieving tx", e);
            }
            
            if( tx != null ) {

                // If there is an active extended persistence context
                // for the same entity manager factory and the same tx,
                // it takes precendence.
                delegate = tx.getExtendedEntityManager(entityManagerFactory);

                if( delegate == null ) {

                    delegate = tx.getTxEntityManager(entityManagerFactory);

                    if( delegate == null ) {

                        // If there is a transaction and this is the first
                        // access of the wrapped entity manager, create an
                        // actual entity manager and associate it with the
                        // entity manager factory.
                        delegate = entityManagerFactory.
                            createEntityManager(emProperties);

                        tx.addTxEntityManagerMapping(entityManagerFactory, 
                                                     delegate);
                    }
                }

            } else {

                nonTxEntityManager = entityManagerFactory.createEntityManager
                    (emProperties);

                // Return a new non-transactional entity manager.
                delegate = nonTxEntityManager;
                    
            }

        } else {

            // EXTENDED Persitence Context

            if( extendedEntityManager == null ) {
                extendedEntityManager = containerFactory.
                    lookupExtendedEntityManager(entityManagerFactory);
                    
            }          
      
            delegate = extendedEntityManager;

        }
        
        if( _logger.isLoggable(Level.FINE) ) {
            _logger.fine("In EntityManagerWrapper::_getDelegate(). " +
                         "Logical entity manager  = " + this);
            _logger.fine("Physical entity manager = " + delegate);
        }

        return delegate;

    }
    
    private void cleanupNonTxEntityManager() {
        if( nonTxEntityManager != null ) {
            nonTxEntityManager.close();
            nonTxEntityManager = null;
        }
    }

    public void persist(Object entity) {
        doTransactionScopedTxCheck();
        
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.PERSIST);
            }
            _getDelegate().persist(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public <T> T merge(T entity) {
        doTransactionScopedTxCheck();
        
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.MERGE);
            }
            return _getDelegate().merge(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public void remove(Object entity) {
        doTransactionScopedTxCheck();
        
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.REMOVE);
            }
            _getDelegate().remove(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        
        // tx is required so there's no need to do any non-tx cleanup
    }

    public <T> T find(Class<T> entityClass, Object primaryKey) {
        T returnValue = null;
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND);
            }
            returnValue = _getDelegate().find(entityClass, primaryKey);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        T returnValue = null;
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_REFERENCE);
            }
            returnValue = _getDelegate().getReference(entityClass, primaryKey);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public void flush() {
        // tx is ALWAYS required, regardless of persistence context type.
        doTxRequiredCheck();
        
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FLUSH);
            }
            _getDelegate().flush();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        
        // tx is required so there's no need to do any non-tx cleanup
    }

    public Query createQuery(String ejbqlString) {
        Query returnValue = null;
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createQuery(ejbqlString);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, ejbqlString);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public Query createNamedQuery(String name) {
        Query returnValue = null;
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NAMED_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNamedQuery(name);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNamedQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, name);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        return returnValue;
    }

    public Query createNativeQuery(String sqlString) {
        Query returnValue = null;
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery(sqlString);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNativeQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, sqlString);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public Query createNativeQuery(String sqlString, Class resultClass) {
        Query returnValue = null;
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING_CLASS);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery(sqlString, resultClass);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNativeQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, sqlString, resultClass);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        Query returnValue = null;
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING_STRING);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery
                (sqlString, resultSetMapping);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNativeQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, sqlString, resultSetMapping);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public void refresh(Object entity) {
        doTransactionScopedTxCheck();
        
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.REFRESH);
            }
            _getDelegate().refresh(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        
        // tx is required so there's no need to do any non-tx cleanup
    }

    public boolean contains(Object entity) {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CONTAINS);
            }
            EntityManager delegate = _getDelegate();
            return delegate.contains(entity);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void close() {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CLOSE);
            callFlowAgent.entityManagerMethodEnd();
        }
        // close() not allowed on container-managed EMs.
        throw new IllegalStateException();
    }

    public boolean isOpen() {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.IS_OPEN);
            callFlowAgent.entityManagerMethodEnd();
        }
        // Not relevant for container-managed EMs.  Just return true.
        return true;
    }

    public EntityTransaction getTransaction() {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_TRANSACTION);
            }
            return _getDelegate().getTransaction();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void lock(Object entity, LockModeType lockMode) {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.LOCK);
            }
            _getDelegate().lock(entity, lockMode);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void clear() {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CLEAR);
            }
            _getDelegate().clear();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public Object getDelegate() {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_DELEGATE);
            }
            return _getDelegate();
        } finally {
            if( nonTxEntityManager != null ) {
                // In this case we can't close the physical EntityManager
                // before returning it to the application, so we just clear
                // the EM wrapper's reference to it.  
                nonTxEntityManager = null;
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    public FlushModeType getFlushMode() {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_FLUSH_MODE);
            }
            return _getDelegate().getFlushMode();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void setFlushMode(FlushModeType flushMode) {
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.SET_FLUSH_MODE);
            }
            _getDelegate().setFlushMode(flushMode);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void joinTransaction() {
        // Doesn't apply to the container-managed case, but all the
        // spec says is that an exception should be thrown if called
        // without a tx.
        doTxRequiredCheck();
        Agent callFlowAgent = Switch.getSwitch().getCallFlowAgent();
        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.JOIN_TRANSACTION);
            callFlowAgent.entityManagerMethodEnd();
        }

        // There's no point in calling anything on the physical 
        // entity manager since in all tx cases it will be
        // correctly associated with a tx already.
    }

}
