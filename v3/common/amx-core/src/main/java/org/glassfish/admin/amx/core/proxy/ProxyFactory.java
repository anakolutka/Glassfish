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
package org.glassfish.admin.amx.core.proxy;

import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.core.AMXProxy;

import org.glassfish.admin.amx.util.AMXDebugHelper;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.jmx.ConnectionSource;
import org.glassfish.admin.amx.util.jmx.MBeanServerConnectionConnectionSource;
import org.glassfish.admin.amx.util.jmx.MBeanServerConnectionSource;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.StringUtil;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.MBeanInfo;
import javax.management.relation.MBeanServerNotificationFilter;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.Descriptor;
import org.glassfish.admin.amx.annotation.Stability;
import org.glassfish.admin.amx.annotation.Taxonomy;
import org.glassfish.admin.amx.client.AppserverConnectionSource;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import org.glassfish.admin.amx.core.AMXConstants;

/**
	Factory for {@link AMXProxy} proxies.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class ProxyFactory implements NotificationListener
{
	private final ConnectionSource	mConnectionSource;
	private final ObjectName		mDomainRootObjectName;
	private final DomainRoot		mDomainRoot;
	private final String			mMBeanServerID;
    
    /**
        For immutable MBeanInfo, we want to pay the cost once and only once of a trip to the server.
        <p>
        Can we assume it's unique per *type* so that we can cache it once per type? If we could so so,
        the size of the cache would stay much smaller.
    */
    private final ConcurrentMap<ObjectName,MBeanInfo> mMBeanInfoCache = new ConcurrentHashMap<ObjectName,MBeanInfo>();
    
    private static final AMXDebugHelper mDebug  =
        new AMXDebugHelper( ProxyFactory.class.getName() );
    private static void debug( final Object... args )
    {
        //mDebug.println( args );
        System.out.println( StringUtil.toString( ", ", args) );
    }
	
	private static final Map<MBeanServerConnection,ProxyFactory> INSTANCES	=
	    Collections.synchronizedMap( new HashMap<MBeanServerConnection,ProxyFactory>() );
    
    /**
        Because ProxyFactory is used on both client and server, emitting anything to stdout
        or to the log is unacceptable in some circumstances.  Warnings remain available
        if the AMX-DEBUG system property allows it.
     */
        private static void
    warning( final Object... args )
    {
        debug( args );
    }
	
		private
	ProxyFactory( final ConnectionSource connSource )
	{
        mDebug.setEchoToStdOut( true );
        
		assert( connSource != null );
		
		mConnectionSource	= connSource;
		
		try
		{
			final MBeanServerConnection	conn	= getMBeanServerConnection();
			
			mMBeanServerID		= JMXUtil.getMBeanServerID( conn );
				
			mDomainRootObjectName = AMXBooter.findDomainRoot(conn);
            if ( mDomainRootObjectName == null )
            {
                throw new IllegalStateException( "ProxyFactory: AMX has not been started" );
            }
            System.out.println( "AMX DomainRoot ObjectName = " + mDomainRootObjectName );
            mDomainRoot           = getProxy(mDomainRootObjectName, DomainRoot.class);
			
			// we should always be able to listen to MBeans--
			// but the http connector does not support listeners
			try
			{
				final MBeanServerNotificationFilter	filter	=
					new MBeanServerNotificationFilter();
				filter.enableAllObjectNames();
				filter.disableAllTypes();
				filter.enableType( MBeanServerNotification.UNREGISTRATION_NOTIFICATION );
				
				JMXUtil.listenToMBeanServerDelegate( conn, this, filter, null );
			}
			catch( Exception e )
			{
				warning( "ProxyFactory: connection does not support notifications: ",
                    mMBeanServerID, connSource);
			}
			
			// same idea as above, this time we want to listen to connection died
			// plus there may not be a JMXConnector involved
			final JMXConnector	connector	= connSource.getJMXConnector( false );
			if ( connector != null )
			{
				try
				{
					connector.addConnectionNotificationListener( this, null, null );
				}
				catch( Exception e )
				{
					warning("addConnectionNotificationListener failed: ",
                        mMBeanServerID, connSource, e);
				}
			}
		}
		catch( Exception e )
		{
			warning( "ProxyFactory.ProxyFactory:\n", e );
			throw new RuntimeException( e );
		}
	}
	
	
	/**
		The connection is bad.  Tell each proxy its gone and remove it.
	 */
		private void
	connectionBad()
	{
        final Set<AMXProxy>   proxies  = new HashSet<AMXProxy>();
        
        for( final AMXProxy amx : proxies )
        {
            final AMXProxyHandler proxy = AMXProxyHandler.unwrap(amx);
            proxy.connectionBad();
        }
	}
	
	/**
		Verify that the connection is still alive.
	 */
		public boolean
	checkConnection()
	{
		boolean	connectionGood	= true;
		
		try
		{
			getMBeanServerConnection().isRegistered( JMXUtil.getMBeanServerDelegateObjectName() );
			connectionGood	= true;
		}
		catch( Exception e )
		{
			connectionBad();
		}
		
		return( connectionGood );
	}
	

		void
	notifsLost()
	{
		// should probably check each proxy for validity, but not clear if it's important...
	}
	
	/**
		Listens for MBeanServerNotification.UNREGISTRATION_NOTIFICATION and
		JMXConnectionNotification and takes appropriate action.
		<br>
	    Used internally as callback for {@link javax.management.NotificationListener}.
	    <b>DO NOT CALL THIS METHOD</b>.
	 */
		public void
	handleNotification(
		final Notification	notifIn, 
		final Object		handback) 
	{
		final String	type	= notifIn.getType();
		
		if ( type.equals( MBeanServerNotification.UNREGISTRATION_NOTIFICATION)  )
		{
			final MBeanServerNotification	notif	= (MBeanServerNotification)notifIn;
			final ObjectName	objectName	= notif.getMBeanName();
			//debug( "ProxyFactory.handleNotification: UNREGISTERED: ", objectName );
		}
		else if ( notifIn instanceof JMXConnectionNotification )
		{
			if ( type.equals( JMXConnectionNotification.CLOSED ) ||
				type.equals( JMXConnectionNotification.FAILED ) )
			{
                debug( "ProxyFactory.handleNotification: connection closed or failed: ", notifIn);
				connectionBad();
			}
			else if ( type.equals( JMXConnectionNotification.NOTIFS_LOST ) )
			{
                debug( "ProxyFactory.handleNotification: notifications lost: ", notifIn);
				notifsLost();
			}
		}
		else
		{
			debug( "ProxyFactory.handleNotification: UNKNOWN notification: ", notifIn );
		}
	}
    
        	
	private final static String	DOMAIN_ROOT_KEY	= "DomainRoot";
	
		public DomainRoot
	createDomainRoot( )
		throws IOException
	{
		return( mDomainRoot );
	}
	
		public DomainRoot
	initDomainRoot( )
		throws IOException
	{
		final ObjectName	domainRootObjectName	= getDomainRootObjectName( );
		
		final DomainRoot dr	= getProxy(domainRootObjectName, DomainRoot.class);
		
		return( dr );
	}

	/**
		Return the ObjectName for the DomainMBean.
	 */
		public ObjectName
	getDomainRootObjectName()
	{
		return( mDomainRootObjectName );
	}
	
	/**
	    Return the DomainRoot. AMX is guaranteed to be ready after this call returns.
	    
		@return the DomainRoot for this factory.
	 */
		public DomainRoot
	getDomainRootProxy( )
	{
		return getDomainRootProxy( false );
	}
	
	/**
	    If 'waitReady' is true, then upon return AMX
	    is guaranteed to be fully loaded.  Otherwise
	    AMX MBeans may continue to initialize asynchronously.
	    
	    @param waitReady
		@return the DomainRoot for this factory.
	 */
		public DomainRoot
	getDomainRootProxy( boolean waitReady )
	{
	    if ( waitReady )
	    {
	        mDomainRoot.waitAMXReady();
	    }
	    
		return( mDomainRoot );
	}
	
	
	/**
		@return the ConnectionSource used by this factory
	 */
		public ConnectionSource
	getConnectionSource()
	{
		return( mConnectionSource );
	}
	
	/**
		@return the JMX MBeanServerID for the MBeanServer in which MBeans reside.
	 */
		public String
	getMBeanServerID()
	{
		return( mMBeanServerID );
	}
	
	/**
		Get an instance of the ProxyFactory for the MBeanServer.  Generally
		not applicable for remote clients.
		
		@param server
	 */
		public static ProxyFactory
	getInstance( final MBeanServer server )
	{
		return( getInstance( new MBeanServerConnectionSource( server ), true ) );
	}
	
	/**
		Get an instance of the ProxyFactory for the MBeanServerConnection.
		Creates a ConnectionSource for it and calls getInstance( connSource, true ).
	 */
		public static ProxyFactory
	getInstance( final MBeanServerConnection conn )
	{
		return( getInstance( new MBeanServerConnectionConnectionSource( conn ), true ) );
	}
	
	/**
		Calls getInstance( connSource, true ).
	 */
		public static ProxyFactory
	getInstance( final ConnectionSource conn )
	{	
		return( getInstance( conn, true ) );
	}
	
	/**
		Get an instance.  If 'useMBeanServerID' is false, and
		the ConnectionSource is not one that has been passed before, a new ProxyFactory
		is instantiated which will not share its proxies with any previously-instantiated
		ones.  Such usage is discouraged, as it duplicates proxies.  Pass 'true' unless
		there is an excellent reason to pass 'false'.
		
		@param connSource			the ConnectionSource
		@param useMBeanServerID		use the MBeanServerID to determine if it's the same server
	 */
		public static synchronized ProxyFactory
	getInstance(
		final ConnectionSource	connSource,
		final boolean			useMBeanServerID )
	{
		ProxyFactory	instance	= findInstance( connSource );
		
		if ( instance == null )
		{
			try
			{
				// match based on the MBeanServerConnection; different
				// ConnectionSource instances could wrap the same connection
				final MBeanServerConnection	conn =
					connSource.getMBeanServerConnection( false );
				
				instance	= findInstance( conn );
				
				// if not found, match based on MBeanServerID as requested, or if this
				// is an in-process MBeanServer
				if ( instance == null &&
					( useMBeanServerID  || connSource instanceof MBeanServerConnectionSource ) )
				{
					final String	id	= JMXUtil.getMBeanServerID( conn );
					instance	= findInstanceByID( id );
				}
			
				if ( instance == null )
				{
                    //debug( "Creating new ProxyFactory for ConnectionSource / conn", connSource, conn );
					instance	= new ProxyFactory( connSource );
					INSTANCES.put( conn, instance );
				}
                
                // ensure that AMX is booted and ready to go.
			}
			catch( Exception e )
			{
				warning( "ProxyFactory.getInstance: failure creating ProxyFactory: ", e );
				throw new RuntimeException( e );
			}
		}
		
		return( instance );
	}
	
	/**
		@return ProxyFactory corresponding to the ConnectionSource
	 */
		public static synchronized ProxyFactory
	findInstance( final ConnectionSource conn )
	{
		return( INSTANCES.get( conn ) );
	}
	
	/**
		@return ProxyFactory corresponding to the MBeanServerConnection
	 */
		public static synchronized ProxyFactory
	findInstance( final MBeanServerConnection conn )
	{
		ProxyFactory	instance	= null;
		
		final Collection<ProxyFactory> values	= INSTANCES.values();
		for( final ProxyFactory factory : values )
		{
			if ( factory.getConnectionSource().getExistingMBeanServerConnection( ) == conn )
			{
				instance	= factory;
				break;
			}
		}
		return( instance );
	}
	
	
	/**
		@return ProxyFactory corresponding to the MBeanServerID
	 */
		public static synchronized ProxyFactory
	findInstanceByID( final String mbeanServerID )
	{
		ProxyFactory	instance	= null;
		
		final Collection<ProxyFactory> values	= INSTANCES.values();
		for( final ProxyFactory factory : values )
		{
			if ( factory.getMBeanServerID().equals( mbeanServerID ) )
			{
				instance	= factory;
				break;
			}
		}
		
		return( instance );
	}
	
    /**
        Return (possibly cached) MBeanInfo.
     */
    public MBeanInfo getMBeanInfo( final ObjectName objectName )
    {
        try
        {
            MBeanInfo info = mMBeanInfoCache.get(objectName);
            if ( info == null )
            {
                // race condition: doesn't matter if two threads both get it
                info = getMBeanServerConnection().getMBeanInfo(objectName);
                if ( invariantMBeanInfo(info)  )
                {
                    mMBeanInfoCache.put(objectName, info);
                }
            }
            return info;
        }
        catch ( Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public static boolean invariantMBeanInfo(final MBeanInfo info )
    {
        final Descriptor d = info.getDescriptor();
        if ( d == null ) return false;
        
        final String value =  "" + d.getFieldValue( AMXConstants.DESC_STD_IMMUTABLE_INFO);
        return Boolean.valueOf( value );
    }
    
   	
	/**
		@return MBeanServerConnection used by this factory
	 */
		protected MBeanServerConnection
	getMBeanServerConnection()
		throws IOException
	{
		return( getConnectionSource().getMBeanServerConnection( false ) );
	}
    
	/**
		Get any existing proxy, returning null if none exists and 'create' is false.
		
		@param objectName	ObjectName for which a proxy should be created
		@param intf         class of returned proxy, avoids casts and compiler warnings
		@return an appropriate {@link AMXProxy} interface for the ObjectName
	 */
		public <T extends AMXProxy> T
	getProxy(
	    final ObjectName	objectName,
	    Class<T>            intf)
	{
		final T proxy = getProxy( objectName, getMBeanInfo(objectName), intf);
		return proxy;
	}
    
    /** Call getProxy(objectName, getGenericAMXInterface() */
    	public AMXProxy
	getProxy( final ObjectName	objectName)
	{
        final MBeanInfo info = getMBeanInfo(objectName);
        final Class<? extends AMXProxy>  intf = genericInterface(info);
		final AMXProxy proxy = getProxy( objectName, info, intf);
        return proxy;
	}

        
    public static Class<? extends AMXProxy> genericInterface(final MBeanInfo info)
    {
        final String intfName = AMXProxyHandler.genericInterfaceName(info);
        Class<? extends AMXProxy> intf = AMXProxy.class;

        if (intfName == null || AMXProxy.class.getName().equals(intfName))
        {
            intf = AMXProxy.class;
        }
        else if (AMXConfigProxy.class.getName().equals(intfName))
        {
            intf = AMXConfigProxy.class;
        }
        else if (intfName.startsWith(AMXProxy.class.getPackage().getName()))
        {
            try
            {
                intf = Class.forName(intfName, false, ProxyFactory.class.getClassLoader()).asSubclass(AMXProxy.class);
            }
            catch (final Exception e)
            {
                // ok, use generic
                debug("ProxyFactory.getInterfaceClass(): Unable to load interface " + intfName);
            }
        }
        else
        {
            intf = AMXProxy.class;
        }
        return intf;
    }



        <T extends AMXProxy> T
	getProxy(
        final ObjectName objectName,
        final MBeanInfo  mbeanInfoIn, 
        final Class<T>   intfIn)
	{
        //debug( "ProxyFactory.createProxy: " + objectName + " of class " + expected.getName() + " with interface " + JMXUtil.interfaceName(mbeanInfo) + ", descriptor = " + mbeanInfo.getDescriptor() );
		AMXProxy proxy = null;
        
        MBeanInfo mbeanInfo = mbeanInfoIn;
        if ( mbeanInfo == null )
        {
            mbeanInfo = getMBeanInfo(objectName);
        }
        
        // if it's a plain AMXProxy, it might have a more generic sub-interface we should use.
        Class<? extends AMXProxy>  intf = intfIn; 
        if ( AMXProxy.class == intf )
        {
            intf = genericInterface(mbeanInfoIn);
        }
        
        try
        {
            final AMXProxyHandler handler	= new AMXProxyHandler( getMBeanServerConnection(), objectName, mbeanInfo);
            proxy	= (AMXProxy)Proxy.newProxyInstance( intf.getClassLoader(), new Class[] { intf }, handler);
            //debug( "CREATED proxy of type " + intf.getName() + ", metadata specifies " + AMXProxyHandler.interfaceName(mbeanInfo) );
        }
        catch( IllegalArgumentException e )
        {
            //debug( "createProxy", e );
            throw e;
        }
        catch( Exception e )
        {
            //debug( "createProxy", e );
            throw new RuntimeException( e );
        }
				
		return intfIn.cast( proxy );
	}

	
		protected static String
	toString( final Object o )
	{
		//return( org.glassfish.admin.amx.util.stringifier.SmartStringifier.toString( o ) );
        return "" + o;
	}
    
    
        public AMXProxy[]
    toProxy( final ObjectName[] objectNames )
    {
        final AMXProxy[] result = new AMXProxy[objectNames.length];
        for( int i = 0; i < objectNames.length; ++i )
        {
            result[i] = getProxy(objectNames[i]);
        }
        return result;
    }
    
	/**
		Convert a Set of ObjectName to a Set of AMX.
		
		@return a Set of AMX from a Set of ObjectName.
	 */
		public Set<AMXProxy>
	toProxySet( final Set<ObjectName> objectNames )
	{
		final Set<AMXProxy>	s	= new HashSet<AMXProxy>();
		
		for( final ObjectName objectName : objectNames )
		{
			try
			{
				final AMXProxy	proxy	= getProxy( objectName );
				assert( ! s.contains( proxy ) );
				s.add( proxy );
			}
			catch( final Exception e )
			{
			    debug( "ProxyFactory.toProxySet: exception for MBean ",
                    objectName, " = ", ExceptionUtil.getRootCause( e ) );
			}
		}
		
		return( s );
	}
    
        public Set<AMXProxy>
	toProxySet( final ObjectName[] objectNames, final Class<? extends AMXProxy> intf)
	{
		final Set<AMXProxy> result = new HashSet<AMXProxy>();
		for( final ObjectName objectName : objectNames )
		{
            result.add( getProxy( objectName, intf) );
		}
		return( result );
    }
	
	/**
		Convert a Collection of ObjectName to a List of AMX.
		
		@return a List of AMX from a List of ObjectName.
	 */
		public List<AMXProxy>
	toProxyList( final Collection<ObjectName> objectNames )
	{
		final List<AMXProxy>	list	= new ArrayList<AMXProxy>();
		
		for( final ObjectName objectName : objectNames )
		{
			try
			{
				final AMXProxy	proxy	= getProxy( objectName );
				list.add( proxy );
			}
			catch( final Exception e )
			{
			    debug( "ProxyFactory.toProxySet: exception for MBean ",
                    objectName, " = ", ExceptionUtil.getRootCause( e ) );
			}
		}
		
		return( list );
	}
	
	/**
		Convert a Map of ObjectName, and convert it to a Map
		of AMX, with the same keys.
		
		@return a Map of AMX from a Map of ObjectName.
	 */
		public Map<String,AMXProxy>
	toProxyMap(
		final Map<String,ObjectName>	objectNameMap )
	{
		final Map<String,AMXProxy> resultMap	= new HashMap<String,AMXProxy>();
		
		final Set<String>   keys    = objectNameMap.keySet();
		
		for( final String key : keys )
		{
			final ObjectName	objectName	= objectNameMap.get( key );
			
			try
			{
				final AMXProxy	proxy	= getProxy( objectName );
				resultMap.put( key, proxy );
			}
			catch( final Exception e )
			{
			    debug( "ProxyFactory.toProxySet: exception for MBean ",
                    objectName, " = ", ExceptionUtil.getRootCause( e ) );
			}
		}
		
		return( resultMap );
	}
	
        public Map<String,AMXProxy>
	toProxyMap( final ObjectName[] objectNames, final Class<? extends AMXProxy> intf)
	{
		final Map<String,AMXProxy> resultMap	= new HashMap<String,AMXProxy>();
		
		for( final ObjectName objectName : objectNames )
		{
            final String key = objectName.getKeyProperty(AMXConstants.NAME_KEY);
            final AMXProxy	proxy	= getProxy( objectName, intf);
            resultMap.put( key, proxy );
		}
		
		return( resultMap );
    }
    
        public List<AMXProxy>
	toProxyList( final ObjectName[] objectNames, final Class<? extends AMXProxy> intf)
	{
		final List<AMXProxy> result = new ArrayList<AMXProxy>();
		for( final ObjectName objectName : objectNames )
		{
            result.add( getProxy( objectName, intf) );
		}
		return( result );
    }
}














