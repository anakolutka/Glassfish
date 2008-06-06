/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package org.glassfish.admin.mbeanserver;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.glassfish.api.Async;
import org.glassfish.api.Startup;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;



/**
    Registers the AMX Booter MBean.

    Public API is the name of the booter MBean: "amx-support:name=amx-booter" along with the
    methods found in AMXStartupServiceMBean, in particular bootAMX(),
 */
final class Booter implements BooterMBean
{
    private final MBeanServer mMBeanServer;
    private final ObjectName  mObjectName;
    private final Habitat     mHabitat;
    private ObjectName        mDomainRootObjectName;
    
    //@Inject
   // private org.glassfish.admin.amx.config.AMXConfigRegistrar mConfigRegistrar;
   
    private static void debug( final String s ) { System.out.println(s); }
    
    private Booter(
        final Habitat habitat,
        final MBeanServer mbeanServer)
    {
        mHabitat = habitat;
        mMBeanServer = mbeanServer;
        mObjectName = BooterMBean.OBJECT_NAME;
        mDomainRootObjectName = null;
        
        if ( mMBeanServer.isRegistered(mObjectName) )
        {
            throw new IllegalStateException();
        }
    }
    
    /**
        Create an instance of the booter.
     */
        public static synchronized Booter
    create( final Habitat habitat, final MBeanServer server )
    {
        final Booter  booter = new Booter( habitat, server);
        final ObjectName objectName = BooterMBean.OBJECT_NAME;
        
        try
        {
            if ( ! server.registerMBean( booter, objectName ).getObjectName().equals(objectName) )
            {
                throw new IllegalStateException();
            }
        }
        catch( JMException e )
        {
            throw new IllegalStateException(e);
        }
        return booter;
    }
    
    /**
        We need to dynamically load the AMX module.  HOW?  we can't depend on the amx-impl module.
        
        For now though, assume that a well-known MBean is available through other means via
        the amx-impl module.
     */
    public synchronized ObjectName bootAMX()
    {
        if ( mDomainRootObjectName == null )
        {
            debug( "Booter.bootAMX: getting AMXStartupServiceMBean via contract" );
            final AMXStartupServiceMBean loader = mHabitat.getByContract(AMXStartupServiceMBean.class);
            //loader.loadAMX(...)
            
            debug( "Booter.bootAMX: assuming that amx-impl loads through other means" );
        
            final ObjectName startupON = AMXStartupServiceMBean.OBJECT_NAME;
            if ( ! mMBeanServer.isRegistered(startupON) )
            {
                debug( "AMX MBean not yet available: " + startupON );
                throw new IllegalStateException( "AMX MBean not yet available: " + startupON );
            }
            
            try
            {
                debug( "Booter.bootAMX: invoking startAMX() on " + startupON);
                mDomainRootObjectName = (ObjectName)mMBeanServer.invoke( startupON, "startAMX", null, null);
                debug( "Booter.bootAMX: domainRoot = " + mDomainRootObjectName);
            }
            catch( final JMException e )
            {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        
        return mDomainRootObjectName;
    }
    
    /**
        Return the JMXServiceURLs for all connectors we've loaded.
     */
        public JMXServiceURL[]
    getJMXServiceURLs()
    {
        final ObjectName queryPattern = Util.newObjectName("jmxremote:type=jmx-connector,*");
        final Set<ObjectName>  objectNames = mMBeanServer.queryNames(queryPattern, null);
        
        final List<JMXServiceURL> urls = new ArrayList<JMXServiceURL>();
        for( final ObjectName objectName : objectNames )
        {
            try
            {
                urls.add( (JMXServiceURL)mMBeanServer.getAttribute( objectName, "Address" ) );
            }
            catch( JMException e )
            {
                e.printStackTrace();
                // ignore
            }
        }
        
        return urls.toArray( new JMXServiceURL[urls.size()] );
    }
}


















