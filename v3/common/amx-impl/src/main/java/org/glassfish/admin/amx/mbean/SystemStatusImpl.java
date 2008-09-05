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
package org.glassfish.admin.amx.mbean;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.management.base.SystemStatus;
import com.sun.appserv.management.base.UnprocessedConfigChange;
import com.sun.appserv.management.config.JDBCConnectionPoolConfig;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;

import javax.management.ObjectName;
import javax.resource.ResourceException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.beans.PropertyChangeEvent;

import org.glassfish.admin.amx.util.Issues;

import org.jvnet.hk2.config.*;

import org.glassfish.admin.mbeanserver.UnprocessedConfigListener;

/**
    
 */
public final class SystemStatusImpl extends AMXNonConfigImplBase
	implements SystemStatus
{
    public SystemStatusImpl(final ObjectName parentObjectName)
	{
        super( SystemStatus.J2EE_TYPE, SystemStatus.J2EE_TYPE, parentObjectName, SystemStatus.class, null );
	}
    
        private Habitat
    getHabitat()
    {
        return  org.glassfish.internal.api.Globals.getDefaultHabitat();
    }
    
        public Map<String,Object>
    pingJDBCConnectionPool( final String poolName )
    {
        final Map<String,Object> result = new HashMap<String,Object>();
        final Habitat habitat = getHabitat();
        ConnectorRuntime connRuntime = null;

        result.put( PING_SUCCEEDED_KEY, false);
        if (habitat == null)
        {
            result.put( REASON_FAILED_KEY, "Habitat is null");
            return result;
        }
            
        // check pool name
        final JDBCConnectionPoolConfig  cfg = 
                getDomainRoot().getDomainConfig().getResourcesConfig().getJDBCConnectionPoolConfigMap().get( poolName );
        if (cfg == null)
        {
            result.put( REASON_FAILED_KEY, "The pool name " + poolName + " does not exist");
            return result;
        }

        // get connector runtime
        try {
            connRuntime = habitat.getComponent(ConnectorRuntime.class, null);
        }
        catch ( final ComponentException e)
        {
            result.putAll( ExceptionUtil.toMap(e) );
            result.put( REASON_FAILED_KEY, ExceptionUtil.toString(e));
            return result;
        }
        
        // do the ping
        try {
            final boolean pingable = connRuntime.pingConnectionPool(poolName);
            result.put( PING_SUCCEEDED_KEY, pingable);
        }
        catch ( final ResourceException e)
        {
            result.putAll( ExceptionUtil.toMap(e) );
            assert REASON_FAILED_KEY.equals( ExceptionUtil.MESSAGE_KEY );
            return result;
        }
        
        return result;
    }
    
//-------------------------------------
    
    private static void xdebug( final String s ) { System.out.println( "### " + s); }
    
    private static String str(final Object o ) {
        return o == null ? null : (""+o);
    }
    
        private ObjectName
    sourceToObjectName( final Object source )
    {
        ObjectName objectName = null;

        if (source instanceof ConfigBean)
        {
            objectName = ((ConfigBean)source).getObjectName();
        }
        else if ( source instanceof ConfigBeanProxy )
        {
            objectName = ((ConfigBean)Dom.unwrap((ConfigBeanProxy)source)).getObjectName();
        }
        else
        {
            xdebug( "UnprocessedConfigChange.sourceToObjectName: source is something else" );
        }
        
        return objectName;
    }
    
        public List<Object[]>
    getUnprocessedConfigChanges() {
        final UnprocessedConfigListener unp = getHabitat().getComponent( UnprocessedConfigListener.class );
        
        final List<UnprocessedChangeEvents> items = unp.getUnprocessedChangeEvents();
        
        final List<Object[]> changesObjects = new ArrayList<Object[]>();
        
        xdebug( "UnprocessedConfigChange: processing events: " + items.size() );
        for( final UnprocessedChangeEvents events : items )
        {
            for( final UnprocessedChangeEvent event : events.getUnprocessed() )
            {
        xdebug( "UnprocessedConfigChange: event: " + event );
                final String reason = event.getReason();
                final PropertyChangeEvent pce = event.getEvent();
                final long when = event.getWhen();
                
                if ( reason.startsWith("no ConfigListener listening") )
                {
                    // ugly kludge; see org.jvnet.hk2.config.Transactions.ConfigListenerJob.process()
                    continue;
                }
                
                final ObjectName objectName = sourceToObjectName(pce.getSource());
                
                final UnprocessedConfigChange ucc = new UnprocessedConfigChange(
                        pce.getPropertyName(),
                        str(pce.getOldValue()),
                        str(pce.getNewValue()),
                        objectName,
                        reason);
                xdebug( "UnprocessedConfigChange: " + ucc );
                changesObjects.add( ucc.toArray() );
            } 
        }
        
        return changesObjects;
    }
}








