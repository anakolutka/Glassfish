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
package org.glassfish.admin.amx.loader;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.client.ProxyFactory;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.TimingDelta;
import org.glassfish.admin.amx.util.SingletonEnforcer;
import org.glassfish.admin.mbeanserver.AppserverMBeanServerFactory;
import org.glassfish.api.Async;
import org.glassfish.api.Startup;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.glassfish.admin.amx.config.AMXConfigLoader;
import org.glassfish.admin.amx.util.ImplUtil;

import org.glassfish.admin.mbeanserver.AMXBooter;
import org.glassfish.admin.mbeanserver.PendingConfigBeans;


/**
    Startup service that waits for AMX to be pinged to load.  At startup, it registers
    itself as an MBean after first loading a JMXXConnector so that the outside world can
    "talk" to it. This initial sequence is very fast (~20ms), but does not load any
    AMX MBeans, not even DomainRoot.
    <p>
    Later, the {@link #startAMX} method can be invoked on the MBean to cause AMX
    to load all the AMX MBeans.
 */
@Service
@Async
public final class AMXStartupService
    implements  Startup,
                org.jvnet.hk2.component.PostConstruct,
                org.jvnet.hk2.component.PreDestroy,
                AMXStartupServiceMBean
{
    private static void debug( final String s ) { System.out.println(s); }
    
    @Inject//(name=AppserverMBeanServerFactory.OFFICIAL_MBEANSERVER)
    private MBeanServer mMBeanServer;
    
    @Inject
    private volatile PendingConfigBeans mPendingConfigBeans;
    
    private volatile ObjectName      mAMXLoaderObjectName;
    private volatile J2EELoader      mJ2EELoader;
    private volatile AMXConfigLoader mConfigLoader;
    
    public AMXStartupService()
    {
    }
    
    private static ObjectName getObjectName()
    {
        return AMXBooter.STARTUP_OBJECT_NAME;
    }
    
    public void postConstruct()
    {
        //debug( "AMXStartupService.postConstruct()" );
        final TimingDelta delta = new TimingDelta();
        
        SingletonEnforcer.register( this.getClass(), this );
        
        if ( mMBeanServer == null ) throw new Error( "AMXStartup: null MBeanServer" );
        if ( mPendingConfigBeans == null ) throw new Error( "AMXStartup: null mPendingConfigBeans" );
        
        try
        {
            mMBeanServer.registerMBean( this, getObjectName() );
        }
        catch( JMException e )
        {
            throw new Error(e);
        }
        //debug( "AMXStartupService.postConstruct(): registered: " + getObjectName());
        
        //debug( "Initialized AMX Startup service in " + delta.elapsedMillis() + " ms " );
    }

    public void preDestroy() {
        debug( "AMXStartupService.preDestroy()" );
        stopAMX();
    }

    public synchronized ObjectName
    getDomainRootObjectName()
    {
        try
        { 
            // might not be ready yet
            return Util.getObjectName( getDomainRoot() );
        }
        catch( Exception e )
        {
            return null;
        }
    }
    
    public JMXServiceURL[] getJMXServiceURLs()
    {
        try
        {
            return (JMXServiceURL[])mMBeanServer.getAttribute( AMXBooter.BOOTER_OBJECT_NAME, "JMXServiceURLs" );
        }
        catch ( final JMException e )
        {
            throw new RuntimeException(e);
        }
    }

    /**
        Return a proxy to the AMXStartupService.
     */
        public static AMXStartupServiceMBean
    getAMXStartupServiceMBean( final MBeanServer mbs )
    {
        AMXStartupServiceMBean ss = null;
        
        if ( mbs.isRegistered( getObjectName() ) )
        {
            ss = AMXStartupServiceMBean.class.cast(
                MBeanServerInvocationHandler.newProxyInstance( mbs, getObjectName(), AMXStartupServiceMBean.class, false));
        }
        return ss;
    }
    
        DomainRoot
    getDomainRoot()
    {
        return ProxyFactory.getInstance( mMBeanServer ).getDomainRoot();
    }
    
        public synchronized ObjectName
    startAMX()
    {
        if ( getDomainRootObjectName() == null )
        {
            final TimingDelta delta = new TimingDelta();

            // loads the high-level AMX MBeans, like DomainRoot, QueryMgr, etc
            mAMXLoaderObjectName = LoadAMX.loadAMX( mMBeanServer );
            
            // do this before loading any ConfigBeans so that it will auto-sync
            mJ2EELoader = new J2EELoader(mMBeanServer);
            mJ2EELoader.start();
            
            // load config MBeans
            mConfigLoader = new AMXConfigLoader(mMBeanServer, mPendingConfigBeans);
            mConfigLoader.start();
            
            getDomainRoot().waitAMXReady();
            
            debug( "AMXStartupService: Loaded AMX MBeans in " + delta.elapsedMillis() + " ms " );
        }
        return getDomainRootObjectName();
    }
    
    public synchronized void stopAMX()
    {
        if ( getDomainRoot() != null )
        {
            ImplUtil.unregisterAMXMBeans( getDomainRoot() );
        }
    }

    public Startup.Lifecycle getLifecycle() { return Startup.Lifecycle.SERVER; }
}










