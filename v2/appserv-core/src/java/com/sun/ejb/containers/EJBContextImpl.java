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
package com.sun.ejb.containers;

import java.util.*;
import java.security.Identity;
import java.security.Principal;
import java.lang.reflect.Method;

import java.lang.ref.*;

import javax.ejb.*;
import javax.transaction.*;

import com.sun.ejb.*;
import com.sun.enterprise.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.RoleReference;

import com.sun.ejb.containers.util.cache.CacheEntry;

import java.util.logging.*;
import com.sun.logging.*;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Implementation of javax.ejb.EJBContext for the J2EE Reference Implementation.
 *
 */

public abstract class EJBContextImpl
    implements EJBContext, ComponentContext, java.io.Serializable
{
    private static final Logger _logger =
        LogDomains.getLogger(LogDomains.EJB_LOGGER);
    
    private static LocalStringManagerImpl localStrings =
    new LocalStringManagerImpl(EJBContextImpl.class);
    protected static final int NOT_INITIALIZED = -999;  // not initialized state
    
    private Object ejb;
    
    // These are all transient to prevent serialization during passivation
    // Note: all these will be initialized to default values during
    // deserialization.
    transient protected BaseContainer container;
    
    transient protected Transaction transaction = null;
    transient private Context initialContext = null;
    transient private ArrayList resources;
    transient private int concInvokeCount = 0;
    
    // the EJBObject's client-side RMI stub
    transient protected EJBObject ejbStub=null;
    transient protected EJBObjectImpl ejbObjectImpl;

    transient protected EJBObjectImpl ejbRemoteBusinessObjectImpl;

    transient protected EJBLocalObjectImpl ejbLocalObjectImpl;
    transient protected EJBLocalObjectImpl ejbLocalBusinessObjectImpl;
    
    transient private long lastTimeUsed;
    transient protected int state;
    
    // true if the bean exposes a RemoteHome/Remote view  
    // (not 3.0 business view)
    protected final boolean isRemoteInterfaceSupported;

    // true if the bean exposes a LocalHome/Localview 
    // (not 3.0 business view)
    protected final boolean isLocalInterfaceSupported;

    // can't/doesn't set the context to DESTROYED until after calling ejbRemove
    // but it needs a way to know if bean is being removed.  Standardizing
    // on the DESTROYED state doesn't help, since often times the container
    // can't/doesn't set the context to DESTROYED until after calling ejbRemove
    transient protected boolean inEjbRemove;
    
    private Object[]    interceptorInstances;
    
    EJBContextImpl(Object ejb, BaseContainer container) {
        this.ejb = ejb;
        this.container = container;
        state = NOT_INITIALIZED;
        inEjbRemove = false;

        isRemoteInterfaceSupported = container.isRemoteInterfaceSupported();
        isLocalInterfaceSupported  = container.isLocalInterfaceSupported();
    }
    
    public Transaction getTransaction() {
        return transaction;
    }
    
    public void setTransaction(Transaction tr) {
        transaction = tr;
    }
    
    void setEJBStub(EJBObject ejbStub) {
        this.ejbStub = ejbStub;
    }
    
    void setEJBLocalObjectImpl(EJBLocalObjectImpl localObjectImpl) {
        this.ejbLocalObjectImpl = localObjectImpl;
    }

    void setEJBLocalBusinessObjectImpl(EJBLocalObjectImpl localBusObjectImpl) {
        this.ejbLocalBusinessObjectImpl = localBusObjectImpl;
    }

    
    void setEJBObjectImpl(EJBObjectImpl ejbo) {
        this.ejbObjectImpl = ejbo;
    }
    
    EJBObjectImpl getEJBObjectImpl() {
        return ejbObjectImpl;
    }
    
    void setEJBRemoteBusinessObjectImpl(EJBObjectImpl ejbo) {
        this.ejbRemoteBusinessObjectImpl = ejbo;
    }

    EJBObjectImpl getEJBRemoteBusinessObjectImpl() {
        return this.ejbRemoteBusinessObjectImpl;
    }

    EJBLocalObjectImpl getEJBLocalObjectImpl() {
        return ejbLocalObjectImpl;
    }

    EJBLocalObjectImpl getEJBLocalBusinessObjectImpl() {
        return ejbLocalBusinessObjectImpl;
    }
    
    void setContainer(BaseContainer container) {
        this.container = container;
    }
    
    void setState(int s) {
        state = s;
    }

    boolean isTimedObject() {
        return container.isTimedObject();
    }

    int getState() {
        return state;
    }
    
    void setInEjbRemove(boolean beingRemoved) {
        inEjbRemove = beingRemoved;
    }
    
    boolean isInEjbRemove() {
        return inEjbRemove;
    }
    
    /**
     * Returns true if this context has NOT progressed past its initial
     * state.  The point at which this happens is container-specific.
     */
    boolean isUnitialized() {
        return (state == NOT_INITIALIZED);
    }
    
    public long getLastTimeUsed() {
        return lastTimeUsed;
    }
    
    void setSoftRef(SoftReference softRef) {
    }
    
    void setHardRef(Object referent) {
    }
    
    void touch() {
        lastTimeUsed = System.currentTimeMillis();
    }
    
    
    
    /**************************************************************************
    The following are implementations of ComponentContext methods.
     **************************************************************************/
    
    /**
     *
     */
    public Object getEJB() {
        return ejb;
    }
    
    
    public Container getContainer() {
        return container;
    }
    
    /**
     * Register a resource opened by the EJB instance
     * associated with this Context.
     */
    public void registerResource(ResourceHandle h) {
        if ( resources == null )
            resources = new ArrayList();
        resources.add(h);
    }
    
    /**
     * Unregister a resource from this Context.
     */
    public void unregisterResource(ResourceHandle h) {
        if ( resources == null )
            resources = new ArrayList();
        resources.remove(h);
    }

    /**
     * Get all the resources associated with the context
     */
    public List getResourceList() {
        if (resources == null)
            resources = new ArrayList(0);
        return resources;
    }
    
    
    /**
     * Get the number of concurrent invocations on this bean
     * (could happen with re-entrant bean).
     * Used by TM.
     */
    public int getConcurrentInvokeCount() {
        return concInvokeCount;
    }
    
    /**
     * Increment the number of concurrent invocations on this bean
     * (could happen with re-entrant bean).
     * Used by TM.
     */
    public synchronized void incrementConcurrentInvokeCount() {
        concInvokeCount++;
    }
    
    /**
     * Decrement the number of concurrent invocations on this bean
     * (could happen with re-entrant bean).
     * Used by TM.
     */
    public synchronized void decrementConcurrentInvokeCount() {
        concInvokeCount--;
    }
    
    /**************************************************************************
    The following are implementations of EJBContext methods.
     **************************************************************************/
    
    /**
     * This is a SessionContext/EntityContext method.
     */
    public EJBObject getEJBObject()
        throws IllegalStateException
    {
        if (ejbStub == null) {
            throw new IllegalStateException("EJBObject not available");
        }

        return ejbStub;
    }
    
    /**
     * This is a SessionContext/EntityContext method.
     */
    public EJBLocalObject getEJBLocalObject()
        throws IllegalStateException
    {
        if ( ejbLocalObjectImpl == null ) {
            throw new IllegalStateException("EJBLocalObject not available");
        }
        
        // Have to convert EJBLocalObjectImpl to the client-view of 
        // EJBLocalObject
        return (EJBLocalObject) ejbLocalObjectImpl.getClientObject();
    }
    
    /**
     *
     */
    public EJBHome getEJBHome() {
        if (! isRemoteInterfaceSupported) {
            throw new IllegalStateException("EJBHome not available");
        }

        return container.getEJBHomeStub();
    }
    
    
    /**
     *
     */
    public EJBLocalHome getEJBLocalHome() {
        if (! isLocalInterfaceSupported) {
            throw new IllegalStateException("EJBLocalHome not available");
        }

        return container.getEJBLocalHome();
    }
    
    
    /**
     *
     */
    public Properties getEnvironment() {
        // This is deprecated, see EJB2.0 section 20.6.
        return container.getEnvironmentProperties();
    }
    
    /**
     * @deprecated
     */
    public Identity getCallerIdentity() {
        // This method is deprecated.
        // see EJB2.0 section 21.2.5
        throw new RuntimeException(
        "getCallerIdentity() is deprecated, please use getCallerPrincipal().");
    }


    public Object lookup(String name) {
        Object o = null;

        if( name == null ) {
            throw new IllegalArgumentException("Argument is null");
        }
        try {
            if( initialContext == null ) {
                initialContext = new InitialContext();
            }
            // name is relative to the private component namespace
            o = initialContext.lookup("java:comp/env/" + name);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
        return o;
    }
    
    /**
     *
     */
    public Principal getCallerPrincipal() {

        checkAccessToCallerSecurity();

        com.sun.enterprise.SecurityManager sm = container.getSecurityManager();
        return sm.getCallerPrincipal();
    }
    
    
    /**
     * @deprecated
     */
    public boolean isCallerInRole(Identity identity) {
        // THis method is deprecated.
        // This implementation is as in EJB2.0 section 21.2.5
        return isCallerInRole(identity.getName());
    }
    
    
    /**
     *
     */
    public boolean isCallerInRole(String roleRef) {
        if ( roleRef == null )
            throw new IllegalArgumentException("Argument is null");

        checkAccessToCallerSecurity();
        
        EjbDescriptor ejbd = container.getEjbDescriptor();
        RoleReference rr = ejbd.getRoleReferenceByName(roleRef);
        
        if ( rr == null ) {
            throw new IllegalArgumentException(
                "No mapping available for role reference " + roleRef);
        }
        
        com.sun.enterprise.SecurityManager sm = container.getSecurityManager();
	return sm.isCallerInRole(roleRef);
    }
    
    /**
     * Overridden in containers that allow access to isCallerInRole() and
     * getCallerPrincipal()
     */
    protected void checkAccessToCallerSecurity()
        throws IllegalStateException
    {
        throw new IllegalStateException("Operation not allowed");
    }
    
    /**
     *
     */
    public UserTransaction getUserTransaction()
        throws IllegalStateException
    {
        throw new IllegalStateException("Operation not allowed");
    }
    
    /**
     *
     */
    public void setRollbackOnly()
        throws IllegalStateException
    {
        if ( state == NOT_INITIALIZED )
            throw new IllegalStateException("EJB not in READY state");
        
        // EJB2.0 section 7.5.2: only EJBs with container managed transactions
        // can use this method.
        if ( container.isBeanManagedTx() )
            throw new IllegalStateException(
                "Illegal operation for bean-managed transactions");
        
        J2EETransactionManager tm = Switch.getSwitch().getTransactionManager();
        
        try {
            if ( tm.getStatus() == Status.STATUS_NO_TRANSACTION ) {
                // EJB might be in a non-business method (for SessionBeans)
                // or afterCompletion.
                // OR this was a NotSupported/Never/Supports
                // EJB which was invoked without a global transaction.
                // In that case the JDBC connection would have autoCommit=true
                // so the container doesnt have to do anything.
                throw new IllegalStateException("No transaction context.");
            }
            
            checkActivatePassivate();
            
            tm.setRollbackOnly();
            
        } catch (Exception ex) {
            IllegalStateException illEx = new IllegalStateException(ex.toString());
            illEx.initCause(ex);
            throw illEx;
        }
    }
    
    /**
     *
     */
    public boolean getRollbackOnly()
        throws IllegalStateException
    {
        if ( state == NOT_INITIALIZED )
            throw new IllegalStateException("EJB not in READY state");
        
        // EJB2.0 section 7.5.2: only EJBs with container managed transactions
        // can use this method.
        if ( container.isBeanManagedTx() )
            throw new IllegalStateException(
                "Illegal operation for bean-managed transactions");
        
        J2EETransactionManager tm = Switch.getSwitch().getTransactionManager();
        
        try {
            int status = tm.getStatus();
            if ( status == Status.STATUS_NO_TRANSACTION ) {
                // EJB which was invoked without a global transaction.
                throw new IllegalStateException("No transaction context.");
            }
            
            checkActivatePassivate();
            
            if ( status == Status.STATUS_MARKED_ROLLBACK
            || status == Status.STATUS_ROLLEDBACK
            || status == Status.STATUS_ROLLING_BACK )
                return true;
            else
                return false;
        } catch (Exception ex) {
            _logger.log(Level.FINE, "Exception in method getRollbackOnly()", 
                ex);
            IllegalStateException illEx = new IllegalStateException(ex.toString());
            illEx.initCause(ex);
            throw illEx;
        }
    }
    
    /**************************************************************************
    The following are EJBContextImpl-specific methods.
     **************************************************************************/
    void setInterceptorInstances(Object[] instances) {
        this.interceptorInstances = instances;
    }
    
    public Object[] getInterceptorInstances() {
        return this.interceptorInstances;
    }
    
    /**
     * The EJB spec makes a distinction between access to the TimerService
     * object itself (via EJBContext.getTimerService) and access to the
     * methods on TimerService, Timer, and TimerHandle.  The latter case
     * is covered by this check.  It is overridden in the applicable concrete
     * context impl subclasses.
     */
    public void checkTimerServiceMethodAccess()
        throws IllegalStateException
    {
        throw new IllegalStateException("EJB Timer Service method calls " +
        "cannot be called in this context");
    }
    
    // Throw exception if EJB is in ejbActivate/Passivate
    protected void checkActivatePassivate()
        throws IllegalStateException
    {
        if( inActivatePassivate() ) {
            throw new IllegalStateException("Operation not allowed.");
        }
        
    }
    
    protected boolean inActivatePassivate() {
        return inActivatePassivate(
            container.invocationManager.getCurrentInvocation());
    }

    protected boolean inActivatePassivate(ComponentInvocation inv) {
        boolean inActivatePassivate = false;
        if ( inv instanceof Invocation ) {
            Method currentMethod = ((Invocation)inv).method;
            inActivatePassivate  = (currentMethod != null)
                ? (currentMethod.getName().equals("ejbActivate") ||
                   currentMethod.getName().equals("ejbPassivate")
                  )
                : false;
        }
        return inActivatePassivate;
    } 
    
    /**
     * Called after this context is freed up.
     */
    void deleteAllReferences() {
        ejb = null;
        container = null;
        transaction = null;
        resources = null;
        ejbStub = null;
        ejbObjectImpl = null;
        ejbRemoteBusinessObjectImpl = null;
        ejbLocalObjectImpl = null;
        ejbLocalBusinessObjectImpl = null;
    }
	
}
