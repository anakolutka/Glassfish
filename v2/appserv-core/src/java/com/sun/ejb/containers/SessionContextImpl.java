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

import java.lang.reflect.Method;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;

import javax.ejb.*;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

import com.sun.ejb.*;
import com.sun.enterprise.*;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;

import com.sun.ejb.spi.container.StatefulEJBContext;

import static com.sun.ejb.containers.StatefulSessionContainer.EEMRefInfo;
import static com.sun.ejb.containers.StatefulSessionContainer.EEMRefInfoKey;

/**
 * Implementation of EJBContext for SessionBeans
 *
 * @author Mahesh Kannan
 */

public final class SessionContextImpl
    extends EJBContextImpl
    implements SessionContext, StatefulEJBContext
{
    private Object instanceKey;
    private boolean completedTxStatus;
    private boolean afterCompletionDelayed=false;
    private boolean committing=false;
    private boolean inAfterCompletion=false;
    private boolean isStateless = false;
    private boolean isStateful  = false;

    private boolean existsInSessionStore = false; 
    private transient int refCount = 0;

    private boolean txCheckpointDelayed;
    private String ejbName;
    private long    lastPersistedAt;

    private long version;
    
    // Map of entity managers with extended persistence context 
    // for this stateful session bean.
    private transient Map<EntityManagerFactory, EntityManager> extendedEntityManagerMap;

    private transient Set<EntityManagerFactory> emfsRegisteredWithTx;

    //Used during activation to populate entries in the above maps
    //Also, EEMRefInfo implements IndirectlySerializable
    private Collection<EEMRefInfo> eemRefInfos = new HashSet<EEMRefInfo>();
    
    
    SessionContextImpl(Object ejb, BaseContainer container) {
        super(ejb, container);
        EjbSessionDescriptor sessionDesc = 
            (EjbSessionDescriptor) getContainer().getEjbDescriptor();
        isStateless = sessionDesc.isStateless();
        isStateful  = sessionDesc.isStateful();

	this.ejbName = sessionDesc.getName();
    }

    public String toString() {
	return ejbName + "; id: " + instanceKey;
    }

    public Map<EntityManagerFactory, EntityManager> getExtendedEntityManagerMap() {
        if( extendedEntityManagerMap == null ) {
            extendedEntityManagerMap = 
                new HashMap<EntityManagerFactory, EntityManager>();
        }
        return extendedEntityManagerMap;
    }
    
    Collection<EEMRefInfo> getAllEEMRefInfos() {
        return eemRefInfos;
    }

    void setEEMRefInfos(Collection<EEMRefInfo> val) {
        if (val != null) {
            eemRefInfos = val;
	}
    }

    public void addExtendedEntityManagerMapping(EntityManagerFactory emf,
		    EEMRefInfo refInfo) {
        getExtendedEntityManagerMap().put(emf, refInfo.getEntityManager());
    }

    public EntityManager getExtendedEntityManager(EntityManagerFactory emf) {
        return getExtendedEntityManagerMap().get(emf);
    }

    public Collection<EntityManager> getExtendedEntityManagers() {
        return getExtendedEntityManagerMap().values();
    }

    private Set<EntityManagerFactory> getEmfsRegisteredWithTx() {
        if( emfsRegisteredWithTx == null ) {
            emfsRegisteredWithTx = new HashSet<EntityManagerFactory>();
        }
        return emfsRegisteredWithTx;
    }

    public void setEmfRegisteredWithTx(EntityManagerFactory emf, boolean flag)
    {
        if( flag ) {
            getEmfsRegisteredWithTx().add(emf);
        } else {
            getEmfsRegisteredWithTx().remove(emf);
        }
    }

    public boolean isEmfRegisteredWithTx(EntityManagerFactory emf) {
        return getEmfsRegisteredWithTx().contains(emf);
    }

    public TimerService getTimerService() throws IllegalStateException {
        if( isStateful ) {
            throw new IllegalStateException
                ("EJBTimer Service is not accessible to Stateful Session ejbs");
        }

        // Instance key is first set between after setSessionContext and
        // before ejbCreate
        if ( instanceKey == null ) {
            throw new IllegalStateException("Operation not allowed");
        }

        ContainerFactoryImpl cf = (ContainerFactoryImpl)
        Switch.getSwitch().getContainerFactory();
        EJBTimerService timerService = cf.getEJBTimerService();
        if( timerService == null ) {
            throw new EJBException("EJB Timer service not available");
        }

        return new EJBTimerServiceWrapper(timerService, this);
    }
    
    public UserTransaction getUserTransaction()
        throws IllegalStateException
    {
        // The state check ensures that an exception is thrown if this
        // was called from setSession/EntityContext. The instance key check
        // ensures that an exception is not thrown if this was called
        // from a stateless SessionBean's ejbCreate.
        if ( (state == NOT_INITIALIZED) && (instanceKey == null) )
            throw new IllegalStateException("Operation not allowed");
        
        return ((BaseContainer)getContainer()).getUserTransaction();
    }
    
    public MessageContext getMessageContext() {
        if( isStateless ) {
            Switch theSwitch = Switch.getSwitch();
            InvocationManager invManager = theSwitch.getInvocationManager();
            try {
                ComponentInvocation inv = invManager.getCurrentInvocation();
                    
                if( (inv != null) && isWebServiceInvocation(inv) ) {
                    return ((Invocation)inv).messageContext;
                } else {
                    throw new IllegalStateException("Attempt to access " +
                       "MessageContext outside of a web service invocation");
                }
            } catch(Exception e) {
                IllegalStateException ise = new IllegalStateException();
                ise.initCause(e);
                throw ise;
            }
        } else {
            throw new IllegalStateException
                ("Attempt to access MessageContext from stateful session ejb");
        }
    }

    public <T> T getBusinessObject(Class<T> businessInterface) 
        throws IllegalStateException
    {

        // getBusinessObject not allowed for Stateless/Stateful beans
        // until after dependency injection
        if ( instanceKey == null ) {
            throw new IllegalStateException("Operation not allowed");
        }

        T businessObject = null;

        EjbDescriptor ejbDesc = container.getEjbDescriptor();

        if( businessInterface != null ) {
            String intfName = businessInterface.getName();
            
            if( (ejbLocalBusinessObjectImpl != null) &&
                ejbDesc.getLocalBusinessClassNames().contains(intfName) ) {

                // Get proxy corresponding to this business interface.
                businessObject = (T) ejbLocalBusinessObjectImpl
                    .getClientObject(intfName);

            } else if( (ejbRemoteBusinessObjectImpl != null) &&
                   ejbDesc.getRemoteBusinessClassNames().contains(intfName)) {

                // Create a new client object from the stub for this
                // business interface.
                String generatedIntf = 
                    EJBUtils.getGeneratedRemoteIntfName(intfName);

                java.rmi.Remote stub = 
                    ejbRemoteBusinessObjectImpl.getStub(generatedIntf);

                try {
                    businessObject = (T) EJBUtils.createRemoteBusinessObject
                        (container.getClassLoader(), intfName, stub);
                } catch(Exception e) {

                    IllegalStateException ise = new IllegalStateException
                        ("Error creating remote business object for " +
                         intfName);
                    ise.initCause(e);
                    throw ise;
                }
            }
        }
        
        if( businessObject == null ) {
            throw new IllegalStateException("Invalid business interface : " +
                businessInterface + " for ejb " + ejbDesc.getName());
        }
        
        return businessObject;
    }

    public Class getInvokedBusinessInterface() 
        throws IllegalStateException
    {

        Switch theSwitch = Switch.getSwitch();
        InvocationManager invManager = theSwitch.getInvocationManager();

        Class businessInterface = null;

        try {
            ComponentInvocation inv = invManager.getCurrentInvocation();
            
            if( (inv != null) && (inv instanceof Invocation) ) {
                Invocation invocation = (Invocation) inv;
                if( invocation.isBusinessInterface ) {
                    businessInterface = invocation.clientInterface;
                } 
            }
        } catch(Exception e) {
            IllegalStateException ise = new IllegalStateException();
            ise.initCause(e);
            throw ise;
        }        

        if( businessInterface == null ) {
            throw new IllegalStateException("Attempt to call " + 
               "getInvokedBusinessInterface outside the scope of a business " +
                                            "method");
        }

        return businessInterface;
    }
    
    protected void checkAccessToCallerSecurity()
        throws IllegalStateException
    {
        
        if( isStateless ) {
            // This covers constructor, setSessionContext, ejbCreate,
            // and ejbRemove. NOTE : For stateless session beans,
            // instances don't move past NOT_INITIALIZED until after ejbCreate.
            if( (state == NOT_INITIALIZED) || inEjbRemove ) {
                throw new IllegalStateException("Operation not allowed");
            }
        } else {
            // This covers constructor and setSessionContext.
            // For stateful session beans, instances move past
            // NOT_INITIALIZED after setSessionContext.
            if( state == NOT_INITIALIZED ) {
                throw new IllegalStateException("Operation not allowed");
            }
        }
        
    }
    
    
    public void checkTimerServiceMethodAccess()
        throws IllegalStateException
    {
        // checks that only apply to stateful session beans
        ComponentInvocation compInv = getCurrentComponentInvocation();
        if (isStateful) {
            if (
            inStatefulSessionEjbCreate(compInv) ||
            inActivatePassivate(compInv) ||
            inAfterCompletion ) {
                throw new IllegalStateException
                ("EJB Timer methods for stateful session beans cannot be " +
                " called in this context");
            }
        }
        
        // checks that apply to both stateful AND stateless
        if ( (state == NOT_INITIALIZED) || inEjbRemove ) {
            throw new IllegalStateException
            ("EJB Timer method calls cannot be called in this context");
        }
    }
    
    public Object getInstanceKey() {
        return instanceKey;
    }
    
    public void setInstanceKey(Object instanceKey) {
        this.instanceKey = instanceKey;
    }
    
    
    boolean getCompletedTxStatus() {
        return completedTxStatus;
    }
    
    void setCompletedTxStatus(boolean s) {
        this.completedTxStatus = s;
    }
    
    boolean isAfterCompletionDelayed() {
        return afterCompletionDelayed;
    }
    
    void setAfterCompletionDelayed(boolean s) {
        this.afterCompletionDelayed = s;
    }
    
    boolean isTxCompleting() {
        return committing;
    }
    
    void setTxCompleting(boolean s) {
        this.committing = s;
    }
    
    void setInAfterCompletion(boolean flag) {
        inAfterCompletion = flag;
    }

    private ComponentInvocation getCurrentComponentInvocation() {
        BaseContainer container = (BaseContainer) getContainer();
        return container.invocationManager.getCurrentInvocation();
    }
    
    private boolean isWebServiceInvocation(ComponentInvocation inv) {
        return (inv instanceof Invocation) && ((Invocation)inv).isWebService;
    }
    
    // Used to check if stateful session bean is in ejbCreate.
    // Since bean goes to READY state before ejbCreate is called by
    // EJBHomeImpl and EJBLocalHomeImpl, we can't rely on getState()
    // being NOT_INITIALIZED for operations matrix checks.
    private boolean inStatefulSessionEjbCreate(ComponentInvocation inv) {
        boolean inEjbCreate = false;
        if ( inv instanceof Invocation ) {
            Class clientIntf = ((Invocation)inv).clientInterface;
            // If call came through a home/local-home, this can only be a
            // create call.
            inEjbCreate = ((Invocation)inv).isHome &&
                (javax.ejb.EJBHome.class.isAssignableFrom(clientIntf) ||
                 javax.ejb.EJBLocalHome.class.isAssignableFrom(clientIntf));
        }
        return inEjbCreate;
    }
    
    void setTxCheckpointDelayed(boolean val) {
	this.txCheckpointDelayed = val;
    }

    boolean isTxCheckpointDelayed() {
	return this.txCheckpointDelayed;
    }

    long getLastPersistedAt() {
	return lastPersistedAt;
    }

    void setLastPersistedAt(long val) {
	this.lastPersistedAt = val;
    }

    public long getVersion() {
        return version;
    }
    
    public long incrementAndGetVersion() {
        return ++version;
    }

    public void setVersion(long newVersion) {
        this.version = newVersion;
    }

    /*************************************************************************/
    /************ Implementation of StatefulEJBContext ***********************/
    /*************************************************************************/

    public long getLastAccessTime() {
        return getLastTimeUsed();
    }

    public boolean canBePassivated() {
        return (state == StatefulSessionContainer.READY);
    }
    
    public boolean hasExtendedPC() {
        return (this.getExtendedEntityManagerMap().size() != 0);
    }

    public SessionContext getSessionContext() {
        return this;
    }

    public boolean existsInStore() {
        return existsInSessionStore ;
    }

    public void setExistsInStore(boolean val) {
        this.existsInSessionStore = val;
    }

    public final void incrementRefCount() {
        refCount++;
    }

    public final void decrementRefCount() {
        refCount--;
    }

    public final int getRefCount() {
        return refCount;
    }
    
}
