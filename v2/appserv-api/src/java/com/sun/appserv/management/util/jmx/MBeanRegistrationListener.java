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
package com.sun.appserv.management.util.jmx;

import java.io.IOException;

import javax.management.ObjectName;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.MBeanServerNotification;
import javax.management.MBeanServerConnection;
import javax.management.InstanceNotFoundException;
import com.sun.appserv.management.base.AMXRootLogger;

/**
	Convenience base class for listening to 
	{@link MBeanServerNotification} notifications.
	A class extending this class must implement {@link #mbeanRegistered}
	and {@link #mbeanUnregistered}.
	<p>
	The class is designed to start listening upon creation.
	The caller should call cleanup() when listening is no longer
	desired.  Once cleanup() is called, no further listening can
	be done; a new MBeanRegistrationListener should be instantiated
	if further listening is desired.
 */
public abstract class MBeanRegistrationListener extends NotificationListenerBase
{
    private final ObjectName    mRegUnregFilter;
    private final String        mDefaultDomain;
    
	/**
	    If 'constrain' is non-null, then all registration and unregistration
	    events will be filtered through it.  Only those MBeans
	    matching will be passed through to {@link #mbeanRegistered}
	    and {@link #mbeanUnregistered}.
	    
	    @param conn
	    @param constrain     optional fixed or pattern ObjectName
	 */
	    protected
	MBeanRegistrationListener(
        final String name,
	    final MBeanServerConnection conn,
	    final ObjectName    constrain )
		throws InstanceNotFoundException, IOException
	{
	    super( name, conn, JMXUtil.getMBeanServerDelegateObjectName() );
	    mRegUnregFilter = constrain;
	    
	    mDefaultDomain  = conn.getDefaultDomain();
	}
    
	/**
	    Calls this( conn, null ).
	    @param conn
	 */
	   protected
	MBeanRegistrationListener(
        final String name,
        final MBeanServerConnection conn)
		throws InstanceNotFoundException, IOException
	{
	    this( name, conn, (ObjectName)null );
	}
    
    protected abstract void mbeanRegistered( final ObjectName objectName );
    protected abstract void mbeanUnregistered( final ObjectName objectName );
        
        public void
	handleNotification( final Notification notifIn, final Object handback)
	{
		if ( ! (notifIn instanceof MBeanServerNotification) )
		{
		    throw new IllegalArgumentException( notifIn.toString() );
		}
		
		final MBeanServerNotification notif	= (MBeanServerNotification)notifIn;
		final ObjectName objectName	= notif.getMBeanName();
		final String     type	= notif.getType();
		
		final boolean matchesFilter   = (mRegUnregFilter == null) ||
			JMXUtil.matchesPattern( mDefaultDomain, mRegUnregFilter, objectName );
			
		if ( matchesFilter )
		{
            if ( type.equals( MBeanServerNotification.REGISTRATION_NOTIFICATION  ) )
            {
                mbeanRegistered( objectName );
            }
            else if ( type.equals( MBeanServerNotification.UNREGISTRATION_NOTIFICATION  ) )
            {
                mbeanUnregistered( objectName );
            }
        }
	}
}




