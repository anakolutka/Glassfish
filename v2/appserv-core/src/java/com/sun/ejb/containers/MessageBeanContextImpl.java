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

import java.rmi.RemoteException;
import java.security.Principal;

import javax.ejb.*;
import javax.jms.*;

import javax.transaction.UserTransaction;

import com.sun.ejb.*;
import com.sun.enterprise.*;
import com.sun.enterprise.log.Log;
import java.util.logging.*;
import com.sun.logging.*;

/**
 * Implementation of EJBContext for message-driven beans
 *
 * @author Kenneth Saks
 */

public final class MessageBeanContextImpl extends EJBContextImpl implements MessageDrivenContext
{ 
    private static final Logger _logger =
        LogDomains.getLogger(LogDomains.MDB_LOGGER);

    private boolean afterSetContext = false;

    MessageBeanContextImpl(Object ejb, BaseContainer container)
    {
        super(ejb, container);
    }    

    void setEJBStub(EJBObject ejbStub)
    {
	throw new RuntimeException("No stubs for Message-driven beans");
    }

    void setEJBObjectImpl(EJBObjectImpl ejbo)
    {
	throw new RuntimeException("No EJB Object for Message-driven beans");
    }

    EJBObjectImpl getEJBObjectImpl()
    {
	throw new RuntimeException("No EJB Object for Message-driven beans");
    }

    public void setContextCalled() {
        this.afterSetContext = true;
    }

    /*****************************************************************
     *    The following are implementations of EJBContext methods.
     ******************************************************************/

    /**
     * 
     */
    public UserTransaction getUserTransaction()
	throws java.lang.IllegalStateException
    {
	// The state check ensures that an exception is thrown if this
	// was called from the constructor or setMessageDrivenContext.
	// The remaining checks are performed by the container.
	if ( !this.afterSetContext ) {
	    throw new java.lang.IllegalStateException("Operation not allowed");
        }

	return ((BaseContainer)getContainer()).getUserTransaction();
    }

    /*
     * Doesn't make any sense to get EJBHome object for 
     * a message-driven ejb.
     */
    public EJBHome getEJBHome() 
    {
        RuntimeException exception = new java.lang.IllegalStateException
            ("getEJBHome not allowed for message-driven beans");
        throw exception;
    }

    public boolean isCallerInRole(String roleRef)
    {
        throw new java.lang.IllegalStateException("isCallerInRole() is not defined for message-driven ejbs");
    }

    protected void checkAccessToCallerSecurity()
        throws java.lang.IllegalStateException
    {
        // A message-driven ejb's state transitions past UNINITIALIZED
        // AFTER ejbCreate
        if ( isUnitialized() || isInEjbRemove() ) {
            throw new java.lang.IllegalStateException("Operation not allowed");
        }

    }
    
    public TimerService getTimerService() 
        throws java.lang.IllegalStateException {

        if( !afterSetContext ) {
            throw new java.lang.IllegalStateException("Operation not allowed");
        }

        ContainerFactoryImpl cf = (ContainerFactoryImpl)
            Switch.getSwitch().getContainerFactory();
        EJBTimerService timerService = cf.getEJBTimerService();
        if( timerService == null ) {
            throw new EJBException("EJB Timer service not available");
        }
        return new EJBTimerServiceWrapper(timerService, this);
    }

    public void checkTimerServiceMethodAccess() 
        throws java.lang.IllegalStateException {

        // A message-driven ejb's state transitions past UNINITIALIZED
        // AFTER ejbCreate
        if ( isUnitialized() || isInEjbRemove() ) {
            throw new java.lang.IllegalStateException
                ("EJB Timer Service method calls cannot be called in " +
                 " this context");
        } 
    }

}
