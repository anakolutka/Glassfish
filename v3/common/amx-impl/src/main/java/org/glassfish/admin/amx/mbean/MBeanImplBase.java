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

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;

import java.io.Serializable;

import javax.management.ObjectName;
import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.MBeanRegistration;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ReflectionException;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ListenerNotFoundException;

import com.sun.appserv.management.util.misc.StringUtil;
import com.sun.appserv.management.base.AMXDebug;
import com.sun.appserv.management.base.AMXMBeanLogging;

import com.sun.appserv.management.util.stringifier.SmartStringifier;
import com.sun.appserv.management.util.jmx.NotificationBuilder;
import com.sun.appserv.management.util.jmx.NotificationSender;
import com.sun.appserv.management.util.jmx.NotificationEmitterSupport;
import com.sun.appserv.management.util.jmx.AttributeChangeNotificationBuilder;

import com.sun.appserv.management.util.misc.Output;

/**
	Absolute base impl class. Should contain only core functionality,
	nothing to do with appserver specifics.
 */
public abstract class MBeanImplBase
	implements MBeanRegistration, NotificationSender, AMXMBeanLogging
{
    protected static final String[]   EMPTY_STRING_ARRAY  = new String[0];
    
	/**
		The MBeanServer in which this object is registered (if any)
	 */
	protected MBeanServer	mServer;
	
	/**
		The ObjectName by which this object is registered (if registered).
		Multiple registrations, which are possible, overwrite this; the last registration
		wins.  If the MBean has not been registered, this name will be null.
	 */
	protected ObjectName	mSelfObjectName;
	
	private NotificationEmitterSupport	mNotificationEmitter;
	
	private Map<String,NotificationBuilder> mNotificationBuilders;
	
    /**
        We need debugging before the MBean is registered, so our
        only choice is to use an ID based on something other than
        the as-yet-unknown ObjectName, namely the classname.
     */
    private final Output   mDebug = AMXDebug.getInstance().getOutput( getDebugID() );
	
		public
	MBeanImplBase()
	{
		mNotificationEmitter	= null;
	}
	
	
		public final int
	getListenerCount()
	{
		return( getNotificationEmitter().getListenerCount() );
	}
	
		public final int
	getNotificationTypeListenerCount( final String type )
	{
		return( getNotificationEmitter().getNotificationTypeListenerCount( type ) );
	}
	
	/**
		@return an empty array
		Subclass may wish to override this.
	*/
		synchronized protected final NotificationEmitterSupport
	getNotificationEmitter()
	{
		if ( mNotificationEmitter == null )
		{
			mNotificationEmitter	= new NotificationEmitterSupport( true );
		}
		
		return( mNotificationEmitter );
	}
	
		public synchronized void
	addNotificationListener(
		final NotificationListener	listener )
	{
		getNotificationEmitter().addNotificationListener( listener, null, null );
	}
	
		public synchronized void
	addNotificationListener(
		final NotificationListener	listener,
		final NotificationFilter	filter,
		final Object				handback)
	{
		getNotificationEmitter().addNotificationListener( listener, filter, handback );
	}

		public synchronized void
	removeNotificationListener( final NotificationListener listener)
		throws ListenerNotFoundException
	{
 		getNotificationEmitter().removeNotificationListener( listener );
	}
 
		public synchronized void
	removeNotificationListener(
		final NotificationListener	listener,
		final NotificationFilter	filter,
		final Object				handback)
		throws ListenerNotFoundException
	{
		getNotificationEmitter().removeNotificationListener( listener, filter, handback );
	}

		public void
 	sendNotification( final Notification notification)
 	{
 		getNotificationEmitter().sendNotification( notification );
 	}
	
		protected NotificationBuilder
	createNotificationBuilder( final String notificationType )
	{
		NotificationBuilder	builder	= null;
		
		if ( notificationType.equals( AttributeChangeNotification.ATTRIBUTE_CHANGE ) )
		{
			builder	= new AttributeChangeNotificationBuilder( getObjectName() );
		}
		else
		{
			builder	= new NotificationBuilder( notificationType, getObjectName() );
		}
		
		return( builder );
	}
	
	/**
		Get a NotificationBuilder for the specified type of Notification
		whose source is this object.
	 */
		protected synchronized NotificationBuilder
	getNotificationBuilder( final String notificationType )
	{
		if ( mNotificationBuilders == null )
		{
			mNotificationBuilders	= new HashMap<String,NotificationBuilder>();
		}
		
		NotificationBuilder	builder	=
			(NotificationBuilder)mNotificationBuilders.get( notificationType );
		
		if ( builder == null )
		{
			builder	= createNotificationBuilder( notificationType );
			mNotificationBuilders.put( notificationType, builder );
		}
		
		return( builder );
	}
	
	/**
		Send a Notification of the specified type containing no data.
	 */
		protected void
	sendNotification( final String	notificationType )
	{
		sendNotification( notificationType, notificationType, null, null );
	}
	
	
	/**
		Send a Notification of the specified type containing a single
		key/value pair for data.  
	 */
		protected void
	sendNotification(
		final String	    notificationType,
		final String	    key,
		final Serializable	value)
	{
	    final String    message = "no message specified";
	    
	    sendNotification( notificationType, message, key, value );
	}
	
	/**
		Send a Notification of the specified type containing a single
		key/value pair for data.
	 */
		protected void
	sendNotification(
		final String	    notificationType,
		final String        message,
		final String	    key,
		final Serializable	value)
	{
		final NotificationBuilder	builder	=
			getNotificationBuilder( notificationType );
			
		final Notification	notif	= builder.buildNew( message );
		NotificationBuilder.putMapData( notif, key, value );
			
		sendNotification( notif );
	}
	
	
		public final ObjectName
	getObjectName()
	{
		return( mSelfObjectName );
	}
	
		public String
	getJMXDomain()
	{
		return( getObjectName().getDomain() );
	}
	
		public final MBeanServer
	getMBeanServer()
	{
		return( mServer );
	}
	
		protected static String
	toString( Object o )
	{
	    if ( o == null )
	    {
	        return( "" + o );
	    }
	    
		return( SmartStringifier.toString( o ) );
	}
	
		protected final void
	trace( Object o )
	{
	    debug( o );
	}
	
		protected final void
	logSevere( Object o )
	{
	    final String msg    = toString( o );
		debug( msg );
		
		if ( getMBeanLogLevelInt() <= Level.SEVERE.intValue() )
		{
			getMBeanLogger().severe( msg );
		}
	}
	
		protected final void
	logWarning( Object o )
	{
	    final String msg    = toString( o );
		debug( msg );
		
		if ( getMBeanLogLevelInt() <= Level.WARNING.intValue() )
		{
			getMBeanLogger().warning( msg );
		}
	}
	
		protected final void
	logInfo( Object o )
	{
	    final String msg    = toString( o );
		debug( msg );
		
		if ( getMBeanLogLevelInt() <= Level.INFO.intValue() )
		{
			getMBeanLogger().info( msg );
		}
	}
	
		protected final void
	logFine( Object o )
	{
	    final String msg    = toString( o );
		debug( msg );
		
		if ( getMBeanLogLevelInt() <= Level.FINE.intValue() )
		{
			getMBeanLogger().fine( msg );
		}
	}
	
		protected final void
	logFiner( Object o )
	{
	    final String msg    = toString( o );
		debug( msg );
		
		if ( getMBeanLogLevelInt() <= Level.FINER.intValue() )
		{
			getMBeanLogger().finer( msg );
		}
	}
	
		protected final void
	logFinest( Object o )
	{
	    final String msg    = toString( o );
		debug( msg );
		
		if ( getMBeanLogLevelInt() <= Level.FINEST.intValue() )
		{
			getMBeanLogger().finest( msg );
		}
	}
	
		protected final Logger
	getMBeanLogger()
	{
	    return Logger.getLogger( "MBeans" );
	}
	
	
		public final Level
	getMBeanLogLevel()
	{
		Logger	logger	= getMBeanLogger();
		assert( logger != null );
		
		Level	level	= logger.getLevel();
		while ( level == null )
		{
			logger	= logger.getParent();
			level	= logger.getLevel();
		}
		
		return( level );
	}
	
	
		public final void
	setMBeanLogLevel( final Level level )
	{
		getMBeanLogger().setLevel( level );
	}
	
		protected final int
	getMBeanLogLevelInt()
	{
		return( getMBeanLogLevel().intValue() );
	}
	
		public final String
	getMBeanLoggerName()
	{
		return( getMBeanLogger().getName() );
	}
	
	private static final Level[]	LOG_LEVELS	=
	{
		Level.ALL,
		Level.SEVERE,
		Level.WARNING,
		Level.INFO,
		Level.FINE,
		Level.FINER,
		Level.FINEST,
		Level.OFF,
	};
	
		public final void
	setMBeanLogLevelString( final String levelString )
	{
		Level	level	= null;
		for( int i = 0; i < LOG_LEVELS.length; ++i )
		{
			if ( levelString.equals( LOG_LEVELS[ i ].getName() ) )
			{
				level	= LOG_LEVELS[ i ];
				break;
			}
		}
		
		if ( level == null )
		{
			throw new IllegalArgumentException( levelString );
		}

		setMBeanLogLevel( level );
	}
	
		protected static String
	quote( final Object o )
	{
		return( StringUtil.quote( "" + o ) );
	}
	
		public ObjectName
	preRegister(
		final MBeanServer	server,
		final ObjectName	nameIn)
		throws Exception
	{
		assert( nameIn != null );
		mServer			= server;
		mSelfObjectName	= nameIn;
		
		// ObjectName could still be modified by subclass
		return( mSelfObjectName );
	}
	
		public void
	postRegister( Boolean registrationDone )
	{	
		if ( registrationDone.booleanValue() )
		{
			getMBeanLogger().finest( "postRegister: " + getObjectName());
		}
		else
		{
			getMBeanLogger().finest( "postRegister: FAILURE: " + getObjectName());
		}
	}
	
		public void
	preDeregister()
		throws Exception
	{
		getMBeanLogger().finest( "preDeregister: " + getObjectName() );
	}
	
		public void
	postDeregister()
	{
		getMBeanLogger().finest( "postDeregister: " + getObjectName() );
		
		if ( mNotificationEmitter != null )
		{
			mNotificationEmitter.cleanup();
			mNotificationEmitter	= null;
		}
		
		if ( mNotificationBuilders != null )
		{
			mNotificationBuilders.clear();
			mNotificationBuilders	= null;
		}
		
		
		mServer					= null;
		mSelfObjectName			= null;
		
	}
	
        protected String
    getDebugID()
    {
        return this.getClass().getName();
    }
    
    
    /**
        Although unusual, a subclass may override the debug state.
        Generally it is faster to NOT call getAMXDebug() before calling
        debug( x ) if 'x' is just a string.  If it is expensive to
        construct 'x', then preflighting with getAMXDebug() may
        be worthwhile. 
      */
        public final boolean
    getAMXDebug()
    {
        return AMXDebug.getInstance().getDebug( getDebugID() );
    }
    
        public boolean
    enableAMXDebug( final boolean enabled )
    {
        final boolean formerValue   = getAMXDebug();
        if ( formerValue != enabled )
        {
            setAMXDebug( enabled );
        }
        return formerValue;
    }
    
        protected Output
    getDebugOutput()
    {
        return AMXDebug.getInstance().getOutput( getDebugID() );
    }
    
        public final void
    setAMXDebug( final boolean debug )
    {
        AMXDebug.getInstance().setDebug( getDebugID(), debug);
    }
    
        protected boolean
    shouldOmitObjectNameForDebug()
    {
        return mSelfObjectName == null;
    }
    
	 	protected final void
	debug(final Object o)
	{
	    if ( getAMXDebug() && mDebug != null)
	    {
	        final String    newline = System.getProperty( "line.separator" );
	        
	        if ( shouldOmitObjectNameForDebug() )
	        {
	            mDebug.println( o );
	        }
	        else
	        {
	            mDebug.println( mSelfObjectName.toString() );
	            mDebug.println( "===> " + o );
	        }
	        mDebug.println( newline );
	    }
	}
	
		protected void
	debugMethod( final String methodName, final Object... args )
	{
	    if ( getAMXDebug() )
	    {
	        debug( AMXDebug.methodString( methodName, args ) );
	    }
	}
	
		protected void
	debugMethod(
	    final String msg,
	    final String methodName,
	    final Object... args )
	{
	    if ( getAMXDebug() )
	    {
	        debug( AMXDebug.methodString( methodName, args ) + ": " + msg );
	    }
	}
	
		protected void
	debug( final Object... args )
	{
	    if ( getAMXDebug() )
	    {
	        debug( StringUtil.toString( "", args) );
	    }
	}
	
	
		protected boolean
	sleepMillis( final long millis )
	{
		boolean	interrupted	= false;
		
		try
		{
			Thread.sleep( millis );
		}
		catch( InterruptedException e )
		{
			Thread.interrupted();
			interrupted	= true;
		}
		
		return interrupted;
	}
}








